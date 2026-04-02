local tables = {}

-- =========================
-- ADMIN STAGING
-- =========================
tables.admin_units_stage = osm2pgsql.define_table({
    name = 'admin_units_stage',
    ids = { type = 'any', id_column = 'osm_id' },
    columns = {
        { column = 'name', type = 'text' },
        { column = 'type', type = 'text' },
        { column = 'admin_level', type = 'int' },
        { column = 'population', type = 'bigint' },
        { column = 'geom', type = 'geometry', projection = 4326 }
    }
})

-- =========================
-- STREETS STAGING
-- =========================
tables.streets_stage = osm2pgsql.define_table({
    name = 'streets_stage',
    ids = { type = 'way', id_column = 'osm_id' },
    columns = {
        { column = 'name', type = 'text' },
        { column = 'alt_name', type = 'text' },
        { column = 'geom', type = 'linestring', projection = 4326 }
    }
})

-- =========================
-- HELPERS
-- =========================
local function get_place_type(tags)
    local p = tags.place
    if not p then return nil end
    if p == 'city' then return 'city' end
    if p == 'town' then return 'town' end
    if p == 'village' then return 'village' end
    if p == 'hamlet' then return 'hamlet' end
    if p == 'locality' then return 'locality' end
    return nil
end

local function get_admin_type(level)
    if level == '4' then return 'county' end
    if level == '8' then return 'uat' end
    return nil
end

local function parse_population(tags)
    if not tags.population then return nil end
    local cleaned = string.gsub(tags.population, "%s+", "")
    return tonumber(cleaned)
end

-- =========================
-- ADMIN RELATIONS
-- =========================
function osm2pgsql.process_relation(object)
    if object.tags.boundary == 'administrative' then
        local t = get_admin_type(object.tags.admin_level)
        if t and object.tags.name then
            local geom = object:as_multipolygon()
            if not geom then return end

            tables.admin_units_stage:insert({
                osm_id = 'r' .. object.id,
                name = object.tags.name,
                type = t,
                admin_level = tonumber(object.tags.admin_level),
                population = parse_population(object.tags),
                geom = geom
            })
        end
    end
end

-- =========================
-- ADMIN WAYS (fallback)
-- =========================
function osm2pgsql.process_way(object)

    -- streets
    if object.tags.highway and object.tags.name then
        local geom = object:as_linestring()
        if geom then
            tables.streets_stage:insert({
                osm_id = 'w' .. object.id,
                name = object.tags.name,
                alt_name = object.tags.alt_name,
                geom = geom
            })
        end
    end

    -- admin fallback
    if object.tags.boundary == 'administrative' then
        local t = get_admin_type(object.tags.admin_level)
        if t and object.tags.name and object.is_closed then
            local geom = object:as_multipolygon()
            if not geom then return end

            tables.admin_units_stage:insert({
                osm_id = 'w' .. object.id,
                name = object.tags.name,
                type = t,
                admin_level = tonumber(object.tags.admin_level),
                population = parse_population(object.tags),
                geom = geom
            })
        end
    end
end

-- =========================
-- PLACES (nodes)
-- =========================
function osm2pgsql.process_node(object)
    local place_type = get_place_type(object.tags)

    if place_type and object.tags.name then
        local geom = object:as_point()
        if not geom then return end

        tables.admin_units_stage:insert({
            osm_id = 'n' .. object.id,
            name = object.tags.name,
            type = place_type,
            population = parse_population(object.tags),
            geom = geom
        })
    end
end