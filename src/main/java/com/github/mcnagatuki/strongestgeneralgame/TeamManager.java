package com.github.mcnagatuki.strongestgeneralgame;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class TeamManager {
    // 落ち武者チーム
    private static final String fallenWarriorTeamName = "sgg_fallen_warrior";
    private static final String fallenWarriorTeamDisplayName = "落ち武者";

    // 結果
    public enum TeamManagerResult { SUCCESS, FAILURE; }

    public static void initialize(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        // チームの削除
        List<String> sggTeamNames = scoreboard.getPlayerTeams()
                .stream()
                .map(PlayerTeam::getName)
                .filter(e -> e.startsWith("sgg_"))
                .toList();
        for (String sggTeamName : sggTeamNames) {
            PlayerTeam sggTeam = scoreboard.getPlayerTeam(sggTeamName);
            if (sggTeam != null) {
                scoreboard.removePlayerTeam(sggTeam);
            }
        }

        // チーム作成・追加
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player: players) {
            // プレイヤーがどこかのチームに入っていたら、パスする。
            Team playerJoinedTeam = player.getTeam();
            if (playerJoinedTeam != null) {
                continue;
            }

            String playerName = player.getName().getString();
            String playerTeamName = "sgg_" + playerName;

            PlayerTeam playerTeam = scoreboard.addPlayerTeam(playerTeamName);
            playerTeam.setDisplayName(Component.literal(playerName));
            scoreboard.addPlayerToTeam(playerName, playerTeam);

            playerTeam.setAllowFriendlyFire(false);
        }

        // 落ち武者team
        PlayerTeam fallenWarriorTeam = scoreboard.addPlayerTeam(fallenWarriorTeamName);
        fallenWarriorTeam.setDisplayName(Component.literal(fallenWarriorTeamDisplayName));
    }

    public static boolean isPlayerInGame(@NotNull Player player) {
        Team team = player.getTeam();
        if (team == null) {
            return false;
        }

        String teamName = team.getName();
        return teamName.startsWith("sgg_");
    }

    public static boolean isPlayerGeneral(@NotNull Player player) {
        Team team = player.getTeam();
        if (team == null) {
            return false;
        }

        String playerName = player.getName().getString();
        String teamName = team.getName();
        return teamName.endsWith(playerName);
    }

    public static boolean isFallenWarrior(@NotNull Player player) {
        Team team = player.getTeam();
        if (team == null) {
            return false;
        }

        String teamName = team.getName();
        return teamName.equals(fallenWarriorTeamName);
    }

    public static TeamManagerResult addPlayerToTeam(Player player, Team team) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return TeamManagerResult.FAILURE;
        }

        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam playerTeam = scoreboard.getPlayerTeam(team.getName());
        scoreboard.addPlayerToTeam(player.getName().getString(), playerTeam);

        return TeamManagerResult.SUCCESS;
    }

    public static TeamManagerResult moveTeamToTeam(MinecraftServer server, Team fromTeam, Team toTeam) {
        Scoreboard scoreboard = server.getScoreboard();

        // get team name
        String toTeamName = toTeam.getName();

        // get player team
        PlayerTeam toPlayerTeam = scoreboard.getPlayerTeam(toTeamName);

        List<String> fromTeamPlayerNames = fromTeam.getPlayers().stream().toList();
        for (String fromTeamPlayerName : fromTeamPlayerNames){
            scoreboard.addPlayerToTeam(fromTeamPlayerName, toPlayerTeam);
        }

        return TeamManagerResult.SUCCESS;
    }

    public static TeamManagerResult addPlayerToFallenWarrior(Player player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return TeamManagerResult.FAILURE;
        }

        return addPlayerToTeam(player, getFallenWarriorPlayerTeam(server));
    }

    public static TeamManagerResult moveTeamToFallenWarrior(MinecraftServer server, Team team) {
        Team fallenWarriorTeam = getFallenWarriorPlayerTeam(server);
        if (fallenWarriorTeam == null) {
            return TeamManagerResult.FAILURE;
        }

        return moveTeamToTeam(server, team, fallenWarriorTeam);
    }

    public static PlayerTeam getFallenWarriorPlayerTeam(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        return scoreboard.getPlayerTeam(fallenWarriorTeamName);
    }

    // 死亡時にチーム名を操作し、どのチームがどうなったかをたどれるようにする
    // これにより、「ゲーム終了時にどう攻略されたか知る」と「途中鯖から抜けていた人が再ログイン時に処理をうける」ことができる
    // 「途中鯖から抜けていた人が再ログイン時に処理をうける」
    // ＝ 現所属チームにリーダーがいるか確認、いなければ履歴をたどって別のチームへ移動
    // ＝ これを繰り返して、チームに**スペクテイターとして**復帰する
    // TODO: 「ゲーム終了時にどう攻略されたか知る」
//    @SubscribeEvent
//    public static void onPlayerLoggedInEvent(PlayerLoggedInEvent event) {
//        // ゲーム中でなければ何もしない
//        if (!MainLogic.isRunning()) {
//            return;
//        }
//
//        // 入ったプレイヤーを取得
//        Player player = event.getEntity();
//        String playerName = player.getName().getString();
//
//        // Serverのみで動作
//        MinecraftServer server = player.getServer();
//        if (server == null) {
//            return;
//        }
//
//        if (!(player instanceof ServerPlayer serverPlayer)) {
//            return;
//        }
//
//        Scoreboard scoreboard = server.getScoreboard();
//        PlayerTeam playerTeam = scoreboard.getPlayersTeam(player.getName().getString());
//        if (playerTeam == null) {
//            return;
//        }
//
//        String suffix = playerTeam.getPlayerSuffix().getString();
//        player.sendSystemMessage(Component.literal("suffix: " + suffix));  // TODO: DEBUG
//        if (!suffix.equals("")) {
//            serverPlayer.setGameMode(GameType.SPECTATOR);
//        }
//
//        while (!suffix.equals("")) {
//            PlayerTeam nextPlayerTeam = scoreboard.getPlayerTeam(suffix);
//            scoreboard.addPlayerToTeam(playerName, nextPlayerTeam);
//            suffix = nextPlayerTeam.getPlayerSuffix().getString();
//        }
//    }

//        MinecraftServer server = deathPlayer.getServer();
//        Scoreboard scoreboard = server.getScoreboard();
//        LOGGER.info("getPlayerTeamは以下のように記述");
//        LOGGER.info("SetTeamName: " + scoreboard.getPlayerTeam(deathPlayer.getTeam().getName()));
//        LOGGER.info("getPlayersTeamは以下のように記述");
//        LOGGER.info("SetPlayerName: " + scoreboard.getPlayersTeam(deathPlayer.getName().getString()));
}
