# Let Flyway Own the Permanent Schema

Flyway exclusively owns permanent database structures, including extensions, tables, columns, constraints, and
indexes. OSM initialization and refresh jobs may create isolated staging structures and reconcile imported data,
but may not evolve the application schema. This keeps every deployed schema reproducible and prevents operational
imports from silently changing it.
