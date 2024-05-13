/*
 * Support indexed queries by layerinfo's styles.id, layergroupinfo's styles.id, and layergroupinfo's layers.id
 * 
 * @since 1.8.1 
 */

-- Add styles column to layerinfo and a GIN index to contain the style ids
-- when the populate_table_columns_from_jsonb trigger runs
ALTER TABLE layerinfo ADD COLUMN styles TEXT[];
CREATE INDEX layerinfo_styles_gin_idx ON layerinfo USING GIN (styles);

-- Add layers column to layergroupinfo and a GIN index to contain the style ids
-- when the populate_table_columns_from_jsonb trigger runs
ALTER TABLE layergroupinfo ADD COLUMN layers TEXT[];
CREATE INDEX layergroupinfo_layers_gin_idx ON layergroupinfo USING GIN (layers);

-- Add styles column to layergroupinfo and a GIN index to contain the layer ids
-- when the populate_table_columns_from_jsonb trigger runs
ALTER TABLE layergroupinfo ADD COLUMN styles TEXT[];
CREATE INDEX layergroupinfo_styles_gin_idx ON layergroupinfo USING GIN (styles);

-- Force running the populate_table_columns_from_jsonb trigger and update the new columns
UPDATE layerinfo SET info = info;
UPDATE layergroupinfo SET info = info;

CREATE OR REPLACE VIEW layerinfos
AS
  SELECT layer.id                        AS id,
         layer."@type"                   AS "@type",
         -- override layer.name with resource.name while it's coupled in the object model
         -- layer.name                      AS name,
         resource.name                   AS name,
         resource.title                  AS title,
         resource.enabled                AS enabled,
         resource.advertised             AS advertised,
         layer."defaultStyle"            AS "defaultStyle.id",
         layer."type"                    AS "type",
         style.name                      AS "defaultStyle.name",
         resource.id                     AS "resource.id",
         resource.name                   AS "resource.name",
         resource."store.workspace.name" || ':' || layer.name  AS "prefixedName",
         resource.enabled                AS "resource.enabled",
         resource.advertised             AS "resource.advertised",
         resource."SRS"                  AS "resource.SRS",
         resource."store.id"             AS "resource.store.id",
         resource."store.name"           AS "resource.store.name",
         resource."store.enabled"        AS "resource.store.enabled",
         resource."store.type"           AS "resource.store.type",
         resource."store.workspace.id"   AS "resource.store.workspace.id",
         resource."store.workspace.name" AS "resource.store.workspace.name",
         resource."namespace.id"         AS "resource.namespace.id",
         resource."namespace.prefix"     AS "resource.namespace.prefix",
         style.filename                  AS "defaultStyle.filename",
         style.format                    AS "defaultStyle.format",
         layer.info                      AS publishedinfo,
         resource.resource               AS resource,
         resource.store                  AS store,
         resource.workspace              AS workspace,
         resource.namespace              AS namespace,
         style.info                      AS "defaultStyle",
         layer.styles                    AS "styles.id"
  FROM layerinfo layer
  INNER JOIN resourceinfos resource ON layer.resource = resource.id
  LEFT OUTER JOIN styleinfo style ON layer."defaultStyle" = style.id;

CREATE OR REPLACE VIEW layergroupinfos
AS
  SELECT lg.id             AS id,
         lg."@type"        AS "@type",
         lg.name           AS name,
         lg.title          AS title,
         'GROUP'           AS "type",
         concat_ws(':', workspace.name, lg.name) AS "prefixedName",
         lg.mode           AS mode,
         lg.enabled        AS enabled,
         lg.advertised     AS advertised,
         lg.workspace      AS "workspace.id",
         workspace.name    AS "workspace.name",
         lg.info           AS publishedinfo,
         workspace.info    AS workspace,
         lg.layers         AS "layers.id",
         lg.styles         AS "styles.id"
  FROM layergroupinfo lg
  LEFT OUTER JOIN workspaceinfo workspace
  ON lg.workspace = workspace.id;
