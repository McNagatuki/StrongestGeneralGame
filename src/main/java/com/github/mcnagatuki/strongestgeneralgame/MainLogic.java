package com.github.mcnagatuki.strongestgeneralgame;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class MainLogic {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Supplier<ExecutorService> executorService = Executors::newSingleThreadScheduledExecutor;
    private static boolean running = false;

    // ワールド内コマンド操作用tag, このtagを通じて、コマンドの対象操作であることを伝達する。
    private static final String commandRandomTpTag = "sgg_random_tp";  // TODO: コマンド側で実装。
    private static final String commandGeneralDeathAnnounceTag = "sgg_general_death_announce";  // TODO: コマンド側で実装

    // リスポーン用タグ
    private static final String respawnTpTagPrefix = "sgg_respawn_tp_";
    private static final String respawnDeathPoint = respawnTpTagPrefix + "death_point";
    private static final String respawnTeamMember = respawnTpTagPrefix + "team_member";
    private static final String respawnRandomTp = respawnTpTagPrefix + "random";

    public MainLogic() {
    }

    public static void initializeGame(CommandSourceStack source) {
        if (running) {
            source.sendFailure(Component.literal("[SGG] Game is still running."));
            return;
        }

        // チームの初期化
        MinecraftServer server = source.getServer();
        TeamManager.initialize(server);

        // 過去のダメージ記録を削除
        DamageTracker.clear();

        // 準備完了
        Supplier<Component> supplier = () -> Component.literal("[SGG] Game is initialized.");
        source.sendSuccess(supplier, true);
    }

    public static void startGame(CommandSourceStack source) {
        running = true;
        Supplier<Component> supplier = () -> Component.literal("[SGG] Game is started!");
        source.sendSuccess(supplier, true);
    }

    public static void endGame(CommandSourceStack source) {
        if (!running) {
            source.sendFailure(Component.literal("[SGG] Game is NOT started."));
            return;
        }
        running = false;

        Supplier<Component> supplier = () -> Component.literal("[SGG] Game is stopped!");
        source.sendSuccess(supplier, true);
    }

    public static boolean isRunning() {
        return running;
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // check if game is running
        if (!running) {
            return;
        }

        // check if player death
        if (!(event.getEntity() instanceof Player deathPlayer)) {
            return;
        }

        if (!TeamManager.isPlayerInGame(deathPlayer)) {
            return;  // この時点で、プレイヤーはsgg_XXXのチームに入っていることが確定する。
        }

        // SGGプレイヤーの死亡時処理
        if (TeamManager.isPlayerGeneral(deathPlayer)) {
            // 死んだのが大将だった場合
            executeDeathGeneral(deathPlayer);
        } else if (TeamManager.isFallenWarrior(deathPlayer)) {
            // 死んだのが落ち武者だった場合
            executeDeathFallenWarrior(deathPlayer);
        } else {
            // 死んだのが一般プレイヤーだった場合
            executeDeathCommonSoldier(deathPlayer);
        }

        // TODO: 相打ちの対処、自死（killerが自分になるケース）の対処
    }

    private static void executeDeathGeneral(@NotNull Player deathPlayer) {
        // Serverのみで動作
        MinecraftServer server = deathPlayer.getServer();
        if (server == null) {
            return;
        }

        // 死んだプレイヤーチーム・殺したプレイヤーチームの取得
        Team deathPlayerTeam = deathPlayer.getTeam();
        Optional<Team> killerTeam = getWhichTeamKillMe(deathPlayer);

        // 死んだチームのメンバーを列挙
        List<Player> deathPlayerTeamMembers = getTeamPlayers(server, deathPlayerTeam);

        // 「大将が死にました」のアナウンス, MODではタグをつけるだけ
        deathPlayerTeamMembers.forEach(e -> e.addTag(commandGeneralDeathAnnounceTag));

        // 死因がSGGプレイヤーによるものではない場合
        // あるいは、殺したチームが死んだチームと同じ場合（自殺？や相打ち）
        if (killerTeam.isEmpty() || killerTeam.get().getName().equals(deathPlayerTeam.getName())) {
            // チームの全プレイヤーを落ち武者化
            TeamManager.moveTeamToFallenWarrior(server, deathPlayerTeam);

            // 死んでいる人はランダムtp, tagで操作, 続きはonPlayerRespawn
            deathPlayerTeamMembers
                    .stream()
                    .filter(e -> !e.isAlive())
                    .forEach(e -> setRespawnTpTag(e, respawnRandomTp));
            return;
        }

        // 一度全員スペクテイター化
        deathPlayerTeamMembers.stream()
                .filter(e -> e instanceof ServerPlayer)
                .map(e -> (ServerPlayer) e)
                .forEach(e -> e.setGameMode(GameType.SPECTATOR));

        // 大将を殺したプレイヤーのチームにチーム変更
        TeamManager.moveTeamToTeam(server, deathPlayerTeam, killerTeam.get());

        // *** 以下の処理は、新チーム全員に適用 ***
        getTeamPlayers(server, killerTeam.get())
                .stream()
                .filter(e -> !e.isAlive())
                .forEach(e -> setRespawnTpTag(e, respawnTeamMember));
    }

    private static void executeDeathFallenWarrior(@NotNull Player deathPlayer) {
        // Serverのみで動作
        MinecraftServer server = deathPlayer.getServer();
        if (server == null) {
            return;
        }

        // ServerPlayerのみで動作
        if (!(deathPlayer instanceof ServerPlayer deathServerPlayer)) {
            return;
        }

        // 殺したプレイヤーチームの取得
        Optional<Team> killerTeam = getWhichTeamKillMe(deathPlayer);
        // 死因がSGGプレイヤーによるものではない場合、または殺したプレイヤーが鯖にいない場合
        if (killerTeam.isEmpty()) {
            // ランダムtp, tagで操作, 続きはonPlayerRespawn
            setRespawnTpTag(deathPlayer, respawnRandomTp);
            return;
        }

        /* 以下、死因がSGGプレイヤーによる場合*/
        // プレイヤーをスペクテイターにする
        deathServerPlayer.setGameMode(GameType.SPECTATOR);

        // プレイヤーのチーム移動
        TeamManager.addPlayerToTeam(deathPlayer, killerTeam.get());

        // 新チームのもとにリスポーン, tagで操作, 続きはonPlayerRespawn
        setRespawnTpTag(deathPlayer, respawnTeamMember);
    }

    private static void executeDeathCommonSoldier(@NotNull Player deathPlayer) {
        // ServerPlayerのみで動作。すなわち、このMODはクライアントでは動かないはず。
        if (!(deathPlayer instanceof ServerPlayer deathServerPlayer)) {
            return;
        }

        // プレイヤーをスペクテイターにする
        deathServerPlayer.setGameMode(GameType.SPECTATOR);

        // 死んだ場所にリスポーン, tagで操作, 続きはonPlayerRespawn
        setRespawnTpTag(deathPlayer, respawnDeathPoint);
    }

    private static Optional<Team> getWhichTeamKillMe(Player deathPlayer) {
        // およそ15秒以内にentityから攻撃されていた場合、死亡ログにentityの情報が表示されるみたい。
        // ただし、getLastHurtByMobやgetKillCreditからは5秒ほどしか情報を抜き出せない。
        // なので、自分で直近誰が攻撃したかを取得する。
        double durationSecond = 15;

        Optional<String> killerPlayerName = DamageTracker.getLatestHurtByPlayerName(deathPlayer, durationSecond);
        if (killerPlayerName.isEmpty()) {
            return Optional.empty();
        }

        MinecraftServer server = deathPlayer.getServer();
        if (server == null) {
            return Optional.empty();
        }

        Player killerPlayer = deathPlayer.getServer().getPlayerList().getPlayerByName(killerPlayerName.get());
        if (killerPlayer == null) {
            return Optional.empty();  // もし殺したやつがサーバーにいなかったら、落ち武者化する。
        }

        if (!TeamManager.isPlayerInGame(killerPlayer)) {
            return Optional.empty();
        }

        // killerPlayerのチームを取得して返す
        return Optional.ofNullable(killerPlayer.getTeam());
    }

    public static List<Player> getTeamPlayers(MinecraftServer server, Team team) {
        return server.getPlayerList().getPlayers().stream()
                .filter(e -> e.getTeam() != null)
                .filter(e -> team.equals(e.getTeam()))
                .collect(Collectors.toList());
    }

    public static List<Player> getTpAblePlayers(List<Player> players) {
        // 生きている && ゲームモードサバイバル
        return players.stream()
                .filter(e -> e instanceof ServerPlayer)
                .map(e -> (ServerPlayer) e)
                .filter(LivingEntity::isAlive)
                .filter(e -> !(e.isCreative() || e.isSpectator()))
                .map(e -> (Player) e)
                .toList();
    }

    public static <T> T getRandomElement(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }

        Random random = new Random();
        int randomIndex = random.nextInt(list.size()); // 0 から list.size() - 1 の範囲でランダムなインデックスを生成
        return list.get(randomIndex); // ランダムなインデックスの要素を返す
    }

    public static void setRespawnTpTag(Player player, String tagString) {
        // 他のリスポーンタグをすべて削除
        List<String> tags = player.getTags().stream().filter(e -> e.startsWith(respawnTpTagPrefix)).toList();
        for (String tag : tags) {
            player.removeTag(tag);
        }

        // コマンドランダムtp用タグも削除
        player.removeTag(commandRandomTpTag);

        // タグの追加
        player.addTag(tagString);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        // check if game is running
        if (!running) {
            return;
        }

        // サーバーのみで動作
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }

        if (!serverPlayer.isAlive()) {
            return;
        }

        // リスポーンタグを取得
        Optional<String> respawnTag = serverPlayer.getTags().stream().filter(e -> e.startsWith(respawnTpTagPrefix)).findFirst();
        if (respawnTag.isEmpty()) {
            return;
        }

        // 他のリスポーンタグをすべて削除
        List<String> respawnTags = serverPlayer.getTags().stream().filter(e -> e.startsWith(respawnTpTagPrefix)).toList();
        respawnTags.forEach(serverPlayer::removeTag);

        if (respawnTag.get().equals(respawnTeamMember)) {
            // 新チームの名前を取得
            Team team = serverPlayer.getTeam();
            if (team == null) {
                return;
            }

            // チームの中からリスポーンできる人を取得
            List<Player> newTeamMembers = getTeamPlayers(server, team);
            List<Player> tpCandidatePlayers = getTpAblePlayers(newTeamMembers);
            Player tpDistPlayer = getRandomElement(tpCandidatePlayers);
            if (tpDistPlayer == null) {
                return;
            }

            // リス後にtp
            ServerPlayer tpDistServerPlayer = (ServerPlayer) tpDistPlayer;
            Vec3 pos = tpDistServerPlayer.position();
            serverPlayer.teleportTo(pos.x, pos.y, pos.z);

        } else if (respawnTag.get().equals(respawnDeathPoint)) {
            // 死んだ場所を取得
            Optional<GlobalPos> deathLocation = serverPlayer.getLastDeathLocation();
            if (deathLocation.isEmpty()) {
                return;
            }

            // リス後にtp
            Vec3 pos = deathLocation.get().pos().getCenter();
            serverPlayer.teleportTo(pos.x, pos.y, pos.z);

        } else if (respawnTag.get().equals(respawnRandomTp)){
            // ランダムtp, コマンドで実装
            serverPlayer.addTag(commandRandomTpTag);

        } else {
            // Do nothing
            return;
        }

        // TODO: ゲームモード変更用のリスポーン設定
    }


    //    public static void gameRespawnPlayersAroundTeamMember(List<Player> players, Team targetTeam) {
//        // たぶんカウントダウン処理が重くなるので、リスポーン処理はまとめてやった方がいい。
//
//        if (players.isEmpty()){
//            return;
//        }
//
//        // ServerPlayerのみで動作。すなわち、このMODはクライアントでは動かないはず。
//        if (players.stream().anyMatch(e -> !(e instanceof ServerPlayer))){
//            return;
//        }
//
//        List<ServerPlayer> serverPlayers = players.stream().map(e -> (ServerPlayer) e).toList();
//        serverPlayers.forEach(e -> e.setGameMode(GameType.SPECTATOR));  // スペクテイターに変更
//        serverPlayers.stream().filter(LivingEntity::isAlive).forEach(Player::respawn);  // 死んでいたら強制復活
//
//        // tp先候補のプレイヤーを一覧で取得
//        MinecraftServer server = serverPlayers.get(0).getServer();
//        List<Player> teamPlayers = getTeamPlayers(server, targetTeam);
//
//        // tp
//        for (ServerPlayer serverPlayer : serverPlayers) {
//            Player tpDistPlayer = getRandomElement(teamPlayers);
//            BlockPos tpDist = tpDistPlayer.getOnPos();
//            serverPlayer.setPos(tpDist.getCenter());
//        }
//
//        // TODO: カウントダウン処理中に大将がやられてしまった時の対処
//
//        // 5秒待機, カウントダウン付き
//        int countDownTimeSec = 5;
//        executorService.get().
//                3wscheduleAtFixedRate(() -> {
//            for (int i = countDownTimeSec; i > 0; i--) {
//                // チャットにカウントダウンを表示
//                String text = "リスポーンまで、あと" + String.valueOf(i) + "秒";
//                Component textComponent = Component.literal(text);
//                serverPlayers.forEach(e -> e.sendSystemMessage(textComponent));
//
//                // 音を鳴らす（プレイヤーの近くで音を出す）
//                serverPlayers.forEach(e -> e.playSound(SoundEvents.LEVER_CLICK, 0.3F, 1.0F));
//
//                // 1秒待つ
//                try {
//                    TimeUnit.SECONDS.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            serverPlayers.forEach(e -> e.setGameMode(GameType.SURVIVAL));  // サバイバルに変更
//        }, 0, 1, TimeUnit.SECONDS);
//
//    }
}
