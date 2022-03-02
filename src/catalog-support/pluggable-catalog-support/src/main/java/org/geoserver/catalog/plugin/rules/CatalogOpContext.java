/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.rules;

import static java.util.Objects.requireNonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoTypeRegistry;
import org.geoserver.catalog.plugin.PropertyDiff;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Encapsulates the context on which a catalog add/save/remove operation is being executed,
 * providing the necessary information for {@link CatalogInfoBusinessRules} implementations to act
 * accordingly depending on the type, phase, and status of the operation.
 *
 * <p>{@link CatalogInfoBusinessRules#afterAdd},{@link CatalogInfoBusinessRules#afterSave}, and
 * {@link CatalogInfoBusinessRules#afterRemove} can check {@link #isSuccess()} to decide what to do
 * in case of success or failure, may they have to implement some counter-command in case of
 * failure, or change some other state in case of success.
 *
 * <p>{@link CatalogInfoBusinessRules#beforeSave(CatalogOpContext)} and {@link
 * CatalogInfoBusinessRules#afterSave} can access the {@link #getDiff() diff} that's going to be
 * applied or has just been applied to the {@link #getObject() object}.
 */
public class CatalogOpContext<T extends CatalogInfo> {
    private Catalog catalog;
    private Map<String, Object> operationContext;

    private T object;
    private PropertyDiff diff;
    private RuntimeException error;

    public CatalogOpContext(Catalog catalog, T object) {
        requireNonNull(catalog);
        requireNonNull(object);
        this.catalog = catalog;
        this.object = object;
        this.diff = null;
    }

    public CatalogOpContext(Catalog catalog, T object, PropertyDiff diff) {
        requireNonNull(catalog);
        requireNonNull(object);
        requireNonNull(diff);
        this.catalog = catalog;
        this.object = object;
        this.diff = diff;
    }

    /**
     * @return the catalog on which the operation is being executed
     */
    public Catalog getCatalog() {
        return catalog;
    }

    /**
     * @return the {@link CatalogInfo} object subject of the add,save, or remove operation.
     */
    public T getObject() {
        return object;
    }

    public CatalogOpContext<T> setObject(T object) {
        this.object = object;
        return this;
    }

    /**
     * Returns the diff to be applied on a {@link CatalogInfoBusinessRules#beforeSave beforeSave}
     * context, just applied on a {@link CatalogInfoBusinessRules#afterSave} context, or {@code
     * null} if the context is not pre or post modify.
     */
    public PropertyDiff getDiff() {
        return diff;
    }

    public CatalogOpContext<T> setError(RuntimeException error) {
        this.error = error;
        return this;
    }

    /**
     * @return the reason for the operation failure, or {@code null} if the operation succeeded or
     *     is a pre-operation context
     */
    public RuntimeException getError() {
        return error;
    }

    public boolean isSuccess() {
        return error == null;
    }

    @SuppressWarnings("unchecked")
    public <V> V getContextOption(String key) {
        return operationContext == null ? null : (V) operationContext.get(key);
    }

    public void setContextOption(String key, Object value) {
        if (operationContext == null) {
            operationContext = new HashMap<>();
        }
        operationContext.put(key, value);
    }

    public void set(String key, BooleanSupplier condition) {
        boolean value = condition.getAsBoolean();
        setContextOption(key, Boolean.valueOf(value));
    }

    public boolean is(String key) {
        Boolean value = getContextOption(key);
        return value == null ? false : value;
    }

    /**
     * @return {@code this} type narrowed to a sub type of {@code <T>}
     */
    @SuppressWarnings("unchecked")
    public <S extends T> CatalogOpContext<S> as(Class<S> subtype) {
        if (subtype.isInstance(object)) return (CatalogOpContext<S>) this;
        throw new IllegalArgumentException(
                String.format(
                        "%s<%s> can't be type narrowed to <%s>",
                        getClass().getSimpleName(),
                        CatalogInfoTypeRegistry.determineKey(object.getClass()),
                        subtype.getSimpleName()));
    }
}
