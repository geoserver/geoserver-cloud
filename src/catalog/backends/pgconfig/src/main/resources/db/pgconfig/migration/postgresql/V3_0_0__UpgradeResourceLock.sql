/*
 * Upgrade the RESOURCE_LOCK table first defined in V1_4__ResourceStore_Tables
 * after the migration to Spring Boot 4 / spring-integration-jdbc 7.0.2.
 *
 * The EXPIRED_AFTER column is required by spring-integration-jdbc 7.0.2 for
 * lock expiration support.
 *
 * A DEFAULT value is provided for backwards compatibility: if a rollback to
 * geoserver-cloud 2.28.2.0 (spring-integration-jdbc 6.x) is needed, the old
 * code can still insert rows without specifying this column. The default of
 * CURRENT_TIMESTAMP means any such locks would be considered immediately
 * expired, which is a safe fallback for orphaned locks.
 */
ALTER TABLE RESOURCE_LOCK ADD COLUMN IF NOT EXISTS EXPIRED_AFTER TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

