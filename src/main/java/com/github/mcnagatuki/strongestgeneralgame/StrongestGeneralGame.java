package com.github.mcnagatuki.strongestgeneralgame;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;

import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
//import net.minecraft.entity.player.PlayerEntity;

/**
 * 1. Level#isClientSideを見てfalseを確認。実行がserver sideであることを確かめる。
 * 2. 各種ロジックを登録、コマンドも登録
 * - メインロジック
 *   - キルイベント（デスイベント）の取得
 *   - スペクテイター化
 *   - キルされたのが大将だった場合
 *     - キルされた側をキルした側に全員追加
 *     - キルした側、された側ともにリーダー付近に tp & サバイバル化
 * - その他仕様
 *   - 大将が自死した場合
 *     - 全員「落ち武者」となる。落ち武者が大将になることは二度とない。キルされたらその場でそのチームに入る。
 *     - 面倒だし弱体化もつける。ただただ逃げまどう。
 *     - チームに入っていない自死勢も、全員この扱い。
 *   - 途中参加
 *     - ゲーム開始時に「参加ゲーム」を登録。ログイン時実行中のゲームに対して参加していない場合は途中参加となる。
 *     - 途中参加の人も普通に新しいチームにすればよいか。
 * **/

// The value here should match an entry in the META-INF/mods.toml file
@Mod(StrongestGeneralGame.MODID)
public class StrongestGeneralGame {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "strongestgeneralgame";

    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

//    public static final CommandManager commandManager = new CommandManager();
//    public static final MainLogic mainLogic = new MainLogic();

    public StrongestGeneralGame()
    {
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
//        MinecraftForge.EVENT_BUS.register(StrongestGeneralGame.commandManager);
//        MinecraftForge.EVENT_BUS.register(StrongestGeneralGame.mainLogic);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, com.github.mcnagatuki.strongestgeneralgame.Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("Setup Strongest General Game");

//        if (com.github.mcnagatuki.strongestgeneralgame.Config.logDirtBlock)
//            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
//
//        LOGGER.info(com.github.mcnagatuki.strongestgeneralgame.Config.magicNumberIntroduction + com.github.mcnagatuki.strongestgeneralgame.Config.magicNumber);
//
//        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

//    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
//    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//    public static class ClientModEvents
//    {
//        @SubscribeEvent
//        public static void onClientSetup(FMLClientSetupEvent event)
//        {
//            // Some client setup code
//            LOGGER.info("HELLO FROM CLIENT SETUP");
//            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
//        }
//    }
//}
}
