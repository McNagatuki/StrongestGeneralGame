scoreboard players add sgg_timer sgg_timer_tick 1
execute if score sgg_timer sgg_timer_tick matches 20.. run scoreboard players add sgg_timer sgg_timer_s 1
execute if score sgg_timer sgg_timer_s matches 60.. run scoreboard players add sgg_timer sgg_timer_m 1
execute if score sgg_timer sgg_timer_tick matches 20.. run scoreboard players set sgg_timer sgg_timer_tick 0
execute if score sgg_timer sgg_timer_s matches 60.. run scoreboard players set sgg_timer sgg_timer_s 0