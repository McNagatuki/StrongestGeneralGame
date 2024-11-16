package com.github.mcnagatuki.strongestgeneralgame;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class DamageTracker {

    // プレイヤーごとの最新ダメージ情報を格納するマップ
    private static final Map<String, DamageRecord> damageRecordMap = new HashMap<>();


    // 被ダメージイベントを記録
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // check if player damaged
        LivingEntity damagedEntity = event.getEntity();
        if (!(damagedEntity instanceof Player damagedPlayer)) {
            return;
        }

        if (!TeamManager.isPlayerInGame(damagedPlayer)) {
            return;
        }

        DamageSource hurtSource = event.getSource();
        if (hurtSource == null) {
            return;
        }

        Entity hurtByEntity = hurtSource.getEntity();
        if (!(hurtByEntity instanceof Player hurtByPlayer)) {
            return;
        }

        if (!TeamManager.isPlayerInGame(hurtByPlayer)) {
            return;
        }

        if (TeamManager.isFallenWarrior(hurtByPlayer)) {
            return;
        }

        // ダメージ記録を更新, やったのもやられたのもSGGプレイヤー、かつやったのは落ち武者ではない。
        DamageRecord record = new DamageRecord(System.nanoTime(), hurtByPlayer.getName().getString());
        damageRecordMap.put(damagedPlayer.getName().getString(), record);
    }

    // プレイヤーに対する最新のダメージ情報を取得
    public static Optional<String> getLatestHurtByPlayerName(Player player, double durationSecond) {
        DamageRecord damageRecord = damageRecordMap.get(player.getName().getString());
        if (damageRecord == null) {
            return Optional.empty();
        }

        if (damageRecord.getDurationInSeconds() > durationSecond) {
            return Optional.empty();
        }

        return Optional.of(damageRecord.attackerName());
    }

    public static void clear() {
        damageRecordMap.clear();
    }


    /**
     * @param timestampNanoTime    ダメージを受けた時間
     * @param attackerName ダメージを与えたプレイヤーの名前
     */ // ダメージ記録を保持するクラス
    public record DamageRecord(long timestampNanoTime, String attackerName) {
        // ダメージ発生から現在時刻までの経過時間（秒単位）を取得
        public double getDurationInSeconds() {
            long durationNano = System.nanoTime() - timestampNanoTime;  // 経過時間（ナノ秒単位）
            return ((double) durationNano) * 1e-9;  // 秒単位に変換して返す
        }
    }
}
