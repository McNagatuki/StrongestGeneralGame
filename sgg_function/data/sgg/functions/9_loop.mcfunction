execute if score sgg_phase sgg_phase matches 1 run function sgg:loop/ready_game
execute if score sgg_phase sgg_phase matches 2 run function sgg:loop/in_game
execute unless score sgg_phase sgg_phase matches 2 run function sgg:loop/out_game