/**
 * Views for bulk queries, contains related info objects
 */
CREATE VIEW workspaceinfos
AS
  SELECT id             AS id,
         "@type"        AS "@type",
         name           AS name,
         info           AS workspace,
         default_workspace,
         default_store
  FROM workspaceinfo;

CREATE VIEW namespaceinfos
AS
  SELECT id,
         "@type",
         name,
         uri,
         isolated,
         info           AS namespace,
         default_namespace
  FROM namespaceinfo;

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
         workspace.info       AS workspace,
         workspace.default_store
  FROM storeinfo store
  INNER JOIN workspaceinfo workspace
  ON store.workspace = workspace.id;

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
  
CREATE VIEW styleinfos
AS
  SELECT style.id             AS id,
         style."@type"        AS "@type",
         style.name           AS name,
         style.filename       AS filename,
         style.format         AS format,
         style.workspace      AS "workspace.id",
         workspace.name       AS "workspace.name",
         style.info           AS style,
         workspace.info       AS workspace
  FROM styleinfo style
  LEFT OUTER JOIN workspaceinfo workspace
  ON style.workspace = workspace.id;

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

CREATE VIEW layergroupinfos
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
         workspace.info    AS workspace
  FROM layergroupinfo lg
  LEFT OUTER JOIN workspaceinfo workspace
  ON lg.workspace = workspace.id;

  

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

