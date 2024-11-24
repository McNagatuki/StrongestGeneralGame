package com.github.mcnagatuki.strongestgeneralgame;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.event.TickEvent.ServerTickEvent;

import java.util.*;

@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class TeamManager {
    // 落ち武者チーム
    private static final String fallenWarriorTeamName = "sgg_fallen_warrior";
    private static final String fallenWarriorTeamDisplayName = "落ち武者";
    private static final String teamLeftScore = "sgg_team_left";
    private static final String dummyPlayerScoreboardName = "sgg_team_left";

    // 結果
    public enum TeamManagerResult { SUCCESS, FAILURE; }

    public static void initialize(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        // チームの削除・スコアボードの削除
        destroy(server);

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

        // 残りチーム数の記録用スコアボード
        ServerScoreboard serverScoreboard = server.getScoreboard();
        ObjectiveCriteria criteria = ObjectiveCriteria.DUMMY;
        Component displayName = Component.literal("残りチーム数");
        ObjectiveCriteria.RenderType renderType = ObjectiveCriteria.RenderType.INTEGER;
        Objective objective = serverScoreboard.addObjective(teamLeftScore, criteria, displayName, renderType);
        serverScoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
    }

    public static void destroy(MinecraftServer server) {
        ServerScoreboard serverScoreboard = server.getScoreboard();

        // チームの削除
        List<String> sggTeamNames = serverScoreboard.getPlayerTeams()
                .stream()
                .map(PlayerTeam::getName)
                .filter(e -> e.startsWith("sgg_"))
                .toList();
        for (String sggTeamName : sggTeamNames) {
            PlayerTeam sggTeam = serverScoreboard.getPlayerTeam(sggTeamName);
            if (sggTeam != null) {
                serverScoreboard.removePlayerTeam(sggTeam);
            }
        }

        // objectiveを削除
        Objective objective = serverScoreboard.getObjective(teamLeftScore);
        if (objective != null) {
            serverScoreboard.removeObjective(objective);
        }
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

    // TODO: 「ゲーム終了時にどう攻略されたか知る」
    @SubscribeEvent
    public static void onPlayerLoggedInEvent(ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        updateTeamLeft(server);
    }

    private static void updateTeamLeft(MinecraftServer server) {
        int numberOfTeamLeft = getNumberOfTeamLeft(server);

        ServerScoreboard serverScoreboard = server.getScoreboard();
        Objective objective = serverScoreboard.getObjective(teamLeftScore);
        if (objective == null) {
            return;
        }

        Score score = serverScoreboard.getOrCreatePlayerScore(dummyPlayerScoreboardName, objective);
        score.setScore(numberOfTeamLeft);
    }

    private static int getNumberOfTeamLeft(MinecraftServer server) {
        ServerScoreboard serverScoreboard = server.getScoreboard();
         long numberOfTeamLeft = serverScoreboard.getPlayerTeams()
                .stream()
                .filter(e -> e.getName().startsWith("sgg_"))
                .filter(e -> !e.getName().equals(fallenWarriorTeamName))
                .filter(e -> e.getPlayers().size() > 0)
                .count();

         try {
             return Math.toIntExact(numberOfTeamLeft);
         } catch (Exception e) {
             return Integer.MAX_VALUE;
         }
    }

//        MinecraftServer server = deathPlayer.getServer();
//        Scoreboard scoreboard = server.getScoreboard();
//        LOGGER.info("getPlayerTeamは以下のように記述");
//        LOGGER.info("SetTeamName: " + scoreboard.getPlayerTeam(deathPlayer.getTeam().getName()));
//        LOGGER.info("getPlayersTeamは以下のように記述");
//        LOGGER.info("SetPlayerName: " + scoreboard.getPlayersTeam(deathPlayer.getName().getString()));
}
