# MOD スタート
sgg start

# タイマー初期化
function sgg:timer/initialize

# ゲームモード変更
gamemode survival @a[team=!ignore]

# フェーズ
scoreboard players set sgg_phase sgg_phase 2

# 開始音
execute as @a at @s run playsound minecraft:block.anvil.place master @s ~ ~ ~