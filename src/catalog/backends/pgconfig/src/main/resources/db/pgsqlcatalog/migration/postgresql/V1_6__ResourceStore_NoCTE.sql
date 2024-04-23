/*
 * Change the resourcestore table to hold the full path instead of just the name,
 * so no recursive queries are required to query. And delete the resources view.
 * The caveat being the ResourceStore implementation is hence in charge of 
 * transactionally update the path of all children when a directory resource is
 * renamed/moved, but the perfomance gain is huge.
 * 
 * @since 1.8 
 */
ALTER TABLE resourcestore ADD COLUMN tmp TEXT;
UPDATE resourcestore SET tmp = v.path FROM resources v WHERE resourcestore.id = v.id;
DROP VIEW resources;
ALTER TABLE resourcestore RENAME COLUMN name TO path;
UPDATE resourcestore SET path = tmp;
ALTER TABLE resourcestore DROP COLUMN tmp;

ALTER INDEX resourcestore_name_idx RENAME TO resourcestore_path_idx;
ALTER INDEX resourcestore_parentid_name_key RENAME TO resourcestore_parentid_path_key;
DROP INDEX resourcestore_parent_name_idx;

-- change OLD.name to OLD.path
CREATE OR REPLACE FUNCTION resourcestore_type_readonly() RETURNS trigger LANGUAGE plpgsql AS
$$BEGIN
  IF NEW."type" <> OLD."type" THEN
    RAISE EXCEPTION 'resourcestore.type is read-only, trying to upate % to %s on % (%)', OLD."type", NEW."type", NEW.id, OLD.path;
  END IF;
  RETURN NEW;
END;$$;

-- change OLD.name to OLD.path and NEW.name to NEW.path
CREATE OR REPLACE FUNCTION resourcestore_update_parent_mtime() RETURNS trigger LANGUAGE plpgsql AS
$$BEGIN
  RAISE DEBUG 'resourcestore_update_parent_mtime: %, %', OLD, NEW;
  IF NEW IS NULL THEN -- delete
    RAISE DEBUG 'updating mtime to % on parent (%) on delete of % (%)', OLD.mtime, OLD.parentid, OLD.id, OLD.path;
    UPDATE resourcestore SET mtime = now() WHERE id = OLD.parentid;
  ELSIF OLD IS NULL THEN -- insert
    RAISE DEBUG 'updating mtime to % on parent (%) on insert of % (%)', NEW.mtime, NEW.parentid, NEW.id, NEW.path;
    UPDATE resourcestore SET mtime = NEW.mtime WHERE id = NEW.parentid;
  ELSIF OLD.parentid <> NEW.parentid THEN -- moved
    RAISE DEBUG '% parent moved from % to %, updating mtime to % on both', NEW.id, NEW.mtime, OLD.parentid, NEW.parentid;
	  UPDATE resourcestore SET mtime = NEW.mtime WHERE id = OLD.parentid OR parentid = NEW.parentid;
	ELSIF OLD.mtime <> NEW.mtime THEN
    RAISE DEBUG '% updated, parent %, does not trigger update of parent mtime', NEW.id, NEW.parentid;
  END IF;
  RETURN NEW;
END;$$;