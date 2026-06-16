# タイマー初期化
function sgg:timer/initialize

# ワールドボーダー初期化
function sgg:area/area0

# アイテム配布処理
clear @a[team=!ignore]
give @a[team=!ignore] minecraft:cooked_beef 64
give @a[team=!ignore] minecraft:oak_planks 32 

# MOD 初期化
sgg initialize

# ゲームモード変更
gamemode adventure @a[team=!ignore]

# random tp
tag @a[team=!ignore] add sgg_random_tp_waiting

# フェーズ
scoreboard players set sgg_phase sgg_phase 1
