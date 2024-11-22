package com.github.mcnagatuki.strongestgeneralgame;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class TeleportManager {
    public static boolean toTeamMember(ServerPlayer serverPlayer) {
        // サーバーの取得
        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return false;
        }

        // チームの名前を取得
        Team team = serverPlayer.getTeam();
        if (team == null) {
            return false;
        }

        // チームの中からリスポーンできる人を取得
        List<Player> newTeamMembers = getTeamPlayers(server, team);
        List<Player> tpCandidatePlayers = getTpAblePlayers(newTeamMembers);
        Player tpDistPlayer = getRandomElement(tpCandidatePlayers);
        if (tpDistPlayer == null) {
            return false;
        }

        // tp
        ServerPlayer tpDistServerPlayer = (ServerPlayer) tpDistPlayer;
        Vec3 pos = tpDistServerPlayer.position();
        serverPlayer.teleportTo(pos.x, pos.y, pos.z);

        return true;
    }

    public static boolean toDeathPoint(ServerPlayer serverPlayer) {
        // 死んだ場所を取得
        Optional<GlobalPos> deathLocation = serverPlayer.getLastDeathLocation();
        if (deathLocation.isEmpty()) {
            return false;
        }

        // tp
        Vec3 pos = deathLocation.get().pos().getCenter();
        serverPlayer.teleportTo(pos.x, pos.y, pos.z);

        return true;
    }

    private static List<Player> getTeamPlayers(MinecraftServer server, Team team) {
        return server.getPlayerList().getPlayers().stream()
                .filter(e -> e.getTeam() != null)
                .filter(e -> team.equals(e.getTeam()))
                .collect(Collectors.toList());
    }

    private static List<Player> getTpAblePlayers(List<Player> players) {
        // 生きている && ゲームモードサバイバル
        return players.stream()
                .filter(e -> e instanceof ServerPlayer)
                .map(e -> (ServerPlayer) e)
                .filter(LivingEntity::isAlive)
                .filter(e -> !(e.isCreative() || e.isSpectator()))
                .map(e -> (Player) e)
                .toList();
    }

    private static <T> T getRandomElement(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }

        Random random = new Random();
        int randomIndex = random.nextInt(list.size()); // 0 から list.size() - 1 の範囲でランダムなインデックスを生成
        return list.get(randomIndex); // ランダムなインデックスの要素を返す
    }
}
