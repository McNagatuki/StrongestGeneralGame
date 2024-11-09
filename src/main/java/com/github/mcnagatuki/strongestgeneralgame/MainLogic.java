package com.github.mcnagatuki.strongestgeneralgame;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class MainLogic {
    private static boolean isRunning = false;
    private static boolean isReady = false;

    private static String ignoreTeamName = "test";

    public MainLogic() {
    }

    public static void setReady(CommandSourceStack source) {
        if (isRunning) {
            source.sendFailure(Component.literal("[SGG] Game is still running."));
            return;
        }

        MinecraftServer server = source.getServer();
        if (server == null) {
            source.sendFailure(Component.literal("[SGG] Server is null."));
            return;
        }

        // チームの削除
        Scoreboard scoreboard = server.getScoreboard();
        List<String> sggTeamNames = scoreboard.getPlayerTeams()
                .stream()
                .map(e -> e.getName())
                .filter(e -> e.startsWith("sgg_"))
                .collect(Collectors.toList());
        for (String sggTeamName : sggTeamNames) {
            PlayerTeam sggTeam = scoreboard.getPlayerTeam(sggTeamName);
            if (sggTeam != null) {
                scoreboard.removePlayerTeam(sggTeam);
            }
        }

        // チーム作成・追加
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player: players) {
            // プレイヤーがignoreの対象となるチームに入っていたら、パスする。
            Team playerJoinedTeam = player.getTeam();
            if (playerJoinedTeam != null && playerJoinedTeam.getName().equalsIgnoreCase(ignoreTeamName)) {
                continue;
            }

            String playerName = player.getName().getString();
            String playerTeamName = "sgg_" + playerName;

            PlayerTeam playerTeam = scoreboard.addPlayerTeam(playerTeamName);
            playerTeam.setDisplayName(Component.literal(playerName));
            scoreboard.addPlayerToTeam(playerName, playerTeam);
        }

        // 浪人team
        String roninTeamName = "sgg_ronin";
        String roninTeamDisplayName = "浪人";
        PlayerTeam roninTeam = scoreboard.addPlayerTeam(roninTeamName);
        roninTeam.setDisplayName(Component.literal(roninTeamDisplayName));

        // 準備完了
        isReady = true;
        Supplier<Component> supplier = () -> Component.literal("[SGG] Game is ready.");
        source.sendSuccess(supplier, true);
    }

    public static void startGame(CommandSourceStack source) {
        if (!isReady) {
            source.sendFailure(Component.literal("[SGG] Game is NOT ready."));
            return;
        }

        isRunning = true;
        Supplier<Component> supplier = () -> Component.literal("[SGG] Game is started!");
        source.sendSuccess(supplier, true);
    }

    public static void endGame(CommandSourceStack source) {
        if (!isRunning) {
            source.sendFailure(Component.literal("[SGG] Game is NOT started."));
            return;
        }

        isRunning = false;
        isReady = false;

        /* TODO: 処理実装 */

        Supplier<Component> supplier = () -> Component.literal("[SGG] Game is stopped!");
        source.sendSuccess(supplier, true);
    }

    public static void setIgnoreTeam(String teamName) {
        ignoreTeamName = teamName;
    }
    public static void delIgnoreTeam() {
        ignoreTeamName = "";
    }

    public static String getIgnoreTeam() {
        return ignoreTeamName;
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // check if game is running
        if (!isRunning) {
            return;
        }

        // check if player death
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player deathPlayer = (Player) event.getEntity();

        Team deathPlayerTeam = deathPlayer.getTeam();
        // プレイヤーがチームに入っていなかったら無視する。
        if (deathPlayerTeam == null) {
            return;
        }

        // プレイヤーがignoreの対象となるチームに入っていたら無視する。
        if (deathPlayerTeam.getName().equalsIgnoreCase(ignoreTeamName)){
            return;
        }

        // check player kill
        DamageSource source = event.getSource();
        boolean isPlayerKill = source.getEntity() instanceof Player;

        if (isPlayerKill) {
            Player killPlayer = (Player) source.getEntity();
            playerKill(killPlayer, deathPlayer);
        }
        else {
            playerKillOneself(deathPlayer);
        }
    }


    private static void playerKill(Player killPlayer, Player deathPlayer) {
        System.out.println(killPlayer.getName().getString() + " killed!");
        // TODO: 実装
    }

    private static void playerKillOneself(Player deathPlayer){
        // TODO: 実装
    }



}
