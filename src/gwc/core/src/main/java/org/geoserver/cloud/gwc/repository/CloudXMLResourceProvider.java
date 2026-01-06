/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.ConfigurationResourceProvider;

/**
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.repository")
public class CloudXMLResourceProvider implements ConfigurationResourceProvider {

    private String templateLocation = "/geowebcache_empty.xml";

    private Supplier<Resource> configDirectory;
    private @NonNull String configFileName = "geowebcache.xml";

    /**
     * @param resourceStore where to {@link ResourceStore#get(String) get} the config directory from
     * @param configFileName name of the core gwc config file (e.g. {@literal geowebcache.xml})
     * @throws ConfigurationException
     */
    public CloudXMLResourceProvider(@NonNull Supplier<Resource> configDirectory) {
        this.configDirectory = configDirectory;
    }

    @Override
    public InputStream in() throws IOException {
        return findOrCreateConfFile().in();
    }

    @Override
    public OutputStream out() throws IOException {
        return findOrCreateConfFile().out();
    }

    @Override
    public void backup() throws IOException {
        backUpConfig(findOrCreateConfFile());
    }

    @Override
    public String getId() {
        return getConfigDirectory().path();
    }

    @Override
    public void setTemplate(final String templateLocation) {
        this.templateLocation = templateLocation;
    }

    private Resource findConfigFile() {
        Resource dir = getConfigDirectory();
        return dir.get(configFileName);
    }

    public Resource getConfigDirectory() {
        Resource dir = configDirectory.get();
        Objects.requireNonNull(dir);
        return dir;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    @Override
    public String getLocation() throws IOException {
        return findConfigFile().path();
    }

    private Resource findOrCreateConfFile() throws IOException {
        Resource xmlFile = findConfigFile();

        if (!Resources.exists(xmlFile)) {
            log.warn(
                    "Found no configuration file in config directory, will create one at '{}' from template {}",
                    xmlFile.path(),
                    getClass().getResource(templateLocation).toExternalForm());
            // grab template from classpath
            try {
                IOUtils.copy(getClass().getResourceAsStream(templateLocation), xmlFile.out());
            } catch (IOException e) {
                throw new IOException("Error copying template config to " + xmlFile.path(), e);
            }
        }

        return xmlFile;
    }

    private void backUpConfig(final Resource xmlFile) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss").format(new Date());
        String backUpFileName = "geowebcache_%s.bak".formatted(timeStamp);
        Resource parentFile = xmlFile.parent();

        log.debug("Backing up config file {} to {}", xmlFile.name(), backUpFileName);

        List<Resource> previousBackUps = Resources.list(parentFile, res -> {
            String name = res.name();
            if (configFileName.equals(name)) {
                return false;
            }
            return name.startsWith(configFileName) && name.endsWith(".bak");
        });

        final int maxBackups = 10;
        if (previousBackUps.size() > maxBackups) {
            Collections.sort(previousBackUps, (o1, o2) -> (int) (o1.lastmodified() - o2.lastmodified()));
            Resource oldest = previousBackUps.getFirst();
            log.debug("Deleting oldest config backup {} to keep a maximum of {} backups.", oldest, maxBackups);
            oldest.delete();
        }

        Resource backUpFile = parentFile.get(backUpFileName);
        IOUtils.copy(xmlFile.in(), backUpFile.out());
        log.debug("Config backup done");
    }

    @Override
    public boolean hasInput() {
        try {
            return Resources.exists(findOrCreateConfFile());
        } catch (IOException _) {
            return false;
        }
    }

    @Override
    public boolean hasOutput() {
        return true;
    }
}
