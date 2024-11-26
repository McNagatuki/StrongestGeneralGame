# タイマー
function sgg:timer/setup

# ワールドボーダ
function sgg:area/setup

# 非参加チーム作成
team add ignore

# ゲームフェーズ用スコアボード
scoreboard objectives add sgg_phase dummy
scoreboard players set sgg_phase sgg_phase 0