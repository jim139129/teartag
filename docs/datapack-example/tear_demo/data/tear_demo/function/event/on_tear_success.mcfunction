# Event storage is temporary, so copy it before using scheduled functions.
data modify storage tear_demo:last_event current set from storage teartag:event {}
execute if data storage teartag:event {wall_attempt:1b} run tag @s add tear_demo.last_hit_through_wall
execute unless data storage teartag:event {wall_attempt:1b} run tag @s remove tear_demo.last_hit_through_wall
