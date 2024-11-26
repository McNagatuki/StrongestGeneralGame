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

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import java.util.*;
import java.util.logging.SimpleFormatter;

@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class TeamManager {
    // 落ち武者チーム
    private static final String fallenWarriorTeamName = "sgg_fallen_warrior";
    private static final String fallenWarriorTeamDisplayName = "落ち武者";
    private static final String teamLeftScore = "sgg_team_left";
    private static final String teamLeftScoreDisplayName = "残りチーム数";
    private static final String dummyPlayerScoreboardName = "sgg_team_left";

    private static final String teamListScore = "sgg_team_list";
    private static final String teamListScoreDisplayName = "チーム一覧";

    // ログ出力
    private static final Logger logger = Logger.getLogger(TeamManager.class.getName());

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
            playerTeam.setNameTagVisibility(Team.Visibility.HIDE_FOR_OTHER_TEAMS);
            scoreboard.addPlayerToTeam(playerName, playerTeam);

            playerTeam.setAllowFriendlyFire(false);
        }

        // 落ち武者team
        PlayerTeam fallenWarriorTeam = scoreboard.addPlayerTeam(fallenWarriorTeamName);
        fallenWarriorTeam.setDisplayName(Component.literal(fallenWarriorTeamDisplayName));
        fallenWarriorTeam.setNameTagVisibility(Team.Visibility.HIDE_FOR_OTHER_TEAMS);

        // 残りチーム数の記録用スコアボード
        ServerScoreboard serverScoreboard = server.getScoreboard();
        ObjectiveCriteria criteria = ObjectiveCriteria.DUMMY;
        ObjectiveCriteria.RenderType renderType = ObjectiveCriteria.RenderType.INTEGER;

        Component teamLeftScoreComponent = Component.literal(teamLeftScoreDisplayName);
        Objective teamLeftScoreObjective = serverScoreboard.addObjective(teamLeftScore, criteria, teamLeftScoreComponent, renderType);

        Component teamListScoreComponent = Component.literal(teamListScoreDisplayName);
        Objective teamListScoreObjective = serverScoreboard.addObjective(teamListScore, criteria, teamListScoreComponent, renderType);
        serverScoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, teamListScoreObjective);

        // 移動ログの出力
        try {
            // ファイルにログを出力するFileHandlerの設定
            if (logger.getHandlers().length == 0) {
                FileHandler fileHandler = new FileHandler("team_move_log.txt", true); // trueで追記モードに
                fileHandler.setFormatter(new SimpleFormatter()); // フォーマット設定（シンプルな形式）
                logger.addHandler(fileHandler); // Loggerにハンドラを追加
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Team manager was initialized.");
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
        Objective teamLeftScoreObjective = serverScoreboard.getObjective(teamLeftScore);
        if (teamLeftScoreObjective != null) {
            serverScoreboard.removeObjective(teamLeftScoreObjective);
        }

        Objective teamListScoreObjective = serverScoreboard.getObjective(teamListScore);
        if (teamListScoreObjective != null) {
            serverScoreboard.removeObjective(teamListScoreObjective);
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

        logger.info("add " + player.getName().getString() + " " + team.getName());

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

        logger.info("move " + fromTeam.getName() + " " + toTeam.getName());

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
    
    @SubscribeEvent
    public static void onServerTickEvent(ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        updateTeamLeft(server);
        updateTeamList(server);
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

    private static void updateTeamList(MinecraftServer server) {
        ServerScoreboard serverScoreboard = server.getScoreboard();
        Objective objective = serverScoreboard.getObjective(teamListScore);
        if (objective == null) {
            return;
        }

        List<PlayerTeam> playerTeamLeft = serverScoreboard.getPlayerTeams()
                .stream()
                .filter(e -> e.getName().startsWith("sgg_"))
                .filter(e -> !e.getName().equals(fallenWarriorTeamName))
                .toList();

        for (PlayerTeam playerTeam : playerTeamLeft) {
            String name = playerTeam.getName().replace("sgg_", "");
            int numberOfTeamMember = playerTeam.getPlayers().size();

            if (numberOfTeamMember > 0) {
                Score score = serverScoreboard.getOrCreatePlayerScore(name, objective);
                score.setScore(numberOfTeamMember);
            } else {
                serverScoreboard.resetPlayerScore(name, objective);
            }

        }
    }

//        MinecraftServer server = deathPlayer.getServer();
//        Scoreboard scoreboard = server.getScoreboard();
//        LOGGER.info("getPlayerTeamは以下のように記述");
//        LOGGER.info("SetTeamName: " + scoreboard.getPlayerTeam(deathPlayer.getTeam().getName()));
//        LOGGER.info("getPlayersTeamは以下のように記述");
//        LOGGER.info("SetPlayerName: " + scoreboard.getPlayersTeam(deathPlayer.getName().getString()));
}
