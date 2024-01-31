/*
 * Set up CatalogInfo tables with only enough columns to support referential integrity
 * The triggers calling populate_table_columns_from_jsonb() will populate the "@type"
 * column from cataloginfo tables. All JSON representations of CatalogInfo subtyles contain
 * an "@type" attribute with a string value matching one of the constants in this enum.
 */
CREATE TYPE infotype AS ENUM (
 'WorkspaceInfo',
 'NamespaceInfo',
 'DataStoreInfo',
 'CoverageStoreInfo',
 'WMSStoreInfo',
 'WMTSStoreInfo',
 'FeatureTypeInfo',
 'CoverageInfo',
 'WMSLayerInfo',
 'WMTSLayerInfo',
 'LayerInfo',
 'LayerGroupInfo',
 'StyleInfo',
 'MapInfo');

CREATE CAST (character varying AS infotype) WITH INOUT AS ASSIGNMENT;

/*
 * Function for cataloginfo table triggers that update table columns from json field values 
 */
CREATE OR REPLACE FUNCTION populate_table_columns_from_jsonb() RETURNS TRIGGER AS
$func$
BEGIN
   NEW := jsonb_populate_record(NEW, NEW.info);
   RETURN NEW;
END
$func$ LANGUAGE plpgsql;

/*
 * "Abstract" base table for CatalogInfo types
 */
CREATE TABLE IF NOT EXISTS cataloginfo(
  id         TEXT NOT NULL PRIMARY KEY,
  "@type"    infotype NOT NULL,
  name       TEXT NOT NULL,
  info       JSONB NOT NULL,
--  info_tsvector tsvector generated always as (jsonb_to_tsvector('simple', info, '["string"]')) stored
  CHECK (false) NO INHERIT -- Make it abstract, can't insert on it directly
);

/*
 * namespaceinfo inherits cataloginfo
 */
CREATE TABLE IF NOT EXISTS namespaceinfo(
  "@type"   infotype NOT NULL DEFAULT 'NamespaceInfo',
  uri       TEXT NOT NULL,
  isolated  BOOLEAN NOT NULL DEFAULT FALSE,
  default_namespace BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  UNIQUE(name),
  CHECK ("@type"  = 'NamespaceInfo')
) INHERITS (cataloginfo);

CREATE TRIGGER namespaceinfo_populate BEFORE INSERT OR UPDATE ON namespaceinfo FOR EACH ROW EXECUTE PROCEDURE populate_table_columns_from_jsonb();

CREATE INDEX namespaceinfo_type_idx ON namespaceinfo ("@type");
CREATE INDEX namespaceinfo_name_idx ON namespaceinfo (name);
CREATE INDEX namespaceinfo_uri_idx ON namespaceinfo (uri);
CREATE INDEX namespaceinfo_isolated_idx ON namespaceinfo (isolated);
CREATE INDEX namespaceinfo_default_namespace_idx ON namespaceinfo (default_namespace);

/*
 * workspaceinfo inherits cataloginfo
 */
CREATE TABLE IF NOT EXISTS workspaceinfo(
  "@type"    infotype NOT NULL DEFAULT 'WorkspaceInfo',
  isolated   BOOLEAN NOT NULL DEFAULT FALSE,
  default_workspace BOOLEAN NOT NULL DEFAULT FALSE,
  default_store TEXT,
  PRIMARY KEY (id),
  UNIQUE(name),
  CHECK ("@type" = 'WorkspaceInfo')
) INHERITS (cataloginfo);

CREATE TRIGGER workspaceinfo_populate BEFORE INSERT OR UPDATE ON workspaceinfo FOR EACH ROW EXECUTE PROCEDURE populate_table_columns_from_jsonb();

CREATE INDEX workspaceinfo_type_idx ON workspaceinfo ("@type");
CREATE INDEX workspaceinfo_name_idx ON workspaceinfo (name);
CREATE INDEX workspaceinfo_isolated_idx ON workspaceinfo (isolated);
CREATE INDEX workspaceinfo_default_workspace_idx ON workspaceinfo (default_workspace);
CREATE INDEX workspaceinfo_default_store_idx ON workspaceinfo (default_store);

/*
 * storeinfo inherits cataloginfo
 */
CREATE TABLE IF NOT EXISTS storeinfo(
  workspace  TEXT NOT NULL,
  "type"     TEXT,
  enabled    boolean NOT NULL DEFAULT true,
  PRIMARY KEY (id),
  UNIQUE(workspace, name),
  FOREIGN KEY (workspace) REFERENCES workspaceinfo(id),
  CHECK ("@type" IN('DataStoreInfo', 'CoverageStoreInfo', 'WMSStoreInfo', 'WMTSStoreInfo'))
) INHERITS (cataloginfo);

CREATE TRIGGER storeinfo_populate	BEFORE INSERT OR UPDATE ON storeinfo FOR EACH ROW	EXECUTE PROCEDURE populate_table_columns_from_jsonb();

CREATE INDEX storeinfo_type_idx ON storeinfo ("@type");
CREATE INDEX storeinfo_name_idx ON storeinfo (name);
CREATE INDEX storeinfo_storetype_idx ON storeinfo ("type");
CREATE INDEX storeinfo_workspace_idx ON storeinfo (workspace);

/*
 * resourceinfo inherits cataloginfo.
 */
CREATE TABLE IF NOT EXISTS resourceinfo(
  store         TEXT NOT NULL,
  namespace     TEXT NOT NULL,
  title         TEXT,
  enabled       boolean NOT NULL DEFAULT true,
  advertised    boolean NOT NULL DEFAULT true,
  "SRS"         TEXT,
  PRIMARY KEY (id),
  UNIQUE(namespace, name),
  FOREIGN KEY (store) REFERENCES storeinfo(id),
  FOREIGN KEY (namespace) REFERENCES namespaceinfo(id),
  CHECK ("@type" IN('FeatureTypeInfo', 'CoverageInfo', 'WMSLayerInfo', 'WMTSLayerInfo'))
) INHERITS (cataloginfo);

CREATE TRIGGER resourceinfo_populate BEFORE INSERT OR UPDATE ON resourceinfo FOR EACH ROW EXECUTE PROCEDURE populate_table_columns_from_jsonb();

CREATE INDEX resourceinfo_type_idx ON resourceinfo ("@type");
CREATE INDEX resourceinfo_name_idx ON resourceinfo (name);
CREATE INDEX resourceinfo_store_idx ON resourceinfo (store);
CREATE INDEX resourceinfo_namespace_idx ON resourceinfo (namespace);
CREATE INDEX resourceinfo_title_idx ON resourceinfo (title);
CREATE INDEX resourceinfo_enabled_idx ON resourceinfo (enabled);
CREATE INDEX resourceinfo_advertised_idx ON resourceinfo (advertised);
CREATE INDEX resourceinfo_srs_idx ON resourceinfo ("SRS");

/*
 * styleinfo inherits cataloginfo
 */
CREATE TABLE IF NOT EXISTS styleinfo(
  "@type"    infotype NOT NULL DEFAULT 'StyleInfo',
  workspace  TEXT NULL,
  filename   TEXT NULL,
  format     TEXT NULL,
  PRIMARY KEY (id),
  UNIQUE NULLS NOT DISTINCT(workspace, name),
  FOREIGN KEY (workspace) REFERENCES workspaceinfo(id),
  CHECK ("@type" = 'StyleInfo')
) INHERITS (cataloginfo);

CREATE TRIGGER style_populate_fields_trigger
  BEFORE INSERT OR UPDATE ON styleinfo FOR EACH ROW
  EXECUTE PROCEDURE populate_table_columns_from_jsonb();

CREATE INDEX styleinfo_type_idx ON styleinfo ("@type");
CREATE INDEX styleinfo_name_idx ON styleinfo (name);
CREATE INDEX styleinfo_workspace_idx ON styleinfo (workspace);
CREATE INDEX styleinfo_filename_idx ON styleinfo (filename);
CREATE INDEX styleinfo_format_idx ON styleinfo (format);

/*
 * publishedinfo inherits cataloginfo. Base table for layerinfo and layergroupinfo.
 */
CREATE TABLE IF NOT EXISTS publishedinfo(
  PRIMARY KEY (id),
  CHECK (false) NO INHERIT -- Make it abstract, can't insert on it directly
) INHERITS (cataloginfo);

/*
 * layerinfo inherits publishedinfo
 */
CREATE TABLE IF NOT EXISTS layerinfo(
  "@type"        infotype NOT NULL DEFAULT 'LayerInfo',
  resource       TEXT NOT NULL,
  "defaultStyle" TEXT,
  "type"         TEXT,
  PRIMARY KEY (id),
  UNIQUE(resource, name),
  FOREIGN KEY (resource) REFERENCES resourceinfo(id),
  FOREIGN KEY ("defaultStyle") REFERENCES styleinfo(id),
  CHECK ("@type" = 'LayerInfo')
) INHERITS (publishedinfo);

CREATE TRIGGER layerinfo_populate_fields_trigger
	BEFORE INSERT OR UPDATE ON layerinfo FOR EACH ROW
	EXECUTE PROCEDURE populate_table_columns_from_jsonb();

CREATE INDEX layerinfo_type_idx ON layerinfo ("@type");
CREATE INDEX layerinfo_name_idx ON layerinfo (name);
CREATE INDEX layerinfo_resource_idx ON layerinfo (resource);
CREATE INDEX layerinfo_defaultstyle_idx ON layerinfo ("defaultStyle");

/*
 * layergroupinfo inherits publishedinfo
 */
CREATE TABLE IF NOT EXISTS layergroupinfo(
  "@type"     infotype NOT NULL DEFAULT 'LayerGroupInfo',
  workspace   TEXT NULL,
  title       TEXT NULL,
  enabled     BOOLEAN NOT NULL DEFAULT TRUE,
  advertised  BOOLEAN NOT NULL DEFAULT TRUE,
  mode        TEXT NULL,
  PRIMARY KEY (id),
  UNIQUE NULLS NOT DISTINCT (workspace, name),
  FOREIGN KEY (workspace) REFERENCES workspaceinfo(id),
  CHECK ("@type" = 'LayerGroupInfo')
) INHERITS (publishedinfo);

CREATE TRIGGER layergroup_populate_fields_trigger
  BEFORE INSERT OR UPDATE ON layergroupinfo FOR EACH ROW
  EXECUTE PROCEDURE populate_table_columns_from_jsonb();

CREATE INDEX layergroupinfo_type_idx ON layergroupinfo ("@type");
CREATE INDEX layergroupinfo_name_idx ON layergroupinfo (name);
CREATE INDEX layergroupinfo_workspace_idx ON layergroupinfo (workspace);
CREATE INDEX layergroupinfo_enabled_idx ON layergroupinfo (enabled);
CREATE INDEX layergroupinfo_advertised_idx ON layergroupinfo (advertised);

