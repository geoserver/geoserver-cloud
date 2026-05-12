-- V3_2_1__Fix_WorkspaceInfo_Update_Trigger.sql
-- @since 2.28.3
--
-- Fix workspaceinfo_update_trigger() introduced in V2_0_0__PublishedInfos_Materialized_Table.sql.
-- The original implementation collected the affected LayerGroupInfo IDs into
-- affected_ids and then immediately overwrote that variable with the LayerInfo IDs in
-- the very next SELECT INTO. As a result, LayerGroupInfo IDs were silently dropped:
-- renaming a workspace did not refresh publishedinfos_mat (nor tilelayers_mat) for any
-- LayerGroupInfo belonging to that workspace, leaving stale workspace.name /
-- prefixedName values until publishedinfos_mat was rebuilt.
--
-- Combine the two queries with UNION so both kinds of publishedinfo are refreshed.

CREATE OR REPLACE FUNCTION workspaceinfo_update_trigger() RETURNS TRIGGER AS
$$
DECLARE
  affected_ids TEXT[];
BEGIN
  WITH ids AS (
    -- LayerGroupInfo IDs directly owned by this workspace
    SELECT id FROM layergroupinfo WHERE workspace = NEW.id
    UNION
    -- LayerInfo IDs reached via resource.store.workspace
    SELECT l.id
      FROM layerinfo l
      INNER JOIN resourceinfo r ON l.resource = r.id
      INNER JOIN storeinfo     s ON r.store = s.id
     WHERE s.workspace = NEW.id
  )
  SELECT array_agg(id) INTO affected_ids FROM ids;

  IF affected_ids IS NOT NULL THEN
    PERFORM sync_publishedinfos_by_ids(affected_ids);
  END IF;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;
