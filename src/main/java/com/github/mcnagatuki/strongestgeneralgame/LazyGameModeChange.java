package com.github.mcnagatuki.strongestgeneralgame;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class LazyGameModeChange {
    private static String lazyCounter = "sgg_lazy_game_mode_change";

    private static Map<String, Long> lastTickEvent = new HashMap<>();

    public static void initialize(MinecraftServer server) {
        ServerScoreboard serverScoreboard = server.getScoreboard();

        // 既存のobjectiveを削除
        Objective objective = serverScoreboard.getObjective(lazyCounter);
        if (objective != null) {
            serverScoreboard.removeObjective(objective);
        }

        // 改めてobjectiveを作成
        ObjectiveCriteria criteria = ObjectiveCriteria.DUMMY;
        Component displayName = Component.literal("SGG_LAZY_GAME_MODE_CHANGE");
        ObjectiveCriteria.RenderType renderType = ObjectiveCriteria.RenderType.INTEGER;
        serverScoreboard.addObjective(lazyCounter, criteria, displayName, renderType);
    }

    public static void register(ServerPlayer serverPlayer, int counterSecond) {
        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }

        ServerScoreboard serverScoreboard = server.getScoreboard();
        Objective objective = serverScoreboard.getObjective(lazyCounter);
        if (objective == null) {
            return;
        }

        Score score = serverScoreboard.getOrCreatePlayerScore(serverPlayer.getScoreboardName(), objective);
        score.setScore(counterSecond);
    }

    @SubscribeEvent
    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        // startのみ動作
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        // サーバーのみで動作
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // カウント開始していなければここで開始
        long currentNanoTime = System.nanoTime();
        String playerName = serverPlayer.getName().getString();
        if (!lastTickEvent.containsKey(playerName)) {
            lastTickEvent.put(playerName, currentNanoTime);
        }

        // 1秒毎の動作
        double durationSec = (currentNanoTime - lastTickEvent.get(playerName)) / 1e9;
        if (durationSec < 1) {
            return;  // 1秒未満は無視
        }

        lastTickEvent.put(playerName, currentNanoTime);  // 更新

        // check if game is running
        if (!MainLogic.isRunning()) {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }

        // スコアの取得 = カウント時間の取得
        ServerScoreboard serverScoreboard = server.getScoreboard();
        Objective objective = serverScoreboard.getObjective(lazyCounter);
        if (objective == null) {
            return;
        }

        Score score = serverScoreboard.getOrCreatePlayerScore(serverPlayer.getScoreboardName(), objective);
        int counterTime = score.getScore();
        if (counterTime < 0) {
            return;
        }

        if (counterTime > 0) {
            String text = "リスポーンまで、" + String.valueOf(counterTime) + "秒";
            serverPlayer.sendSystemMessage(Component.literal(text));
        } else {
            serverPlayer.setGameMode(GameType.SURVIVAL);
        }

        // update counter
        score.setScore(counterTime - 1);
    }

}