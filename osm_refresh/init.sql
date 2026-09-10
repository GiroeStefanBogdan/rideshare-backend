\set ON_ERROR_STOP on

DO $$
BEGIN
    IF EXISTS (SELECT FROM admin_units) OR EXISTS (SELECT FROM streets) OR EXISTS (SELECT FROM ride) THEN
        RAISE EXCEPTION 'OSM initialization requires empty admin_units, streets, and ride tables';
    END IF;
END $$;

\ir update.sql
