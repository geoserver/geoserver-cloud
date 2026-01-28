/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.config.blobstore;

import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.aiven.testcontainers.fakegcsserver.FakeGcsServerContainer;
import jakarta.annotation.Nullable;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.locks.MemoryLockProvider;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageBlobStore;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageBlobStoreInfo;
import org.junit.jupiter.api.Assumptions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

public class GoogleCloudStorageContainerSupport {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String PROJECT_ID = "test-project";

    public static final DockerImageName IMAGE_NAME = DockerImageName.parse("fsouza/fake-gcs-server:latest");

    private FakeGcsServerContainer gcsEmulator;

    private String emulatorEndpoint;

    public void after() {
        if (gcsEmulator != null) {
            gcsEmulator.stop();
            gcsEmulator = null;
        }
    }

    public void before() throws Exception {
        // Skip tests if Docker is not available
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available, skipping Testcontainers tests.");

        gcsEmulator = new FakeGcsServerContainer(IMAGE_NAME);

        // Wait for the emulator to be ready
        gcsEmulator.start();

        // Configure the Storage client to use the emulator
        String emulatorHost = gcsEmulator.getHost();
        Integer emulatorPort = gcsEmulator.getFirstMappedPort();
        emulatorEndpoint = "http://" + emulatorHost + ":" + emulatorPort;

        // Create Storage client
        try (Storage storage = StorageOptions.newBuilder()
                .setProjectId(PROJECT_ID)
                .setHost(emulatorEndpoint)
                .build()
                .getService()) {

            // Create a bucket
            BucketInfo bucketInfo = BucketInfo.newBuilder(BUCKET_NAME).build();
            storage.create(bucketInfo);
        }
    }

    public String getEmulatorEndpoint() {
        return emulatorEndpoint;
    }

    public String getBucket() {
        return BUCKET_NAME;
    }

    public String getProjectId() {
        return PROJECT_ID;
    }

    public GoogleCloudStorageBlobStoreInfo createBlobstoreInfo() {
        GoogleCloudStorageBlobStoreInfo info = new GoogleCloudStorageBlobStoreInfo();

        info.setEndpointUrl(getEmulatorEndpoint());
        info.setBucket(getBucket());
        info.setProjectId(getProjectId());
        return info;
    }

    public GoogleCloudStorageBlobStoreInfo createBlobstoreInfo(String prefix) {
        GoogleCloudStorageBlobStoreInfo info = createBlobstoreInfo();
        info.setPrefix(prefix);
        return info;
    }

    public GoogleCloudStorageBlobStore createBlobStore(TileLayerDispatcher layers) throws StorageException {
        return createBlobStore(null, layers);
    }

    public GoogleCloudStorageBlobStore createBlobStore(@Nullable String prefix, TileLayerDispatcher layers)
            throws StorageException {
        GoogleCloudStorageBlobStoreInfo info = createBlobstoreInfo(prefix);
        return info.createInstance(layers, new MemoryLockProvider());
    }
}
