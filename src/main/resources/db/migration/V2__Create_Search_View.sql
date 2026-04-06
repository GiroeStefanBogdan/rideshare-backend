CREATE VIEW location_search AS
SELECT
    id,
    name,
    full_name,
    'UAT' AS location_type
FROM admin_search

UNION ALL

SELECT
    id,
    name,
    full_name,
    'STREET' AS location_type
FROM street_search;
