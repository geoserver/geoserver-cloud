package org.geoserver.cloud.gwc.config.blobstore;

import lombok.Getter;
import lombok.NonNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * {@link Testcontainers} container for AWS Azurite blobstore test environment.
 *
 * <p>Runs the <a href=
 * "https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite?toc=%2Fazure%2Fstorage%2Fblobs%2Ftoc.json&bc=%2Fazure%2Fstorage%2Fblobs%2Fbreadcrumb%2Ftoc.json&tabs=docker-hub">Azurite
 * emulator</a> for local Azure Storage development with testcontainers.
 *
 * <p>Azurite accepts the same well-known account and key used by the legacy Azure Storage Emulator.
 *
 * <ul>
 *   <li>Account name: {@code devstoreaccount1}
 *   <li>Account key: {@code
 *       Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==}
 * </ul>
 */
public class AzuriteContainer extends GenericContainer<AzuriteContainer> {

    private static final @NonNull DockerImageName IMAGE_NAME =
            DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:latest");

    public final @Getter String accountName = "devstoreaccount1";
    public final @Getter String accountKey =
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    private final int blobsPort = 10_000;

    public AzuriteContainer() {
        super(IMAGE_NAME);
        super.setWaitStrategy(Wait.forListeningPort());
        super.addExposedPort(blobsPort);
    }

    public int getBlobsPort() {
        return super.getMappedPort(blobsPort);
    }

    public String getBlobsUrl() {
        return "http://localhost:%d/%s".formatted(getBlobsPort(), getAccountName());
    }
}
