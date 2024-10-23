/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.validation;

import java.util.List;
import java.util.Objects;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogValidator;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Support class implementing the {@link Catalog} state validation rules for {@link CatalogInfo}
 * objects before being created, updated, or removed;
 *
 * @see CatalogValidator
 * @see DefaultCatalogValidator
 * @see BeforeRemoveValidator
 */
public class CatalogValidationRules {

    private Catalog catalog;

    /** extended validation switch */
    protected boolean extendedValidation = true;

    public CatalogValidationRules(Catalog catalog) {
        Objects.requireNonNull(catalog);
        this.catalog = catalog;
    }

    /**
     * Turn on/off extended validation switch.
     *
     * <p>This is not part of the public api, it is used for testing purposes where we have to
     * bootstrap catalog contents.
     */
    public void setExtendedValidation(boolean extendedValidation) {
        this.extendedValidation = extendedValidation;
    }

    public boolean isExtendedValidation() {
        return extendedValidation;
    }

    public List<CatalogValidator> getValidators() {
        return GeoServerExtensions.extensions(CatalogValidator.class);
    }

    public <T extends CatalogInfo> ValidationResult validate(T object, boolean isNew) {
        if (isNew) {
            // REVISIT: of course the webui will call catalog.validate with a mod proxy and
            // isNew=true... ResourceConfigurationPage.doSaveInternal() for instance
            object = ModificationProxy.unwrap(object);
        }

        CatalogValidator validator = new DefaultCatalogValidator(catalog);
        CatalogValidatorVisitor visitor = new CatalogValidatorVisitor(validator, isNew);
        object.accept(visitor);
        return postValidate(object, isNew);
    }

    public <T extends CatalogInfo> void beforeRemove(T info) {
        info.accept(new BeforeRemoveValidator(catalog));
    }

    private ValidationResult postValidate(CatalogInfo info, boolean isNew) {
        if (!extendedValidation) {
            return new ValidationResult(null);
        }

        List<RuntimeException> errors = getValidators().stream()
                .map(validator -> validate(info, validator, isNew))
                .filter(Objects::nonNull)
                .toList();

        return new ValidationResult(errors);
    }

    /**
     * @return {@code null} if the validator succeeded, the validator's thrown exception otherwise
     */
    private RuntimeException validate(CatalogInfo info, CatalogValidator validator, boolean isNew) {
        try {
            info.accept(new CatalogValidatorVisitor(validator, isNew));
        } catch (RuntimeException e) {
            return e;
        }
        return null;
    }
}
