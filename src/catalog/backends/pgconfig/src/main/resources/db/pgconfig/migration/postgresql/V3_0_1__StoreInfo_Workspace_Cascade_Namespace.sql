-- V3_0_1__StoreInfo_Workspace_Cascade_Namespace.sql
-- @since 2.28.3
--
-- Purpose:
-- When a storeinfo row's workspace column changes, cascade the change to every
-- resourceinfo belonging to that store so its namespace matches the workspace's
-- namespace (resolved by name == workspace name, the invariant maintained by
-- org.geoserver.catalog.impl.CatalogImpl#findNamespaceChange).
--
-- Background:
-- CatalogPlugin.save(StoreInfo) already cascades namespace updates to resources
-- when a store's workspace changes. This trigger is defense-in-depth: it ensures
-- the invariant holds even if some future code path (REST import, XStream
-- restore, direct SQL, in-process tooling) updates storeinfo.workspace without
-- going through CatalogPlugin.save. Without it, resourceinfo rows can drift out
-- of sync with their store's workspace, breaking layer prefixedName, the
-- pgconfig.publishedinfos view, and the pgconfig.publishedinfos_mat
-- materialized table.
--
-- Note on JSON shape:
-- ResourceInfo.namespace is encoded in the resourceinfo.info JSONB column as a
-- plain string ID (e.g. {"namespace": "NamespaceInfo-...."}). Updating
-- resourceinfo.info with jsonb_set fires the existing BEFORE UPDATE trigger
-- resourceinfo_populate, which calls populate_table_columns_from_jsonb() to
-- keep the denormalized resourceinfo.namespace FK column in sync.

CREATE OR REPLACE FUNCTION cascade_store_workspace_to_resource_namespace() RETURNS TRIGGER AS
$func$
DECLARE
  new_namespace_id TEXT;
BEGIN
  SELECT n.id
    INTO new_namespace_id
    FROM namespaceinfo n
    INNER JOIN workspaceinfo w ON w.name = n.name
   WHERE w.id = NEW.workspace;

  IF new_namespace_id IS NULL THEN
    RAISE EXCEPTION
      'No namespace matches workspace % (id %) - cannot cascade store % namespace',
      (SELECT name FROM workspaceinfo WHERE id = NEW.workspace),
      NEW.workspace,
      NEW.id;
  END IF;

  -- Update the JSON; the existing resourceinfo_populate BEFORE trigger then
  -- copies the new namespace id into the resourceinfo.namespace FK column.
  UPDATE resourceinfo
     SET info = jsonb_set(info, '{namespace}', to_jsonb(new_namespace_id))
   WHERE store = NEW.id
     AND namespace IS DISTINCT FROM new_namespace_id;

  RETURN NEW;
END
$func$ LANGUAGE plpgsql;

-- Fire on every storeinfo UPDATE, not "UPDATE OF workspace": the writable path
-- updates the info JSONB column (UPDATE storeinfo SET info = ...) and the existing
-- BEFORE trigger storeinfo_populate derives the workspace FK column from the new
-- JSON. The workspace column is therefore never directly mentioned in a SET clause,
-- so an "UPDATE OF workspace" trigger would never fire in production. The WHEN
-- clause is evaluated AFTER the BEFORE triggers run, so OLD.workspace and
-- NEW.workspace correctly compare the persisted workspace before and after.
CREATE TRIGGER storeinfo_cascade_workspace_to_resource_namespace
  AFTER UPDATE ON storeinfo
  FOR EACH ROW
  WHEN (NEW.workspace IS DISTINCT FROM OLD.workspace)
  EXECUTE FUNCTION cascade_store_workspace_to_resource_namespace();

-- One-time repair for catalogs that already have resourceinfo rows whose
-- namespace drifted away from their store's workspace's namespace. Mirrors
-- the invariant CatalogPlugin#findNamespaceChange relies on (namespace name
-- equals workspace name). Updating resourceinfo.info fires resourceinfo_populate,
-- which keeps the FK column consistent, and resourceinfo_update_trigger, which
-- refreshes publishedinfos_mat.
WITH expected AS (
  SELECT r.id           AS resource_id,
         n.id           AS expected_namespace_id
    FROM resourceinfo r
    INNER JOIN storeinfo     s ON r.store = s.id
    INNER JOIN workspaceinfo w ON s.workspace = w.id
    INNER JOIN namespaceinfo n ON n.name = w.name
   WHERE r.namespace <> n.id
)
UPDATE resourceinfo r
   SET info = jsonb_set(r.info, '{namespace}', to_jsonb(e.expected_namespace_id))
  FROM expected e
 WHERE r.id = e.resource_id;
