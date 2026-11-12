/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.catalog.plugin.RepositoryCatalogFacadeImpl;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigCatalogInfoRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigLayerGroupRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigLayerRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigNamespaceRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigPublishedInfoReadOnlyRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigResourceRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigStoreRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigStyleRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigWorkspaceRepository;
import org.geotools.api.filter.Filter;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A facade for the catalog in the {@code pgconfig} backend of GeoServer Cloud. This class leverages a PostgreSQL
 * database via a {@link JdbcTemplate} to persist and retrieve catalog data.
 *
 * <p>Repositories are initialized in the constructor using the provided {@link JdbcTemplate}, and share the optional
 * {@link EntityCache} given to the constructor: it answers lookups by id and by name from memory. A notable feature is
 * the inclusion of a dedicated, read-only {@link PublishedInfo} repository
 * ({@link PgconfigPublishedInfoReadOnlyRepository}), which optimizes queries for published information (combining
 * {@link LayerInfo} and {@link LayerGroupInfo}) without requiring separate queries to layer and layer group
 * repositories.
 *
 * <p>The objects of this facade are raw: a reference to another object is a {@link ResolvingProxy} holding its id,
 * unless the row of the object brought the referenced object too. The catalog gets this facade decorated by
 * {@link PgconfigBackendBuilder#resolving}: the decorator resolves the references through this facade, below workspace
 * isolation. A listing evaluating part of its filter in memory resolves its objects the same way first, see
 * {@link #referenceResolver(ExtendedCatalogFacade)}.
 *
 * <p>This class overrides specific methods from {@link RepositoryCatalogFacadeImpl} to utilize the
 * {@link PgconfigPublishedInfoReadOnlyRepository} directly for counting and querying {@link PublishedInfo} objects.
 * This approach enhances performance by avoiding multiple queries and merge-sorting of results, as done in the default
 * implementation.
 *
 * <p><strong>Usage Note:</strong> The {@link PublishedInfo} repository is read-only and intended for querying purposes
 * only. Modifications to catalog data should be performed through the appropriate writable repositories (e.g.,
 * {@link PgconfigLayerRepository} or {@link PgconfigLayerGroupRepository}).
 *
 * @see PgconfigPublishedInfoReadOnlyRepository
 * @see RepositoryCatalogFacadeImpl
 * @see JdbcTemplate
 * @since 1.4
 */
public class PgconfigCatalogFacade extends RepositoryCatalogFacadeImpl {
    /**
     * Read-only {@link PublishedInfo} repository, used to query published without having to perform two queries (one
     * for layers and one for layer groups) and them merge-sort the results.
     *
     * @see #queryPublishedInfo(Query)
     */
    private PgconfigPublishedInfoReadOnlyRepository publishedInfoReadOnlyRepo;

    public PgconfigCatalogFacade(@NonNull JdbcTemplate template) {
        this(template, null);
    }

    /**
     * @param cache keeps the objects read by the repositories, shared by them. Null reads the objects from the
     *     database.
     */
    public PgconfigCatalogFacade(@NonNull JdbcTemplate template, @Nullable EntityCache cache) {
        PgconfigNamespaceRepository namespaces = new PgconfigNamespaceRepository(template);
        PgconfigWorkspaceRepository workspaces = new PgconfigWorkspaceRepository(template);
        PgconfigStyleRepository styles = new PgconfigStyleRepository(template);

        PgconfigStoreRepository stores = new PgconfigStoreRepository(template);
        PgconfigLayerRepository layers = new PgconfigLayerRepository(template, styles);
        PgconfigResourceRepository resources = new PgconfigResourceRepository(template, layers);
        PgconfigLayerGroupRepository layerGroups = new PgconfigLayerGroupRepository(template, styles);

        super.setNamespaceRepository(namespaces);
        super.setWorkspaceRepository(workspaces);
        super.setStoreRepository(stores);
        super.setResourceRepository(resources);
        super.setLayerRepository(layers);
        super.setLayerGroupRepository(layerGroups);
        super.setStyleRepository(styles);
        /*
         * We have a PublishedInfo repository, despite not being common practice (since
         * PublishedInfo was introduced later as a superclass for LayerInfo and
         * LayerGroupInfo, it never had its own "repository" in the default
         * Catalog/Facade implementation)
         */
        this.publishedInfoReadOnlyRepo = new PgconfigPublishedInfoReadOnlyRepository(template, styles);
        UnaryOperator<CatalogInfo> references = referenceResolver(this);
        for (PgconfigCatalogInfoRepository<?> repository : allRepositories()) {
            repository.setEntityCache(cache);
            repository.setReferenceResolver(references);
        }
    }

    /**
     * Returns the function resolving the references of the objects of a facade through that facade, and initializing
     * their null collections. The decorator built by {@link PgconfigBackendBuilder#resolving} applies it to the objects
     * returned to the catalog. The repositories of this facade apply it to the objects of a listing before evaluating
     * the part of its filter not translated to SQL.
     */
    public static UnaryOperator<CatalogInfo> referenceResolver(@NonNull ExtendedCatalogFacade facade) {
        ResolvingProxyResolver<CatalogInfo> references = ResolvingProxyResolver.of(facade);
        UnaryOperator<CatalogInfo> collections = CollectionPropertiesInitializer.instance();
        return references.andThen(collections)::apply;
    }

    /** Sets whether listings add their objects to the cache. Off by default. */
    public void setListingWriteThrough(boolean listingWriteThrough) {
        allRepositories().forEach(repository -> repository.setListingWriteThrough(listingWriteThrough));
    }

    /**
     * The repositories of the facade, and the published infos repository. The latter answers {@code getPublished},
     * {@code findAllById} and the listings of published infos, and needs the cache of the others.
     */
    private List<PgconfigCatalogInfoRepository<?>> allRepositories() {
        List<PgconfigCatalogInfoRepository<?>> all = new ArrayList<>();
        for (CatalogInfoRepository<?> repository : super.repositories.all()) {
            all.add((PgconfigCatalogInfoRepository<?>) repository);
        }
        all.add(publishedInfoReadOnlyRepo);
        return all;
    }

    /**
     * Queries {@link PgconfigPublishedInfoReadOnlyRepository} directly. The default implementation will query
     * {@link LayerRepository} and {@link LayerGroupRepository} separately.
     */
    @Override
    protected int countPublishedInfo(Filter filter) {
        PgconfigPublishedInfoReadOnlyRepository repo = this.publishedInfoReadOnlyRepo;
        Filter simplified = SimplifyingFilterVisitor.simplify(filter);
        return (int) repo.count(PublishedInfo.class, simplified);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repository(Class<T> of) {
        if (PublishedInfo.class.equals(of)) {
            return (R) publishedInfoReadOnlyRepo;
        }
        return repositories.repository(of);
    }

    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repositoryFor(T info) {
        return repositories.repositoryFor(info);
    }

    /** Looks the id up in the published infos view, in one query instead of one per type. */
    @Override
    public PublishedInfo getPublished(@NonNull String id) {
        return publishedInfoReadOnlyRepo.findById(id).orElse(null);
    }

    /** Looks the ids up with one query. A {@link PublishedInfo} lookup reads the published infos view. */
    @Override
    public <T extends CatalogInfo> Stream<T> findAllById(@NonNull List<String> ids, @NonNull Class<T> type) {
        if (CatalogInfo.class.equals(type)) {
            // no repository holds all the types
            return super.findAllById(ids, type);
        }
        PgconfigCatalogInfoRepository<T> repository = repository(type);
        return repository.findAllById(ids, type);
    }

    /**
     * Queries {@link PgconfigPublishedInfoReadOnlyRepository} directly. The default implementation will query
     * {@link LayerRepository} and {@link LayerGroupRepository}, returning a merge-sorted stream.
     */
    @Override
    protected Stream<PublishedInfo> queryPublishedInfo(Query<PublishedInfo> query) {
        final Filter filter = SimplifyingFilterVisitor.simplify(query.getFilter());

        return this.publishedInfoReadOnlyRepo.findAll(query.withFilter(filter));
    }
}
