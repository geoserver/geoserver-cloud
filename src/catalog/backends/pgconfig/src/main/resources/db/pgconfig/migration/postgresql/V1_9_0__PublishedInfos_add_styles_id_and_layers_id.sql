/*
 * Add styles.id TEXT[] and layers.id TEXT[] columns to the publishedinfos view 
 */
DROP VIEW tilelayers;
DROP VIEW publishedinfos;
CREATE OR REPLACE VIEW publishedinfos
AS
  SELECT 
    -- common PublishedInfo properties
         id, "@type", name, "prefixedName", title, enabled, advertised, "type", workspace, publishedinfo, "styles.id",
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
         NULL AS "workspace.name",
         NULL AS "layers.id"
  FROM layerinfos
UNION
  SELECT 
    -- common PublishedInfo properties
         id, "@type", name, "prefixedName", title, enabled, advertised, "type", workspace, publishedinfo, "styles.id",
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
         "workspace.name",
         "layers.id"
  FROM layergroupinfos;

/*
 * re-create tilelayers
 */  
CREATE OR REPLACE VIEW tilelayers
AS
  SELECT
    -- queryable fields
	p."id",
	p."@type",
	v."prefixedName" AS name,
	v.enabled,
	v.advertised,
	v."type",
	CASE
		WHEN v."workspace.id" IS NOT NULL THEN v."workspace.name"
		WHEN v."workspace.id" IS NULL THEN v."resource.store.workspace.name"
	END AS "workspace.name",
	p.name AS "published.name",
	-- jsonb fields, required to get the tilelayer and its publishedinfo in full
	p.tilelayer,
	v.workspace,
	v.namespace,
	v.store,
	v.resource,
	p.info as publishedinfo,
	v."defaultStyle"
	FROM publishedinfo p
	INNER JOIN publishedinfos v ON p.id = v.id
	WHERE p.tilelayer IS NOT NULL;
