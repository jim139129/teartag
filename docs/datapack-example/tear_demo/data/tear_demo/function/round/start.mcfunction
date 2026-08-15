teartag enable @a[tag=tear_demo.active]
teartag reset round @a[tag=tear_demo.active]
tag @a[tag=tear_demo.active] remove tear_demo.eliminated
tag @a[tag=tear_demo.active] remove tear_demo.last_hit_through_wall
teartag rule set @a[tag=tear_demo.runner] tear_demo:hunter_vs_runner
teartag rule set @a[tag=tear_demo.hunter] tear_demo:deny
gamemode adventure @a[tag=tear_demo.active]
