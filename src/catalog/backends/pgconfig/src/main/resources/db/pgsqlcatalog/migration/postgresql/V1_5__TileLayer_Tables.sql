ALTER TABLE publishedinfo ADD COLUMN tilelayer jsonb;
create index publishedinfo_has_tilelayer on publishedinfo ((tilelayer is not null));
create index layerinfo_has_tilelayer on layerinfo ((tilelayer is not null));
create index layergroupinfo_has_tilelayer on layergroupinfo ((tilelayer is not null));

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
