local function preferred_name(tags)
    local romanian_name = tags['name:ro']
    if romanian_name and romanian_name ~= '' then
        return romanian_name
    end
    return tags.name
end

-- 1. Define Staging Tables
local stage_admin = osm2pgsql.define_table({
    schema = 'osm_staging',
    name = 'stage_admin',
    ids = { type = 'any', type_column = 'osm_type', id_column = 'osm_id' },
    columns = {
        { column = 'source_name', type = 'text' },
        { column = 'admin_level', type = 'int4' },
        { column = 'geom', type = 'geometry', projection = 4326 }
    }
})

local stage_places = osm2pgsql.define_table({
    schema = 'osm_staging',
    name = 'stage_places',
    ids = { type = 'node', id_column = 'osm_id' },
    columns = {
        { column = 'source_name', type = 'text' },
        { column = 'place', type = 'text' },
        { column = 'population', type = 'int4' },
        { column = 'geom', type = 'geometry', projection = 4326 }
    }
})

local stage_streets = osm2pgsql.define_table({
    schema = 'osm_staging',
    name = 'stage_streets',
    ids = { type = 'way', id_column = 'osm_id' },
    columns = {
        { column = 'source_name', type = 'text' },
        { column = 'highway', type = 'text' },
        { column = 'access', type = 'text' },
        { column = 'motor_vehicle', type = 'text' },
        { column = 'geom', type = 'linestring', projection = 4326 }
    }
})

-- 2. Process Nodes (Villages, Towns, Localities without boundary relations)
function osm2pgsql.process_node(object)
    local source_name = preferred_name(object.tags)
    if source_name and object.tags.place then
        -- Expanded list to catch all relevant populated areas, neighborhoods, and isolated dwellings
        local valid_places = {
            city=true, town=true, village=true, hamlet=true,
            suburb=true, locality=true,
            neighbourhood=true, quarter=true
        }

        if valid_places[object.tags.place] then
            local pop = tonumber(object.tags.population)
            stage_places:insert({
                source_name = source_name,
                place = object.tags.place,
                population = pop,
                geom = object:as_point()
            })
        end
    end
end

-- 3. Process Ways (Drivable Streets)
function osm2pgsql.process_way(object)
    local source_name = preferred_name(object.tags)
    if source_name and object.tags.highway then
        -- Filter for drivable roads (ignore paths, footways, etc.)
        local drivable = {
            motorway=true, trunk=true, primary=true, secondary=true,
            motorway_link=true, trunk_link=true, primary_link=true, secondary_link=true,
            tertiary_link=true, tertiary=true, unclassified=true, residential=true,
            living_street=true, service=true, road=true
        }

        local access = object.tags.access
        local motor_vehicle = object.tags.motor_vehicle
        local forbidden = access == 'private' or access == 'no'
            or motor_vehicle == 'private' or motor_vehicle == 'no'

        if drivable[object.tags.highway] and not forbidden then
            local geom = object:as_linestring()
            if geom then
                stage_streets:insert({
                    source_name = source_name,
                    highway = object.tags.highway,
                    access = access,
                    motor_vehicle = motor_vehicle,
                    geom = geom
                })
            end
        end
    end
end

-- 4. Process Relations (Administrative Boundaries)
function osm2pgsql.process_relation(object)
    local source_name = preferred_name(object.tags)
    if object.tags.boundary == 'administrative' and source_name then
        local al_str = object.tags.admin_level
        if not al_str then return end -- Safe handling of missing admin_level

        local al = tonumber(al_str)
        if not al then return end -- Safe handling of non-numeric tags

        -- Level 2: National Boundary (used for clipping)
        -- Level 4: County
        -- Level 6: UAT/Commune
        -- Level 8/9/10: Villages/Sectors
        if al == 2 or al == 4 or al == 6 or al == 8 or al == 9 or al == 10 then
            -- MUST use as_multipolygon() for Flex relations
            local geom = object:as_multipolygon()
            if geom then
                stage_admin:insert({
                    source_name = source_name,
                    admin_level = al,
                    geom = geom
                })
            end
        end
    end
end
