-- V3_1_1__ResourceStore_UTC_mtime.sql
-- @since 3.1.0
--
-- Fix resourcestore_update_mtime() (from V1_4__ResourceStore_Tables.sql) and
-- resourcestore_update_parent_mtime() (as redefined by V1_6__ResourceStore_NoCTE.sql, which
-- renamed the "name" column to "path") in two ways.
--
-- 1. Stamp mtime in UTC. The mtime column is TIMESTAMP WITHOUT TIME ZONE. resourcestore's
-- explicit UPSERT path already writes timezone('UTC'::text, now()), but these two trigger
-- functions wrote plain now(), which is a timestamptz cast into the column using the session's
-- TimeZone setting rather than UTC. Whenever the session TimeZone isn't UTC, resources touched
-- only by these triggers (a bare UPDATE outside the UPSERT path, and a parent whose mtime is
-- bumped after a child delete) get an mtime offset from the value the application later reads
-- back assuming UTC, defeating any comparison against a wall-clock time (such as
-- FileSystemResourceStoreCache's no-clobber rule).
--
-- 2. Fix the moved-resource branch of resourcestore_update_parent_mtime(). It previously ran
-- UPDATE resourcestore SET mtime = NEW.mtime WHERE id = OLD.parentid OR parentid = NEW.parentid,
-- which bumps the old parent directory and every row already inside the new parent directory
-- (the moved row's new siblings), but never the new parent directory itself. The second condition
-- is now id = NEW.parentid: the new parent directory is the one that gets bumped, and its existing
-- children are left alone.

CREATE OR REPLACE FUNCTION resourcestore_update_mtime() RETURNS trigger LANGUAGE plpgsql AS
$$BEGIN
  RAISE DEBUG 'resourcestore_update_mtime: %, %', OLD, NEW;
  IF OLD.mtime = NEW.mtime THEN
    RAISE DEBUG 'update mtime % to % on %', OLD.mtime, NEW.mtime, NEW.id;
    NEW.mtime = timezone('UTC'::text, now());
  ELSE
    RAISE DEBUG 'mtime set explicitly to % on % (old mtime: %)', NEW.mtime, NEW.id, OLD.mtime;
  END IF;
  RETURN NEW;
END;$$;

CREATE OR REPLACE FUNCTION resourcestore_update_parent_mtime() RETURNS trigger LANGUAGE plpgsql AS
$$BEGIN
  RAISE DEBUG 'resourcestore_update_parent_mtime: %, %', OLD, NEW;
  IF NEW IS NULL THEN -- delete
    RAISE DEBUG 'updating mtime to % on parent (%) on delete of % (%)', OLD.mtime, OLD.parentid, OLD.id, OLD.path;
    UPDATE resourcestore SET mtime = timezone('UTC'::text, now()) WHERE id = OLD.parentid;
  ELSIF OLD IS NULL THEN -- insert
    RAISE DEBUG 'updating mtime to % on parent (%) on insert of % (%)', NEW.mtime, NEW.parentid, NEW.id, NEW.path;
    UPDATE resourcestore SET mtime = NEW.mtime WHERE id = NEW.parentid;
  ELSIF OLD.parentid <> NEW.parentid THEN -- moved
    RAISE DEBUG '% parent moved from % to %, updating mtime to % on both', NEW.id, NEW.mtime, OLD.parentid, NEW.parentid;
	  UPDATE resourcestore SET mtime = NEW.mtime WHERE id = OLD.parentid OR id = NEW.parentid;
	ELSIF OLD.mtime <> NEW.mtime THEN
    RAISE DEBUG '% updated, parent %, does not trigger update of parent mtime', NEW.id, NEW.parentid;
  END IF;
  RETURN NEW;
END;$$;
