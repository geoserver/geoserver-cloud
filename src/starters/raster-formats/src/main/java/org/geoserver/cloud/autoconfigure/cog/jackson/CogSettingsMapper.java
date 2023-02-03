/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog.jackson;

import org.geoserver.cloud.autoconfigure.cog.jackson.CogSettings.RangeReaderType;

class CogSettingsMapper {

    public CogSettings toJackson(org.geoserver.cog.CogSettings model) {
        return toJackson(model, new CogSettings());
    }

    public org.geoserver.cog.CogSettings toModel(CogSettings pojo) {
        return toModel(pojo, new org.geoserver.cog.CogSettings());
    }

    public CogSettingsStore toJackson(org.geoserver.cog.CogSettingsStore model) {
        CogSettingsStore pojo = new CogSettingsStore();
        pojo.setUsername(model.getUsername());
        pojo.setPassword(model.getPassword());
        return toJackson(model, pojo);
    }

    public org.geoserver.cog.CogSettingsStore toModel(CogSettingsStore pojo) {
        org.geoserver.cog.CogSettingsStore model = new org.geoserver.cog.CogSettingsStore();
        model.setUsername(pojo.getUsername());
        model.setPassword(pojo.getPassword());
        return toModel(pojo, model);
    }

    private <C extends CogSettings> C toJackson(org.geoserver.cog.CogSettings model, C pojo) {
        pojo.setUseCachingStream(model.isUseCachingStream());
        if (null != model.getRangeReaderSettings()) {
            pojo.setRangeReaderSettings(
                    CogSettings.RangeReaderType.valueOf(model.getRangeReaderSettings().toString()));
        }
        return pojo;
    }

    private <M extends org.geoserver.cog.CogSettings> M toModel(CogSettings pojo, M model) {
        model.setUseCachingStream(pojo.isUseCachingStream());
        RangeReaderType r = pojo.getRangeReaderSettings();
        if (null != r) {
            model.setRangeReaderSettings(
                    org.geoserver.cog.CogSettings.RangeReaderType.valueOf(r.toString()));
        }
        return model;
    }
}
