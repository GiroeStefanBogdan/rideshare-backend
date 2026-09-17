# Separate Location Identity from OSM Identity

DrumBun locations have durable identities because rides reference them beyond any one OSM refresh. A location may
aggregate multiple replaceable OSM source records, such as a settlement boundary and place node; refreshes update
the association without replacing the DrumBun identity. This prevents routine upstream changes from invalidating
ride references while retaining OSM provenance.
