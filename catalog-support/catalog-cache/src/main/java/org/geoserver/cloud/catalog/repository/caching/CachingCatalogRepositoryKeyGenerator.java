/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.catalog.CatalogInfo;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

@Component(value = CacheNames.DEFAULT_KEY_GENERATOR_BEAN_NAME)
public class CachingCatalogRepositoryKeyGenerator implements KeyGenerator {

    private final Set<String> simpleCatalogInfoArgumentMethods;

    public CachingCatalogRepositoryKeyGenerator() {
        simpleCatalogInfoArgumentMethods = new HashSet<>();
        simpleCatalogInfoArgumentMethods.add("add");
        simpleCatalogInfoArgumentMethods.add("remove");
        simpleCatalogInfoArgumentMethods.add("update");
    }

    public @Override Object generate(Object target, Method method, Object... params) {
        final String methodName = method.getName();
        if (simpleCatalogInfoArgumentMethods.contains(methodName)) {
            return getCatalogInfoId(0, params);
        }
        if ("findById".equals(methodName)) {
            return (String) params[0];
        }
        //        if ("findByName".equals(methodName) && params != null && params.length == 2) {
        //            Name name = (Name) params[0];
        //            Class<?> type = (Class<?>) params[1];
        //        }
        // if("setDefaultWorkspace")
        throw new UnsupportedOperationException(
                String.format(
                        "Unable to generate cache key for %s.%s(...)",
                        target.getClass().getName(), methodName));
    }

    private String getCatalogInfoId(int argumentIndex, Object... params) {
        CatalogInfo info = (CatalogInfo) params[argumentIndex];
        return info == null ? null : info.getId();
    }
}
