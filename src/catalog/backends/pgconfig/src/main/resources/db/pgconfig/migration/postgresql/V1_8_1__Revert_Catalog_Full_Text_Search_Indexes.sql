-- Drop indexes created in V1_2__Catalog_Full_Text_Search.sql as they're not used
-- We'll address the full text search capability in a future iteration accounting for
-- both the database schema and code changes required.

DROP INDEX IF EXISTS namespaceinfo_to_tsvector_idx;
DROP INDEX IF EXISTS workspaceinfo_to_tsvector_idx;
DROP INDEX IF EXISTS storeinfo_to_tsvector_idx;
DROP INDEX IF EXISTS resourceinfo_to_tsvector_idx;
DROP INDEX IF EXISTS layerinfo_to_tsvector_idx;
DROP INDEX IF EXISTS layergroupinfo_to_tsvector_idx;
DROP INDEX IF EXISTS styleinfo_to_tsvector_idx;


