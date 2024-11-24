package com.github.mcnagatuki.strongestgeneralgame;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class MainLogic {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean running = false;

    // ワールド内コマンド操作用tag, このtagを通じて、コマンドの対象操作であることを伝達する。
    private static final String commandRandomTpTag = "sgg_random_tp";  // TODO: コマンド側で実装。

    // リスポーン用タグ
    private static final String respawnTpTagPrefix = "sgg_respawn_tp_";
    private static final String respawnDeathPoint = respawnTpTagPrefix + "death_point";
    private static final String respawnTeamMember = respawnTpTagPrefix + "team_member";
    private static final String respawnRandomTp = respawnTpTagPrefix + "random";

    // 遅延ゲームモード変更用タグ
    private static final String tickEventTpTagPrefix = "sgg_tick_event_tp_";
    private static final String tickEventTpDeathPoint = tickEventTpTagPrefix + "death_point";
    private static final String tickEventTpTeamMember = tickEventTpTagPrefix + "team_member";
    private static final String tickEventTpRandom = tickEventTpTagPrefix + "random";

    // 遅延ゲームモード変更用タグ
    private static final String lazyGameModeChangeTag = "sgg_lazy_game_mode_change";


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

        // ゲームモード遅延変更用の準備
        LazyGameModeChanger.initialize(server);

        // 過去のダメージ記録を削除
        DamageTracker.clear();

        // 過去のコマンドタグを削除
        server.getPlayerList().getPlayers().forEach(MainLogic::clearTpGameModeChangeTag);

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

    public static void destroy(CommandSourceStack source) {
        if (running) {
            source.sendFailure(Component.literal("[SGG] Game is still running."));
            return;
        }

        MinecraftServer server = source.getServer();
        TeamManager.destroy(server);

        Supplier<Component> supplier = () -> Component.literal("[SGG] Game is destroyed!");
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

        // Serverのみで動作
        MinecraftServer server = deathPlayer.getServer();
        if (server == null) {
            return;
        }

        // ServerPlayerのみで動作
        if (!(deathPlayer instanceof ServerPlayer deathServerPlayer)) {
            return;
        }

        // sgg_XXXのチームに入っているプレイヤーのみ動作
        if (!TeamManager.isPlayerInGame(deathPlayer)) {
            return;
        }

        // クリエやスペクは対象外。サバイバルやアドベンチャーのみ。
        if (deathServerPlayer.isSpectator() || deathServerPlayer.isCreative()) {
            return;
        }

        // SGGプレイヤーの死亡時処理
        if (TeamManager.isPlayerGeneral(deathServerPlayer)) {
            // 死んだのが大将だった場合
            executeDeathGeneral(server, deathServerPlayer);
        } else if (TeamManager.isFallenWarrior(deathServerPlayer)) {
            // 死んだのが落ち武者だった場合
            executeDeathFallenWarrior(server, deathServerPlayer);
        } else {
            // 死んだのが一般プレイヤーだった場合
            executeDeathCommonSoldier(server, deathServerPlayer);
        }
    }

    private static void executeDeathGeneral(MinecraftServer server, ServerPlayer deathServerPlayer) {
        // 死んだプレイヤーチーム・殺したプレイヤーチームの取得
        Team deathPlayerTeam = deathServerPlayer.getTeam();
        Optional<Team> killerTeam = getWhichTeamKillMe(deathServerPlayer);

        // 死んだチームのメンバーを列挙
        List<Player> deathPlayerTeamMembers = getTeamPlayers(server, deathPlayerTeam);

        // 「大将が死にました」のアナウンス, MODではタグをつけるだけ
        deathPlayerTeamMembers.forEach(MainLogic::announceGeneralDeath);

        // 死んだチームの全員スペクテイター化
        deathPlayerTeamMembers.stream()
                .filter(e -> e instanceof ServerPlayer)
                .map(e -> (ServerPlayer) e)
                .forEach(e -> e.setGameMode(GameType.SPECTATOR));

        /* 死因がSGGプレイヤーによるものではない場合
           あるいは、殺したチームが死んだチームと同じ場合（自殺？や相打ち） */
        if (killerTeam.isEmpty() || killerTeam.get().getName().equals(deathPlayerTeam.getName())) {
            // チームの全プレイヤーを落ち武者化
            TeamManager.moveTeamToFallenWarrior(server, deathPlayerTeam);

            // TODO: サーバーにいないプレイヤーに対しては座標移動やゲームモード変更が効かない。要対応。
            // ランダムtp, tagで操作, 続きはonPlayerTickEvent (生きているなら)
            deathPlayerTeamMembers
                    .stream()
                    .filter(LivingEntity::isAlive)
                    .forEach(e -> {
                        setTpGameModeChangeTag(e, tickEventTpRandom, true);
                    });

            // ランダムtp, tagで操作, 続きはonPlayerRespawn (死んでいるなら)
            deathPlayerTeamMembers
                    .stream()
                    .filter(e -> !e.isAlive())
                    .forEach(e -> {
                        setTpGameModeChangeTag(e, respawnRandomTp, true);
                    });
            return;
        }

        /* 以下、死因がSGGプレイヤーによる場合*/
        // 大将を殺したプレイヤーのチームにチーム変更
        TeamManager.moveTeamToTeam(server, deathPlayerTeam, killerTeam.get());

        // TODO: サーバーにいないプレイヤーに対しては座標移動やゲームモード変更が効かない。要対応。
        // *** 以下の処理は、新チーム全員に適用 ***
        // チームメンバーtp, tagで操作, 続きはonPlayerTickEvent (生きている & スペクなら)
        getTeamPlayers(server, killerTeam.get())
                .stream()
                .filter(LivingEntity::isAlive)
                .filter(Player::isSpectator)
                .forEach(e -> setTpGameModeChangeTag(e, tickEventTpTeamMember, true));


        // チームメンバーtp, tagで操作, 続きはonPlayerRespawn (死んでいるなら)
        getTeamPlayers(server, killerTeam.get())
                .stream()
                .filter(e -> !e.isAlive())
                .forEach(e -> setTpGameModeChangeTag(e, respawnTeamMember, true));
    }

    private static void executeDeathFallenWarrior(MinecraftServer server, ServerPlayer deathServerPlayer) {
        // 殺したプレイヤーチームの取得
        Optional<Team> killerTeam = getWhichTeamKillMe(deathServerPlayer);

        /* 死因がSGGプレイヤーによるものではない場合、または殺したプレイヤーが鯖にいない場合 */
        if (killerTeam.isEmpty()) {
            // ランダムtp, tagで操作, 続きはonPlayerRespawn
            setTpGameModeChangeTag(deathServerPlayer, respawnRandomTp, true);
            return;
        }

        /* 以下、死因がSGGプレイヤーによる場合*/
        // プレイヤーをスペクテイターにする
        deathServerPlayer.setGameMode(GameType.SPECTATOR);

        // プレイヤーのチーム移動
        TeamManager.addPlayerToTeam(deathServerPlayer, killerTeam.get());

        // 新チームのもとにリスポーン, tagで操作, 続きはonPlayerRespawn
        setTpGameModeChangeTag(deathServerPlayer, respawnTeamMember, true);
    }

    private static void executeDeathCommonSoldier(MinecraftServer server, ServerPlayer deathServerPlayer) {
        // プレイヤーをスペクテイターにする
        deathServerPlayer.setGameMode(GameType.SPECTATOR);

        // 死んだ場所にリスポーン, tagで操作, 続きはonPlayerRespawn
        setTpGameModeChangeTag(deathServerPlayer, respawnDeathPoint, false);
    }

    private static void announceGeneralDeath(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Packet<?> titleTextPacketPacket = new ClientboundSetTitleTextPacket(Component.literal("大将が死にました"));
        serverPlayer.connection.send(titleTextPacketPacket);

        Packet<?> subTitleTextPacketPacket = new ClientboundSetSubtitleTextPacket(Component.literal("チームが移ります"));
        serverPlayer.connection.send(subTitleTextPacketPacket);

        double x = serverPlayer.getX();
        double y = serverPlayer.getY();
        double z = serverPlayer.getZ();

        SoundEvent sound = SoundEvents.ANVIL_PLACE;
        float volume = 0.4F;
        float pitch = 1.0F;
        long seed = System.nanoTime();

        Packet<?> soundPacket = new ClientboundSoundPacket(Holder.direct(sound), SoundSource.PLAYERS, x, y, z, volume, pitch, seed);
        serverPlayer.connection.send(soundPacket);
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

    public static void setTpGameModeChangeTag(Player player, String tagString, boolean lazyGameModeChange) {
        // サーバーのみ動作
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 残ってるタグをすべて削除
        clearTpGameModeChangeTag(serverPlayer);

        // タグの追加
        serverPlayer.addTag(tagString);

        // リスポーン後に遅延してゲームモード変更するか
        if (lazyGameModeChange) {
            serverPlayer.addTag(lazyGameModeChangeTag);
        }
    }

    public static void clearTpGameModeChangeTag(ServerPlayer serverPlayer) {
        // コマンドランダムtp用タグを削除
        serverPlayer.removeTag(commandRandomTpTag);

        // リスポーンタグを削除
        List<String> respawnTags = serverPlayer.getTags().stream().filter(e -> e.startsWith(respawnTpTagPrefix)).toList();
        for (String tag : respawnTags) {
            serverPlayer.removeTag(tag);
        }

        // tick eventタグを削除
        List<String> tickEventTags = serverPlayer.getTags().stream().filter(e -> e.startsWith(tickEventTpTagPrefix)).toList();
        for (String tag : tickEventTags) {
            serverPlayer.removeTag(tag);
        }

        // 遅延ゲームモード変更タグを削除
        serverPlayer.removeTag(lazyGameModeChangeTag);

        // 遅延ゲームモード変更の中止
        LazyGameModeChanger.cancel(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        /* 死んでいるプレイヤーに対して、リスポーン時に改めてチームメイトへの移動 & ゲームモードの変更 */

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

        // リスポーンタグを取得
        Optional<String> respawnTag = serverPlayer.getTags().stream().filter(e -> e.startsWith(respawnTpTagPrefix)).findFirst();
        if (respawnTag.isEmpty()) {
            return;
        }

        // ゲームモード変更用のリスポーン設定
        boolean isLazyGameModeChange = serverPlayer.getTags().contains(lazyGameModeChangeTag);

        // 他のリスポーンタグをすべて削除
        clearTpGameModeChangeTag(serverPlayer);

        // メイン処理
        if (respawnTag.get().equals(respawnTeamMember)) {
            TeleportManager.toTeamMember(serverPlayer);
        } else if (respawnTag.get().equals(respawnDeathPoint)) {
            TeleportManager.toDeathPoint(serverPlayer);
        } else if (respawnTag.get().equals(respawnRandomTp)){
            // ランダムtp, コマンドで実装
            serverPlayer.addTag(commandRandomTpTag);
        } else {
            // Do nothing
            return;
        }

        // ゲームモード変更用のリスポーン設定
        if (isLazyGameModeChange) {
            LazyGameModeChanger.register(serverPlayer, 5);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        /* 既にリスポーン済みのプレイヤーに対して、改めてチームメイトへの移動 & ゲームモードの変更 */
        // スペクテイター状態にあるプレイヤーは、onRespawnによる処理ではうまく対応できない。解決するべし。

        // startのみ動作
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        // check if game is running
        if (!running) {
            return;
        }

        // サーバーのみで動作
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }

        // tick event タグの取得
        Optional<String> tickEventTag = serverPlayer.getTags().stream().filter(e -> e.startsWith(tickEventTpTagPrefix)).findFirst();
        if (tickEventTag.isEmpty()) {
            return;
        }

        // ゲームモード変更用のリスポーン設定
        boolean isLazyGameModeChange = serverPlayer.getTags().contains(lazyGameModeChangeTag);

        // 他のリスポーンタグをすべて削除
        clearTpGameModeChangeTag(serverPlayer);

        // メイン処理
        if (tickEventTag.get().equals(tickEventTpTeamMember)) {
            TeleportManager.toTeamMember(serverPlayer);
        } else if (tickEventTag.get().equals(tickEventTpDeathPoint)) {
            TeleportManager.toDeathPoint(serverPlayer);
        } else if (tickEventTag.get().equals(tickEventTpRandom)) {
            // ランダムtp, コマンドで実装
            serverPlayer.addTag(commandRandomTpTag);
        } else {
            // Do nothing
            return;
        }

        if (isLazyGameModeChange) {
            LazyGameModeChanger.register(serverPlayer, 5);
        }
    }
}
