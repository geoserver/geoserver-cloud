/*
 * Table used to support distributed locks through spring-integration-jdbc in PgsqlLockProvider
 * Locking table definition from spring-integration-jdbc.jar!org/springframework/integration/jdbc/schema-postgresql.sql
 * original table name: INT_LOCK
 */
CREATE TABLE RESOURCE_LOCK  (
  LOCK_KEY CHAR(36) NOT NULL,
  REGION VARCHAR(100) NOT NULL,
  CLIENT_ID CHAR(36),
  CREATED_DATE TIMESTAMP NOT NULL,
  constraint INT_LOCK_PK primary key (LOCK_KEY, REGION)
);

/*
 * ResourceStore tables
 */

CREATE TYPE resourcetype AS ENUM (
 'DIRECTORY',
 'RESOURCE'
);

CREATE CAST (CHARACTER VARYING AS resourcetype) WITH INOUT AS ASSIGNMENT;

CREATE TABLE resourcestore (
  id        BIGSERIAL PRIMARY KEY,
  parentid	BIGINT NULL CHECK(id <> parentid),
  "type"    resourcetype,
  mtime     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT timezone('UTC'::text, now()),
  name      TEXT NOT NULL,
  content   BYTEA CHECK(("type" = 'DIRECTORY' AND content IS NULL) OR ("type" = 'RESOURCE' AND content IS NOT NULL)),
  CONSTRAINT resourcestore_parent_fkey FOREIGN KEY (parentid)
      REFERENCES resourcestore (id)
      ON UPDATE RESTRICT ON DELETE CASCADE,
  UNIQUE NULLS NOT DISTINCT (parentid, name),
  CONSTRAINT resourcestore_only_one_root_check CHECK (parentid IS NOT NULL OR id = 0)
);

CREATE INDEX resourcestore_name_idx ON resourcestore (name);
CREATE INDEX resourcestore_parent_name_idx ON resourcestore (parentid NULLS FIRST, name NULLS FIRST);

INSERT INTO resourcestore (id, name, parentid, "type") VALUES (0, '', NULL, 'DIRECTORY');

/*
 * Trigger that prevents upadting "type"
 */
CREATE FUNCTION resourcestore_type_readonly() RETURNS trigger LANGUAGE plpgsql AS
$$BEGIN
  IF NEW."type" <> OLD."type" THEN
    RAISE EXCEPTION 'resourcestore.type is read-only, trying to upate % to %s on % (%)', OLD."type", NEW."type", NEW.id, OLD.name;
  END IF;
  RETURN NEW;
END;$$;

CREATE TRIGGER resourcestore_type_readonly_trigger
  BEFORE UPDATE ON resourcestore
  FOR EACH ROW EXECUTE PROCEDURE resourcestore_type_readonly();

/*
 * Trigger that checks on insert and update that the parent is a directory
 */
CREATE FUNCTION resourcestore_parent_is_directory() RETURNS trigger AS
$BODY$
DECLARE 
  parent_type resourcetype;
BEGIN
  SELECT "type" INTO parent_type FROM resourcestore WHERE id = NEW.parentid;
  IF parent_type <> 'DIRECTORY' THEN
      RAISE EXCEPTION 'Parent is not a directory: parentid = %, type = %', NEW.parentid, parent_type;
  END IF;
  RETURN NEW;
END;
$BODY$
LANGUAGE plpgsql;

CREATE TRIGGER resourcestore_parent_is_directory_trigger
  BEFORE INSERT OR UPDATE ON resourcestore 
  FOR EACH ROW EXECUTE PROCEDURE resourcestore_parent_is_directory();

/*
 * Trigger that updates the mtime on update, unless it is explicitly being set
 * NOTE: set client_min_messages to 'debug'; to see the messages in psql
 */
CREATE FUNCTION resourcestore_update_mtime() RETURNS trigger LANGUAGE plpgsql AS
$$BEGIN
  RAISE DEBUG 'resourcestore_update_mtime: %, %', OLD, NEW;
  IF OLD.mtime = NEW.mtime THEN
    RAISE DEBUG 'update mtime % to % on %', OLD.mtime, NEW.mtime, NEW.id;
    NEW.mtime = now();
  ELSE
    RAISE DEBUG 'mtime set explicitly to % on % (old mtime: %)', NEW.mtime, NEW.id, OLD.mtime;
  END IF;
  RETURN NEW;
END;$$;

CREATE TRIGGER resourcestore_update_mtime_trigger
  BEFORE UPDATE ON resourcestore
  FOR EACH ROW EXECUTE PROCEDURE resourcestore_update_mtime();
  
/*
 * Trigger that updates the parent's mtime after a resource is created or deleted
 */
CREATE FUNCTION resourcestore_update_parent_mtime() RETURNS trigger LANGUAGE plpgsql AS
$$BEGIN
  RAISE DEBUG 'resourcestore_update_parent_mtime: %, %', OLD, NEW;
  IF NEW IS NULL THEN -- delete
    RAISE DEBUG 'updating mtime to % on parent (%) on delete of % (%)', OLD.mtime, OLD.parentid, OLD.id, OLD.name;
    UPDATE resourcestore SET mtime = now() WHERE id = OLD.parentid;
  ELSIF OLD IS NULL THEN -- insert
    RAISE DEBUG 'updating mtime to % on parent (%) on insert of % (%)', NEW.mtime, NEW.parentid, NEW.id, NEW.name;
    UPDATE resourcestore SET mtime = NEW.mtime WHERE id = NEW.parentid;
  ELSIF OLD.parentid <> NEW.parentid THEN -- moved
    RAISE DEBUG '% parent moved from % to %, updating mtime to % on both', NEW.id, NEW.mtime, OLD.parentid, NEW.parentid;
	  UPDATE resourcestore SET mtime = NEW.mtime WHERE id = OLD.parentid OR parentid = NEW.parentid;
	ELSIF OLD.mtime <> NEW.mtime THEN
    RAISE DEBUG '% updated, parent %, does not trigger update of parent mtime', NEW.id, NEW.parentid;
  END IF;
  RETURN NEW;
END;$$;

CREATE TRIGGER resourcestore_update_parent_mtime_trigger
  AFTER INSERT OR UPDATE OR DELETE ON resourcestore
  FOR EACH ROW EXECUTE PROCEDURE resourcestore_update_parent_mtime();

CREATE OR REPLACE VIEW resources AS
  WITH RECURSIVE top_down AS (
    SELECT id, parentid, "type", name AS path, mtime, content
    FROM resourcestore
    WHERE parentid = 0
  UNION ALL
    SELECT t.id, t.parentid, t."type", concat_ws('/', r.path, t.name) AS path, t.mtime, t.content
    FROM resourcestore t
    JOIN top_down r ON t.parentid = r.id
)
SELECT id, parentid, "type", path, mtime, content 
FROM top_down
UNION SELECT id, parentid, "type", name AS path, mtime, content FROM resourcestore WHERE parentid IS NULL
ORDER BY path;

