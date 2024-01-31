/*
 * 
 */
CREATE SEQUENCE IF NOT EXISTS gs_update_sequence AS BIGINT CYCLE;
SELECT NEXTVAL('gs_update_sequence');

/*
 * 
 */
CREATE TABLE IF NOT EXISTS geoserverinfo(
  updatesequence  BIGINT NOT NULL DEFAULT 0,
  info            JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS settingsinfo(
  id         TEXT NOT NULL PRIMARY KEY,
  workspace  TEXT NOT NULL,
  info       JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS serviceinfo(
  id          TEXT NOT NULL PRIMARY KEY,
  "@type"     TEXT NOT NULL,
  name        TEXT NOT NULL,
  workspace   TEXT,
  info        JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS logginginfo(
  info        JSONB NOT NULL
);

CREATE TRIGGER geoserverinfo_populate BEFORE INSERT OR UPDATE ON geoserverinfo FOR EACH ROW EXECUTE PROCEDURE populate_table_columns_from_jsonb();
CREATE TRIGGER settingsinfo_populate BEFORE INSERT OR UPDATE ON settingsinfo FOR EACH ROW EXECUTE PROCEDURE populate_table_columns_from_jsonb();
CREATE TRIGGER serviceinfo_populate BEFORE INSERT OR UPDATE ON serviceinfo FOR EACH ROW EXECUTE PROCEDURE populate_table_columns_from_jsonb();
CREATE TRIGGER logginginfo_populate BEFORE INSERT OR UPDATE ON logginginfo FOR EACH ROW EXECUTE PROCEDURE populate_table_columns_from_jsonb();

CREATE VIEW settingsinfos
AS
  SELECT s.id,
         s.workspace  AS "workspace.id",
         s.info,
         w.info AS workspace
  FROM settingsinfo s
  INNER JOIN workspaceinfo w ON s.workspace = w.id;

CREATE VIEW serviceinfos
AS
  SELECT s.id,
         s."@type",
         s.name,
         s.workspace  AS "workspace.id",
         s.info,
         w.info AS workspace
  FROM serviceinfo s
  LEFT OUTER JOIN workspaceinfo w ON s.workspace = w.id;
  