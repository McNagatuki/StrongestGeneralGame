# -- ゲーム正常終了 --
# MOD終了
sgg stop

# アナウンス
title @a title "ゲーム終了！"
playsound minecraft:block.anvil.use player @a ~ ~ ~ 1 1

# フェーズ
scoreboard players set sgg_phase sgg_phase 3

# コマンド実行ログ表示
gamerule sendCommandFeedback true
