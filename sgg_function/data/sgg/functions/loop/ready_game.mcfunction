# n秒に1回、x人を選んでランダムtp処理
function sgg:timer/coutup

execute if score sgg_timer sgg_timer_s matches 5 if score sgg_timer sgg_timer_tick matches 0 run tag @a[tag=sgg_random_tp_waiting, limit=10] add sgg_random_tp
execute if score sgg_timer sgg_timer_s matches 15 if score sgg_timer sgg_timer_tick matches 0 run tag @a[tag=sgg_random_tp_waiting, limit=10] add sgg_random_tp
execute if score sgg_timer sgg_timer_s matches 25 if score sgg_timer sgg_timer_tick matches 0 run tag @a[tag=sgg_random_tp_waiting, limit=10] add sgg_random_tp
execute if score sgg_timer sgg_timer_s matches 35 if score sgg_timer sgg_timer_tick matches 0 run tag @a[tag=sgg_random_tp_waiting, limit=10] add sgg_random_tp
execute if score sgg_timer sgg_timer_s matches 45 if score sgg_timer sgg_timer_tick matches 0 run tag @a[tag=sgg_random_tp_waiting, limit=10] add sgg_random_tp
execute if score sgg_timer sgg_timer_s matches 55 if score sgg_timer sgg_timer_tick matches 0 run tag @a[tag=sgg_random_tp_waiting, limit=10] add sgg_random_tp
tag @a[tag=sgg_random_tp] remove sgg_random_tp_waiting
function sgg:map/random_tp