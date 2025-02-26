-- V2_0_0__PublishedInfos_Materialized_Table.sql
-- @since 2.27.0.0

/*
 * Purpose:
 * Creates and indexes the publishedinfos_mat and tilelayers_mat materialized tables, and
 * establishes triggers to keep them synchronized with changes to layerinfo, layergroupinfo,
 * workspaceinfo, namespaceinfo, storeinfo, resourceinfo, and styleinfo. This ensures real-time
 * consistency with the publishedinfos and tilelayers views.
 *
 * Table Initialization and Indexing:
 * - publishedinfos_mat and tilelayers_mat are initialized using SELECT * INTO from their
 *   respective views (publishedinfos and tilelayers).
 * - Indexes are created to optimize query performance on common fields (e.g., id, @type, name)
 *   and array fields (e.g., styles.id, layers.id) using GIN indexes where applicable.
 * - Partial indexes (e.g., WHERE column IS NOT NULL) are used for columns with high NULL
 *   prevalence (e.g., mode, workspace.id) to reduce size and improve selectivity.
 *
 * tilelayers_mat Synchronization:
 * tilelayers_mat syncs from publishedinfos_mat instead of directly from publishedinfo or other
 * base tables for several reasons:
 * 1. Dependency: Tile layers (tilelayers_mat) are a filtered subset of published infos
 *    (publishedinfos_mat), defined by a non-NULL tilelayer column in publishedinfo. Syncing
 *    from publishedinfos_mat ensures consistency and reuses existing logic.
 * 2. Simplified Maintenance: A single trigger on publishedinfos_mat avoids duplicating logic
 *    across multiple base tables (e.g., layerinfo, layergroupinfo).
 * 3. Future Scalability: Updates to publishedinfos_mat from additional CatalogInfo objects
 *    (e.g., workspaces, namespaces) automatically propagate to tilelayers_mat.
 * 4. Efficiency: Minimal overhead given the low number of layer groups and tile layers,
 *    avoiding data duplication in publishedinfos_mat.
 *
 * Trigger Logic:
 * - A shared function, sync_publishedinfos_by_ids, updates publishedinfos_mat for a given set
 *   of publishedinfo IDs.
 * - Triggers on layerinfo and layergroupinfo handle INSERT, UPDATE, DELETE to sync
 *   publishedinfos_mat directly.
 * - Triggers on workspaceinfo, namespaceinfo, storeinfo, resourceinfo handle UPDATE, while
 *   styleinfo handles UPDATE and DELETE, reflecting application logic where INSERT and DELETE
 *   are managed separately.
 * - The publishedinfos_mat trigger syncs tilelayers_mat based on changes to its rows.
 *
 * Index Cleanup:
 * - Drops unneeded indexes from publishedinfo (enabled_idx, advertised_idx, info_idx if exists),
 *   layerinfo (name_idx, has_tilelayer, styles_gin_idx, to_tsvector_idx, type_idx), and
 *   layergroupinfo (enabled_idx, advertised_idx, name_idx, has_tilelayer, layers_gin_idx,
 *   styles_gin_idx, to_tsvector_idx, type_idx) as queries now use publishedinfos_mat and
 *   tilelayers_mat, reducing write overhead without impacting query performance.
 * - Retains primary keys, foreign key indexes (e.g., resource_idx, workspace_idx), and unique
 *   constraints (e.g., resource_name_key, workspace_name_key) for triggers and integrity.
 *
 * Why This Strategy:
 * - Efficiency: Triggers are tailored to application behavior (e.g., no INSERT/DELETE for most
 *   CatalogInfo tables due to prior publishedinfo management).
 * - Simplicity: Centralized sync logic reduces code duplication.
 * - Safety: Styleinfo DELETE trigger ensures consistency for edge cases.
 * - Real-Time Updates: Immediate synchronization supports querying needs.
 *
 * Note: The @type column is excluded from updates in publishedinfos_mat triggers since it is
 * immutable, set as 'LayerInfo' or 'LayerGroupInfo' based on the source table.
 */

-- Drop existing tables if they exist to ensure a clean slate
DROP TABLE IF EXISTS publishedinfos_mat;
DROP TABLE IF EXISTS tilelayers_mat;

-- Create and populate tables from their respective views
SELECT * INTO publishedinfos_mat FROM publishedinfos;
SELECT * INTO tilelayers_mat FROM tilelayers;

-- Add primary key constraints
ALTER TABLE publishedinfos_mat ADD PRIMARY KEY (id);
ALTER TABLE tilelayers_mat ADD PRIMARY KEY (id);

-- Indexes for publishedinfos_mat (30 indexes with partials)
CREATE INDEX publishedinfos_mat_id_idx ON publishedinfos_mat ("id");
CREATE INDEX publishedinfos_mat_infotype_idx ON publishedinfos_mat ("@type");
CREATE INDEX publishedinfos_mat_name_idx ON publishedinfos_mat (name);
CREATE INDEX publishedinfos_mat_prefixedname_idx ON publishedinfos_mat ("prefixedName");
CREATE INDEX publishedinfos_mat_title_idx ON publishedinfos_mat (title);
CREATE INDEX publishedinfos_mat_enabled_idx ON publishedinfos_mat (enabled);
CREATE INDEX publishedinfos_mat_advertised_idx ON publishedinfos_mat (advertised);
CREATE INDEX publishedinfos_mat_type_idx ON publishedinfos_mat ("type");
CREATE INDEX publishedinfos_mat_resource_id_idx ON publishedinfos_mat ("resource.id");
CREATE INDEX publishedinfos_mat_resource_name_idx ON publishedinfos_mat ("resource.name");
CREATE INDEX publishedinfos_mat_resource_enabled_idx ON publishedinfos_mat ("resource.enabled");
CREATE INDEX publishedinfos_mat_resource_advertised_idx ON publishedinfos_mat ("resource.advertised");
CREATE INDEX publishedinfos_mat_resource_srs_idx ON publishedinfos_mat ("resource.SRS");
CREATE INDEX publishedinfos_mat_resource_store_id_idx ON publishedinfos_mat ("resource.store.id");
CREATE INDEX publishedinfos_mat_resource_store_name_idx ON publishedinfos_mat ("resource.store.name");
CREATE INDEX publishedinfos_mat_resource_store_enabled_idx ON publishedinfos_mat ("resource.store.enabled");
CREATE INDEX publishedinfos_mat_resource_store_type_idx ON publishedinfos_mat ("resource.store.type");
CREATE INDEX publishedinfos_mat_resource_store_workspace_id_idx ON publishedinfos_mat ("resource.store.workspace.id");
CREATE INDEX publishedinfos_mat_resource_store_workspace_name_idx ON publishedinfos_mat ("resource.store.workspace.name");
CREATE INDEX publishedinfos_mat_resource_namespace_id_idx ON publishedinfos_mat ("resource.namespace.id");
CREATE INDEX publishedinfos_mat_resource_namespace_prefix_idx ON publishedinfos_mat ("resource.namespace.prefix");
CREATE INDEX publishedinfos_mat_defaultstyle_id_idx ON publishedinfos_mat ("defaultStyle.id");
CREATE INDEX publishedinfos_mat_defaultstyle_name_idx ON publishedinfos_mat ("defaultStyle.name");
CREATE INDEX publishedinfos_mat_defaultstyle_filename_idx ON publishedinfos_mat ("defaultStyle.filename");
CREATE INDEX publishedinfos_mat_defaultstyle_format_idx ON publishedinfos_mat ("defaultStyle.format");
CREATE INDEX publishedinfos_mat_workspace_id_not_null_idx ON publishedinfos_mat ("workspace.id") WHERE "workspace.id" IS NOT NULL;
CREATE INDEX publishedinfos_mat_workspace_name_not_null_idx ON publishedinfos_mat ("workspace.name") WHERE "workspace.name" IS NOT NULL;
CREATE INDEX publishedinfos_mat_styles_id_idx ON publishedinfos_mat USING GIN ("styles.id");
CREATE INDEX publishedinfos_mat_layers_id_not_null_idx ON publishedinfos_mat USING GIN ("layers.id") WHERE "layers.id" IS NOT NULL;
CREATE INDEX publishedinfos_mat_mode_not_null_idx ON publishedinfos_mat (mode) WHERE mode IS NOT NULL;

-- Indexes for tilelayers_mat (8 indexes preserved)
CREATE INDEX tilelayers_mat_id_idx ON tilelayers_mat ("id");
CREATE INDEX tilelayers_mat_infotype_idx ON tilelayers_mat ("@type");
CREATE INDEX tilelayers_mat_name_idx ON tilelayers_mat (name);
CREATE INDEX tilelayers_mat_enabled_idx ON tilelayers_mat (enabled);
CREATE INDEX tilelayers_mat_advertised_idx ON tilelayers_mat (advertised);
CREATE INDEX tilelayers_mat_type_idx ON tilelayers_mat ("type");
CREATE INDEX tilelayers_mat_workspace_name_idx ON tilelayers_mat ("workspace.name");
CREATE INDEX tilelayers_mat_published_name_idx ON tilelayers_mat ("published.name");

-- Shared function to sync publishedinfos_mat by IDs
CREATE OR REPLACE FUNCTION sync_publishedinfos_by_ids(ids TEXT[]) RETURNS VOID AS
$$
BEGIN
  INSERT INTO publishedinfos_mat
  SELECT * FROM publishedinfos WHERE id = ANY(ids)
  ON CONFLICT (id) DO UPDATE SET
    (name, "prefixedName", title, enabled, advertised, "type", workspace, publishedinfo,
     "styles.id", "resource.id", "resource.name", "resource.enabled", "resource.advertised",
     "resource.SRS", "resource.store.id", "resource.store.name", "resource.store.enabled",
     "resource.store.type", "resource.store.workspace.id", "resource.store.workspace.name",
     "resource.namespace.id", "resource.namespace.prefix", "defaultStyle.id", "defaultStyle.name",
     "defaultStyle.filename", "defaultStyle.format", resource, store, namespace, "defaultStyle",
     mode, "workspace.id", "workspace.name", "layers.id")
    = (EXCLUDED.name, EXCLUDED."prefixedName", EXCLUDED.title, EXCLUDED.enabled, EXCLUDED.advertised,
       EXCLUDED."type", EXCLUDED.workspace, EXCLUDED.publishedinfo, EXCLUDED."styles.id",
       EXCLUDED."resource.id", EXCLUDED."resource.name", EXCLUDED."resource.enabled",
       EXCLUDED."resource.advertised", EXCLUDED."resource.SRS", EXCLUDED."resource.store.id",
       EXCLUDED."resource.store.name", EXCLUDED."resource.store.enabled", EXCLUDED."resource.store.type",
       EXCLUDED."resource.store.workspace.id", EXCLUDED."resource.store.workspace.name",
       EXCLUDED."resource.namespace.id", EXCLUDED."resource.namespace.prefix", EXCLUDED."defaultStyle.id",
       EXCLUDED."defaultStyle.name", EXCLUDED."defaultStyle.filename", EXCLUDED."defaultStyle.format",
       EXCLUDED.resource, EXCLUDED.store, EXCLUDED.namespace, EXCLUDED."defaultStyle", EXCLUDED.mode,
       EXCLUDED."workspace.id", EXCLUDED."workspace.name", EXCLUDED."layers.id");
END
$$ LANGUAGE plpgsql;

-- Trigger function for layerinfo INSERT, UPDATE, DELETE
CREATE OR REPLACE FUNCTION sync_layerinfo_to_publishedinfos_mat() RETURNS TRIGGER AS
$$
BEGIN
  IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
    PERFORM sync_publishedinfos_by_ids(ARRAY[NEW.id]);
  ELSIF TG_OP = 'DELETE' THEN
    DELETE FROM publishedinfos_mat WHERE id = OLD.id;
  END IF;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER layerinfo_sync_to_publishedinfos_mat
  AFTER INSERT OR UPDATE OR DELETE ON layerinfo
  FOR EACH ROW EXECUTE FUNCTION sync_layerinfo_to_publishedinfos_mat();

-- Trigger function for layergroupinfo INSERT, UPDATE, DELETE
CREATE OR REPLACE FUNCTION sync_layergroupinfo_to_publishedinfos_mat() RETURNS TRIGGER AS
$$
BEGIN
  IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
    PERFORM sync_publishedinfos_by_ids(ARRAY[NEW.id]);
  ELSIF TG_OP = 'DELETE' THEN
    DELETE FROM publishedinfos_mat WHERE id = OLD.id;
  END IF;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER layergroupinfo_sync_to_publishedinfos_mat
  AFTER INSERT OR UPDATE OR DELETE ON layergroupinfo
  FOR EACH ROW EXECUTE FUNCTION sync_layergroupinfo_to_publishedinfos_mat();

-- Trigger function for workspaceinfo UPDATE
CREATE OR REPLACE FUNCTION workspaceinfo_update_trigger() RETURNS TRIGGER AS
$$
DECLARE
  affected_ids TEXT[];
BEGIN
  -- LayerGroupInfo IDs where workspace matches
  SELECT array_agg(id) INTO affected_ids
  FROM layergroupinfo WHERE workspace = NEW.id;
  
  -- LayerInfo IDs via resource.store.workspace
  SELECT array_agg(l.id) INTO affected_ids
  FROM layerinfo l
  INNER JOIN resourceinfo r ON l.resource = r.id
  INNER JOIN storeinfo s ON r.store = s.id
  WHERE s.workspace = NEW.id;
  
  IF affected_ids IS NOT NULL THEN
    PERFORM sync_publishedinfos_by_ids(affected_ids);
  END IF;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER workspaceinfo_update_trigger
  AFTER UPDATE ON workspaceinfo
  FOR EACH ROW EXECUTE FUNCTION workspaceinfo_update_trigger();

-- Trigger function for namespaceinfo UPDATE
CREATE OR REPLACE FUNCTION namespaceinfo_update_trigger() RETURNS TRIGGER AS
$$
DECLARE
  affected_ids TEXT[];
BEGIN
  -- LayerInfo IDs via resource.namespace
  SELECT array_agg(l.id) INTO affected_ids
  FROM layerinfo l
  INNER JOIN resourceinfo r ON l.resource = r.id
  WHERE r.namespace = NEW.id;
  
  IF affected_ids IS NOT NULL THEN
    PERFORM sync_publishedinfos_by_ids(affected_ids);
  END IF;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER namespaceinfo_update_trigger
  AFTER UPDATE ON namespaceinfo
  FOR EACH ROW EXECUTE FUNCTION namespaceinfo_update_trigger();

-- Trigger function for storeinfo UPDATE
CREATE OR REPLACE FUNCTION storeinfo_update_trigger() RETURNS TRIGGER AS
$$
DECLARE
  affected_ids TEXT[];
BEGIN
  -- LayerInfo IDs via resource.store
  SELECT array_agg(l.id) INTO affected_ids
  FROM layerinfo l
  INNER JOIN resourceinfo r ON l.resource = r.id
  WHERE r.store = NEW.id;
  
  IF affected_ids IS NOT NULL THEN
    PERFORM sync_publishedinfos_by_ids(affected_ids);
  END IF;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER storeinfo_update_trigger
  AFTER UPDATE ON storeinfo
  FOR EACH ROW EXECUTE FUNCTION storeinfo_update_trigger();

-- Trigger function for resourceinfo UPDATE
CREATE OR REPLACE FUNCTION resourceinfo_update_trigger() RETURNS TRIGGER AS
$$
DECLARE
  affected_ids TEXT[];
BEGIN
  -- LayerInfo IDs where resource matches
  SELECT array_agg(id) INTO affected_ids
  FROM layerinfo WHERE resource = NEW.id;
  
  IF affected_ids IS NOT NULL THEN
    PERFORM sync_publishedinfos_by_ids(affected_ids);
  END IF;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER resourceinfo_update_trigger
  AFTER UPDATE ON resourceinfo
  FOR EACH ROW EXECUTE FUNCTION resourceinfo_update_trigger();

-- Trigger function for styleinfo UPDATE and DELETE
CREATE OR REPLACE FUNCTION styleinfo_update_delete_trigger() RETURNS TRIGGER AS
$$
DECLARE
  affected_ids TEXT[];
BEGIN
  IF TG_OP = 'DELETE' THEN
    SELECT array_agg(id) INTO affected_ids
    FROM layerinfo WHERE "defaultStyle" = OLD.id;
  ELSE
    SELECT array_agg(id) INTO affected_ids
    FROM layerinfo WHERE "defaultStyle" = NEW.id;
  END IF;
  
  IF affected_ids IS NOT NULL THEN
    PERFORM sync_publishedinfos_by_ids(affected_ids);
  END IF;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER styleinfo_update_delete_trigger
  AFTER UPDATE OR DELETE ON styleinfo
  FOR EACH ROW EXECUTE FUNCTION styleinfo_update_delete_trigger();

-- Trigger function to sync tilelayers_mat from publishedinfos_mat
CREATE OR REPLACE FUNCTION sync_publishedinfos_mat_to_tilelayers_mat() RETURNS TRIGGER AS
$$
BEGIN
  IF TG_OP = 'INSERT' THEN
    IF EXISTS (SELECT 1 FROM publishedinfo WHERE id = NEW.id AND tilelayer IS NOT NULL) THEN
      INSERT INTO tilelayers_mat
      SELECT t.* FROM tilelayers t
      INNER JOIN publishedinfos_mat p ON t.id = p.id
      WHERE p.id = NEW.id
      ON CONFLICT (id) DO UPDATE SET
        (id, "@type", name, enabled, advertised, "type", "workspace.name", "published.name",
         tilelayer, workspace, namespace, store, resource, publishedinfo, "defaultStyle")
        = (EXCLUDED.id, EXCLUDED."@type", EXCLUDED.name, EXCLUDED.enabled, EXCLUDED.advertised,
           EXCLUDED."type", EXCLUDED."workspace.name", EXCLUDED."published.name", EXCLUDED.tilelayer,
           EXCLUDED.workspace, EXCLUDED.namespace, EXCLUDED.store, EXCLUDED.resource,
           EXCLUDED.publishedinfo, EXCLUDED."defaultStyle");
    END IF;
  ELSIF TG_OP = 'UPDATE' THEN
    IF EXISTS (SELECT 1 FROM publishedinfo WHERE id = NEW.id AND tilelayer IS NOT NULL) THEN
      INSERT INTO tilelayers_mat
      SELECT t.* FROM tilelayers t
      INNER JOIN publishedinfos_mat p ON t.id = p.id
      WHERE p.id = NEW.id
      ON CONFLICT (id) DO UPDATE SET
        (id, "@type", name, enabled, advertised, "type", "workspace.name", "published.name",
         tilelayer, workspace, namespace, store, resource, publishedinfo, "defaultStyle")
        = (EXCLUDED.id, EXCLUDED."@type", EXCLUDED.name, EXCLUDED.enabled, EXCLUDED.advertised,
           EXCLUDED."type", EXCLUDED."workspace.name", EXCLUDED."published.name", EXCLUDED.tilelayer,
           EXCLUDED.workspace, EXCLUDED.namespace, EXCLUDED.store, EXCLUDED.resource,
           EXCLUDED.publishedinfo, EXCLUDED."defaultStyle");
    ELSE
      DELETE FROM tilelayers_mat WHERE id = NEW.id;
    END IF;
  ELSIF TG_OP = 'DELETE' THEN
    DELETE FROM tilelayers_mat WHERE id = OLD.id;
  END IF;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER publishedinfos_mat_sync_to_tilelayers_mat
  AFTER INSERT OR UPDATE OR DELETE ON publishedinfos_mat
  FOR EACH ROW EXECUTE FUNCTION sync_publishedinfos_mat_to_tilelayers_mat();

-- Drop unneeded indexes from publishedinfo, layerinfo, and layergroupinfo now that queries use publishedinfos_mat
DROP INDEX IF EXISTS publishedinfo_enabled_idx;
DROP INDEX IF EXISTS publishedinfo_advertised_idx;
DROP INDEX IF EXISTS publishedinfo_info_idx;
-- layerinfo indexes
DROP INDEX IF EXISTS layerinfo_defaultstyle_idx;
DROP INDEX IF EXISTS layerinfo_styles_gin_idx;
-- layergroupinfo indexes
DROP INDEX IF EXISTS layergroupinfo_enabled_idx;
DROP INDEX IF EXISTS layergroupinfo_advertised_idx;
DROP INDEX IF EXISTS layergroupinfo_layers_gin_idx;
DROP INDEX IF EXISTS layergroupinfo_styles_gin_idx;
