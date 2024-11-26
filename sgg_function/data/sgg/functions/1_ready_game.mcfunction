# コマンド実行ログ非表示
gamerule sendCommandFeedback false

# タイマー初期化
function sgg:timer/initialize

# ワールドボーダー初期化
function sgg:area/area0

# アイテム配布処理
clear @a[team=!ignore]
give @a[team=!ignore] minecraft:cooked_beef 64
give @a[team=!ignore] minecraft:wooden_pickaxe

# MOD 初期化
sgg initialize

# フェーズ
scoreboard players set sgg_phase sgg_phase 1
