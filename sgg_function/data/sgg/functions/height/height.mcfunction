execute as @a[team=!ignore, gamemode=!creative, gamemode=!spectator] store result score @s y run data get entity @s Pos[1] 1

execute as @a[team=!ignore, gamemode=!creative, gamemode=!spectator] if score @s y >= #height height_warning_upper run effect give @s minecraft:blindness 2 0 true
execute as @a[team=!ignore, gamemode=!creative, gamemode=!spectator] if score @s y <= #height height_warning_lower run effect give @s minecraft:blindness 2 0 true

execute as @a[team=!ignore, gamemode=!creative, gamemode=!spectator] if score sgg_area sgg_area matches 4 if score @s y >= #height height_upper run effect give @s minecraft:blindness 2 0 true
execute as @a[team=!ignore, gamemode=!creative, gamemode=!spectator] if score sgg_area sgg_area matches 4 if score @s y <= #height height_lower run effect give @s minecraft:blindness 2 0 true

execute as @a[team=!ignore, gamemode=!creative, gamemode=!spectator] if score sgg_area sgg_area matches 5 if score @s y >= #height height_upper run effect give @s minecraft:wither 2 0 true
execute as @a[team=!ignore, gamemode=!creative, gamemode=!spectator] if score sgg_area sgg_area matches 5 if score @s y <= #height height_lower run effect give @s minecraft:wither 2 0 true