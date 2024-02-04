DROP VIEW storeinfos CASCADE;

-- re-create storeinfos without the default_store column
CREATE VIEW storeinfos
AS
  SELECT store.id             AS id,
         store."@type"        AS "@type",
         store.name           AS name,
         store.enabled        AS enabled,
         store."type"         AS "type",
         store.workspace      AS "workspace.id",
         workspace.name       AS "workspace.name",
         workspace.isolated   AS "workspace.isolated",
         store.info           AS store,
         workspace.info       AS workspace
  FROM storeinfo store
  INNER JOIN workspaceinfo workspace
  ON store.workspace = workspace.id;

-- re-create cascade deleted views resourceinfos, layerinfos, publishedinfos
  
CREATE VIEW resourceinfos
AS
  SELECT resource.id             AS id,
         resource."@type"        AS "@type",
         resource.name           AS name,
         resource.title          AS title,
         resource.enabled        AS enabled,
         resource.advertised     AS advertised,
         resource."SRS"          AS "SRS",
         stores.id               AS "store.id",
         stores.name             AS "store.name",
         stores.enabled          AS "store.enabled",
         stores."type"           AS "store.type",
         stores."workspace.id"   AS "store.workspace.id",
         stores."workspace.name" AS "store.workspace.name",
         stores."workspace.isolated" AS "store.workspace.isolated",
         resource.namespace      AS "namespace.id",
         namespace.name          AS "namespace.prefix",
         namespace.uri           AS "namespace.uri",
         namespace.isolated      AS "namespace.isolated",
         resource.info           AS resource,
         stores.store            AS store,
         stores.workspace        AS workspace,
         namespace.info          AS namespace
  FROM resourceinfo resource
  INNER JOIN storeinfos stores ON resource.store = stores.id
  INNER JOIN namespaceinfo namespace ON resource.namespace = namespace.id;

CREATE VIEW layerinfos
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
         style.info                      AS "defaultStyle"
  FROM layerinfo layer
  INNER JOIN resourceinfos resource ON layer.resource = resource.id
  LEFT OUTER JOIN styleinfo style ON layer."defaultStyle" = style.id;

CREATE VIEW publishedinfos
AS
  SELECT 
    -- common PublishedInfo properties
         id, "@type", name, "prefixedName", title, enabled, advertised, "type", workspace, publishedinfo,
    -- LayerInfo specific properties
         "resource.id",
         "resource.name",
         "resource.enabled",
         "resource.advertised",
         "resource.SRS",
         "resource.store.id",
         "resource.store.name",
         "resource.store.enabled",
         "resource.store.type",
         "resource.store.workspace.id",
         "resource.store.workspace.name",
         "resource.namespace.id",
         "resource.namespace.prefix",
         "defaultStyle.id",
         "defaultStyle.name",
         "defaultStyle.filename",
         "defaultStyle.format",
         resource,
         store,
         namespace,
         "defaultStyle",
    -- LayerGroupInfo specific properties
         NULL AS mode,
         NULL AS "workspace.id",
         NULL AS "workspace.name"
  FROM layerinfos
UNION
  SELECT 
    -- common PublishedInfo properties
         id, "@type", name, "prefixedName", title, enabled, advertised, "type", workspace, publishedinfo,
    -- LayerInfo specific properties
         NULL AS "resource.id",
         NULL AS "resource.name",
         NULL AS "resource.enabled",
         NULL AS "resource.advertised",
         NULL AS "resource.SRS",
         NULL AS "resource.store.id",
         NULL AS "resource.store.name",
         NULL AS "resource.store.enabled",
         NULL AS "resource.store.type",
         NULL AS "resource.store.workspace.id",
         NULL AS "resource.store.workspace.name",
         NULL AS "resource.namespace.id",
         NULL AS "resource.namespace.prefix",
         NULL AS "defaultStyle.id",
         NULL AS "defaultStyle.name",
         NULL AS "defaultStyle.filename",
         NULL AS "defaultStyle.format",
         NULL AS resource,
         NULL AS store,
         NULL AS namespace,
         NULL AS "defaultStyle",
    -- LayerGroupInfo specific properties
         mode,
         "workspace.id",
         "workspace.name"
  FROM layergroupinfos;
