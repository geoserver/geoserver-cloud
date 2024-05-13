-- Full Text Search over cataloginfo's jsonb info column
-- PostgreSQL 10 introduces Full Text Search on JSONB. https://www.postgresql.org/docs/current/functions-textsearch.html
-- The new FTS indexing on JSON works with phrase search and skips over both the JSON-markup and keys.
-- sample query: select * from cataloginfo where to_tsvector('simple', info::text) @@ plainto_tsquery('simple', 'layer1');

CREATE INDEX ON namespaceinfo USING gin ( to_tsvector('simple', info) );
CREATE INDEX ON workspaceinfo USING gin ( to_tsvector('simple', info) );
CREATE INDEX ON storeinfo USING gin ( to_tsvector('simple', info) );
CREATE INDEX ON resourceinfo USING gin ( to_tsvector('simple', info) );
CREATE INDEX ON layerinfo USING gin ( to_tsvector('simple', info) );
CREATE INDEX ON layergroupinfo USING gin ( to_tsvector('simple', info) );
CREATE INDEX ON styleinfo USING gin ( to_tsvector('simple', info) );


