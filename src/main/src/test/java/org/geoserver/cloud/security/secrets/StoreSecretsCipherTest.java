/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.security.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.security.secrets.SecurityManagerSupport.COVERAGE_SECRET;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;

/** Encryption of store secrets with the keystore and encoders of a real security manager. */
class StoreSecretsCipherTest {

    @TempDir
    File dataDirectory;

    @TempDir
    File otherDataDirectory;

    @Test
    void encryptedValueDecrypts() {
        runInitialized(dataDirectory, cipher -> {
            String encrypted = cipher.encrypt("s3cr3t").orElseThrow();

            assertThat(encrypted).startsWith("crypt1:");
            assertThat(cipher.isEncrypted(encrypted)).isTrue();
            assertThat(cipher.decrypt(encrypted)).contains("s3cr3t");
        });
    }

    @ParameterizedTest
    @CsvSource({"strongPbePasswordEncoder, crypt2:", "aesGcmPasswordEncoder, crypt3:"})
    void configEncrypterUsed(String encoderName, String prefix) {
        runWithEncrypter(encoderName, cipher -> {
            String encrypted = cipher.encrypt("s3cr3t").orElseThrow();

            assertThat(encrypted).startsWith(prefix);
            assertThat(cipher.decrypt(encrypted)).contains("s3cr3t");
        });
    }

    // a reset makes the next decryption create the encoders, as the first one after startup does
    @ParameterizedTest
    @ValueSource(strings = {"pbePasswordEncoder", "strongPbePasswordEncoder", "aesGcmPasswordEncoder"})
    void parallelFirstDecrypts(String encoderName) {
        runWithEncrypter(encoderName, cipher -> {
            String encrypted = cipher.encrypt("s3cr3t").orElseThrow();
            for (int round = 0; round < 20; round++) {
                cipher.reset();

                List<Optional<String>> decrypted = inParallel(16, () -> cipher.decrypt(encrypted));

                assertThat(decrypted).containsOnly(Optional.of("s3cr3t"));
            }
        });
    }

    @Test
    void encodersCreatedOnce() {
        EncoderInstances instances = new EncoderInstances();
        SecurityManagerSupport.contextRunner(dataDirectory)
                .withBean(EncoderInstances.class, () -> instances)
                .run(context -> {
                    GeoServerSecurityManager manager = SecurityManagerSupport.initialize(securityManager(context));
                    String encrypted = new StoreSecretsCipher(() -> manager)
                            .encrypt("s3cr3t")
                            .orElseThrow();
                    int oneSet = instances.createdBy(() -> new StoreSecretsCipher(() -> manager).decrypt(encrypted));

                    StoreSecretsCipher shared = new StoreSecretsCipher(() -> manager);
                    int created = instances.createdBy(() -> inParallel(16, () -> shared.decrypt(encrypted)));

                    assertThat(created).isEqualTo(oneSet);
                });
    }

    @Test
    void clearTextNotEncrypted() {
        runInitialized(dataDirectory, cipher -> {
            assertThat(cipher.isEncrypted("s3cr3t")).isFalse();
            assertThat(cipher.isEncrypted(null)).isFalse();
        });
    }

    // the keystore of another installation holds other keys
    @Test
    void foreignKeysFailDecryption() {
        AtomicReference<String> foreign = new AtomicReference<>();
        runInitialized(
                otherDataDirectory,
                cipher -> foreign.set(cipher.encrypt("s3cr3t").orElseThrow()));

        runInitialized(dataDirectory, cipher -> assertThat(cipher.decrypt(foreign.get()))
                .isEmpty());
    }

    @Test
    void noSecurityManagerYet() {
        StoreSecretsCipher cipher = new StoreSecretsCipher(() -> null);

        assertThat(cipher.canDecrypt()).isFalse();
        assertThat(cipher.canEncrypt()).isFalse();
        assertThat(cipher.isEncrypted("crypt1:abc")).isTrue();
        assertThat(cipher.isEncrypted("crypt2:abc")).isTrue();
        assertThat(cipher.isEncrypted("crypt3:abc")).isTrue();
        assertThat(cipher.isEncrypted("s3cr3t")).isFalse();
    }

    @Test
    void uninitializedCannotEncrypt() {
        SecurityManagerSupport.contextRunner(dataDirectory).run(context -> {
            StoreSecretsCipher cipher = new StoreSecretsCipher(() -> securityManager(context));

            assertThat(cipher.canDecrypt()).isTrue();
            assertThat(cipher.canEncrypt()).isFalse();
        });
    }

    @Test
    void providerParametersAreSecret() {
        runInitialized(dataDirectory, cipher -> {
            CoverageStoreInfoImpl coverageStore = new CoverageStoreInfoImpl((Catalog) null);
            WMSStoreInfoImpl wmsStore = new WMSStoreInfoImpl((Catalog) null);

            assertThat(cipher.secretParameters(coverageStore)).containsExactly(COVERAGE_SECRET);
            assertThat(cipher.secretParameters(wmsStore)).isEmpty();
        });
    }

    @FunctionalInterface
    interface CipherTest {
        void run(StoreSecretsCipher cipher) throws Exception;
    }

    private static void runInitialized(File dataDirectory, CipherTest test) {
        SecurityManagerSupport.contextRunner(dataDirectory).run(context -> {
            GeoServerSecurityManager manager = SecurityManagerSupport.initialize(securityManager(context));
            test.run(new StoreSecretsCipher(() -> manager));
        });
    }

    private void runWithEncrypter(String encoderName, CipherTest test) {
        SecurityManagerSupport.contextRunner(dataDirectory).run(context -> {
            GeoServerSecurityManager manager = SecurityManagerSupport.initialize(securityManager(context));
            SecurityManagerSupport.useConfigEncrypter(manager, encoderName);
            test.run(new StoreSecretsCipher(() -> manager));
        });
    }

    private static GeoServerSecurityManager securityManager(AssertableApplicationContext context) {
        return context.getBean(GeoServerSecurityManager.class);
    }

    /** Runs the task on that many threads released together, and returns their results. */
    private static <T> List<T> inParallel(int threads, Supplier<T> task) throws Exception {
        CyclicBarrier start = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.get();
                }));
            }
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    /** Counts the password encoder beans created by the context: the encoder beans are prototypes. */
    static class EncoderInstances implements BeanPostProcessor {

        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof GeoServerPasswordEncoder) {
                count.incrementAndGet();
            }
            return bean;
        }

        int createdBy(ThrowingRunnable task) throws Exception {
            int before = count.get();
            task.run();
            return count.get() - before;
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
