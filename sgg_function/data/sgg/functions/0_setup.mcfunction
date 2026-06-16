# タイマー
function sgg:timer/setup

# マップ
function sgg:map/setup
function sgg:map/map_0/set

# ワールドボーダ
function sgg:area/setup

# 非参加チーム作成
team add ignore

# ゲームフェーズ用スコアボード
scoreboard objectives add sgg_phase dummy
scoreboard players set sgg_phase sgg_phase 0

# 高さ制限
function sgg:height/setup

# ゲームルール
gamerule commandBlockOutput false
gamerule keepInventory true
defaultgamemode adventure
