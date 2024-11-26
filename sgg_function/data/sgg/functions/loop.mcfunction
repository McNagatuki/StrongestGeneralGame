# -- 試合中ずっと回る処理 --
# ゲーム終了判定
execute if score sgg_team_left sgg_team_left matches ..1 run function sgg:3_end_game

# タイマー
function sgg:timer/coutup
function sgg:timer/show/actionbar

# エリア
execute if score sgg_timer sgg_timer_m matches 30 if score sgg_timer sgg_timer_s matches 0 if score sgg_timer sgg_timer_tick matches 0 run function sgg:area/area1
execute if score sgg_timer sgg_timer_m matches 60 if score sgg_timer sgg_timer_s matches 0 if score sgg_timer sgg_timer_tick matches 0 run function sgg:area/area2
execute if score sgg_timer sgg_timer_m matches 80 if score sgg_timer sgg_timer_s matches 0 if score sgg_timer sgg_timer_tick matches 0 run function sgg:area/area3
execute if score sgg_timer sgg_timer_m matches 100 if score sgg_timer sgg_timer_s matches 0 if score sgg_timer sgg_timer_tick matches 0 run function sgg:area/area4
execute if score sgg_timer sgg_timer_m matches 110 if score sgg_timer sgg_timer_s matches 0 if score sgg_timer sgg_timer_tick matches 0 run function sgg:area/area5

# 落ち武者の処理
effect give @a[team=sgg_fallen_warrior] minecraft:weakness 2 255 true
effect give @a[team=sgg_fallen_warrior] minecraft:glowing 2 255 true
