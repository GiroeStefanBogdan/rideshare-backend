-- import.lua

-- 1. Define Staging Tables
local stage_admin = osm2pgsql.define_table({
    name = 'stage_admin',
    ids = { type = 'any', type_column = 'osm_type', id_column = 'osm_id' },
    columns = {
        { column = 'name', type = 'text' },
        { column = 'admin_level', type = 'int4' },
        { column = 'geom', type = 'geometry', projection = 4326 }
    }
})

local stage_places = osm2pgsql.define_table({
    name = 'stage_places',
    ids = { type = 'node', id_column = 'osm_id' },
    columns = {
        { column = 'name', type = 'text' },
        { column = 'place', type = 'text' },
        { column = 'population', type = 'int4' },
        { column = 'geom', type = 'geometry', projection = 4326 }
    }
})

local stage_streets = osm2pgsql.define_table({
    name = 'stage_streets',
    ids = { type = 'way', id_column = 'osm_id' },
    columns = {
        { column = 'name', type = 'text' },
        { column = 'highway', type = 'text' },
        { column = 'geom', type = 'linestring', projection = 4326 }
    }
})

-- 2. Process Nodes (Villages, Towns, Localities without boundary relations)
function osm2pgsql.process_node(object)
    if object.tags.name and object.tags.place then
        -- Expanded list to catch all relevant populated areas, neighborhoods, and isolated dwellings
        local valid_places = {
            city=true, town=true, village=true, hamlet=true,
            suburb=true, locality=true, isolated_dwelling=true,
            neighbourhood=true, quarter=true
        }

        if valid_places[object.tags.place] then
            local pop = tonumber(object.tags.population)
            stage_places:insert({
                name = object.tags.name,
                place = object.tags.place,
                population = pop,
                geom = object:as_point()
            })
        end
    end
end

-- 3. Process Ways (Drivable Streets)
function osm2pgsql.process_way(object)
    if object.tags.name and object.tags.highway then
        -- Filter for drivable roads (ignore paths, footways, etc.)
        local drivable = {
            motorway=true, trunk=true, primary=true, secondary=true,
            tertiary=true, unclassified=true, residential=true, living_street=true
        }

        if drivable[object.tags.highway] then
            local geom = object:as_linestring()
            if geom then
                stage_streets:insert({
                    name = object.tags.name,
                    highway = object.tags.highway,
                    geom = geom
                })
            end
        end
    end
end

-- 4. Process Relations (Administrative Boundaries)
function osm2pgsql.process_relation(object)
    if object.tags.boundary == 'administrative' and object.tags.name then
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
                    name = object.tags.name,
                    admin_level = al,
                    geom = geom
                })
            end
        end
    end
end