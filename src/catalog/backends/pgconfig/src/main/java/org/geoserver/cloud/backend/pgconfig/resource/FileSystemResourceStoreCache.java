package org.geoserver.cloud.backend.pgconfig.resource;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileSystemUtils;

@Slf4j
public class FileSystemResourceStoreCache implements DisposableBean {

    private final Path base;
    private boolean disposable;
    private @Getter FileSystemResourceStore localOnlyStore;

    private FileSystemResourceStoreCache(@NonNull Path cacheDirectory, boolean disposable) {
        this.disposable = disposable;
        Preconditions.checkArgument(
                Files.isDirectory(cacheDirectory),
                "Cache directory is not a directory: %s",
                cacheDirectory.toAbsolutePath());
        Preconditions.checkArgument(
                Files.isWritable(cacheDirectory),
                "Cache directory is not writable: %s",
                cacheDirectory.toAbsolutePath());
        this.base = cacheDirectory;
        this.localOnlyStore = new FileSystemResourceStore(new File(this.base.toUri()));
    }

    @SneakyThrows
    public static @NonNull FileSystemResourceStoreCache newTempDirInstance() {
        boolean disposable = true;
        Path tempDirectory = Files.createTempDirectory("pgconfig_resourcestore_cache");
        return new FileSystemResourceStoreCache(tempDirectory, disposable);
    }

    public static @NonNull FileSystemResourceStoreCache of(@NonNull Path cacheDirectory) {
        boolean disposable = false;
        return new FileSystemResourceStoreCache(cacheDirectory, disposable);
    }

    @Override
    public void destroy() {
        if (disposable && Files.isDirectory(this.base)) {
            try {
                log.info("Deleting resource store cache directory {}", base);
                FileSystemUtils.deleteRecursively(base);
                log.info("Resource store cache directory {} deleted", base);
            } catch (IOException e) {
                log.warn("Error deleting resource cache {}", base, e);
            }
        }
    }

    @SneakyThrows
    public File getFile(PgconfigResource resource) {
        final Path path = ensureFileExists(resource);
        final long fileMtime = getLastmodified(path);
        final long resourceMtime = resource.lastmodified();
        if (fileMtime != resourceMtime) {
            dump(resource);
        }
        return path.toFile();
    }

    private long getLastmodified(final Path path) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        return attr.lastModifiedTime().toMillis();
    }

    public Path ensureFileExists(PgconfigResource resource) throws IOException {
        Preconditions.checkArgument(resource.isFile());
        Path path = toPath(resource);
        if (!Files.exists(path)) {
            ensureDirectoryExists(path.getParent());
            Files.createFile(path);
        }
        return path;
    }

    @SneakyThrows
    public File getDirectory(PgconfigResource resource) {
        return ensureDirectory(resource).toFile();
    }

    @SneakyThrows
    public Path ensureDirectory(PgconfigResource resource) {
        Preconditions.checkArgument(resource.isDirectory());
        Path path = toPath(resource);
        return ensureDirectoryExists(path);
    }

    private Path ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    @SneakyThrows
    private Path dump(PgconfigResource resource) {
        try (InputStream in = resource.in()) {
            return dump(resource, in);
        }
    }

    @SneakyThrows
    public Path dump(PgconfigResource resource, InputStream in) {
        Path file = ensureFileExists(resource);
        Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        Files.setLastModifiedTime(file, FileTime.fromMillis(resource.lastmodified()));
        return file;
    }

    public void updateAll(List<Resource> list) {
        list.stream()
                .map(PgconfigResource.class::cast)
                .filter(PgconfigResource::isDirectory)
                .forEach(this::ensureDirectory);

        list.stream()
                .map(PgconfigResource.class::cast)
                .filter(PgconfigResource::isFile)
                .forEach(this::dump);
    }

    private Path toPath(PgconfigResource resource) {
        return base.resolve(resource.path());
    }

    @SneakyThrows
    public void moved(@NonNull PgconfigResource source, @NonNull PgconfigResource target) {
        Path sourcePath = toPath(source);
        if (Files.exists(sourcePath)) {
            Path targetPath = toPath(target);
            ensureDirectoryExists(targetPath.getParent());
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
