CREATE TABLE resources
(
  oid serial NOT NULL,
  name character varying NOT NULL,
  parent integer,
  last_modified timestamp without time zone NOT NULL DEFAULT timezone('UTC'::text, now()),
  content bytea,
  CONSTRAINT resources_pkey PRIMARY KEY (oid),
  CONSTRAINT resources_parent_fkey FOREIGN KEY (parent)
      REFERENCES resources (oid)
      ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT resources_parent_name_key UNIQUE (parent, name),
  CONSTRAINT resources_only_one_root_check CHECK (parent IS NOT NULL OR oid = 0)
);

CREATE INDEX resources_parent_name_idx
  ON resources (parent NULLS FIRST, name NULLS FIRST);

INSERT INTO resources (oid, name, parent, content) VALUES (0, '', NULL, NULL);

-- only the required table(s) to support distributed locking, from 
-- spring-integration-jdbc-<version>.jar/org/springframework/integration/jdbc/schema-postgresql.sql
-- Table named RESOURCE_LOCK instead of INT_LOCK
CREATE TABLE RESOURCE_LOCK  (
  LOCK_KEY CHAR(36) NOT NULL,
  REGION VARCHAR(100) NOT NULL,
  CLIENT_ID CHAR(36),
  CREATED_DATE TIMESTAMP NOT NULL,
  constraint LOCK_PK primary key (LOCK_KEY, REGION)
);
