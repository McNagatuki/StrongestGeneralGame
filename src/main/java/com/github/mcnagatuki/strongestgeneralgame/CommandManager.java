package com.github.mcnagatuki.strongestgeneralgame;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * - initialize
 * - start
 * - stop
 */

/*
  sendSuccess(text, true) の場合: 送信者には、白文字で結果が出力。サーバーコンソールにはコマンド実行の表示あり。
  sendSuccess(text, false) の場合: 送信者には、白文字で結果が出力。サーバーコンソールにはコマンド実行の表示なし。
  sendFailure(text) の場合: 送信者には赤文字で結果が出力。サーバーにはコマンド実行結果の表示なし。
  sendSystemMessage(text) の場合: 送信者には、白文字で結果が出力。サーバーコンソールにはコマンド実行の表示なし。
 */

@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class CommandManager {

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("sgg")
                .then(Commands.literal("initialize")
                        .executes(CommandManager::executeInitialize))
                .then(Commands.literal("start")
                        .executes(CommandManager::executeStart))
                .then(Commands.literal("stop")
                        .executes(CommandManager::executeStop))
                .then(Commands.literal("destroy")
                        .executes(CommandManager::executeDestroy)
                );

        event.getDispatcher().register(builder);
    }

    public static int executeInitialize(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.hasPermission(2) || !source.isPlayer()) {
            MainLogic.initializeGame(source);
        } else {
            source.sendFailure(Component.literal("[SGG] Permission denied."));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int executeStart(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.hasPermission(2) || !source.isPlayer()) {
            MainLogic.startGame(source);
        } else {
            source.sendFailure(Component.literal("[SGG] Permission denied."));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int executeStop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.hasPermission(2) || !source.isPlayer()) {
            MainLogic.endGame(source);
        } else {
            source.sendFailure(Component.literal("[SGG] Permission denied."));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int executeDestroy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.hasPermission(2) || !source.isPlayer()) {
            MainLogic.destroy(source);
        } else {
            source.sendFailure(Component.literal("[SGG] Permission denied."));
        }
        return Command.SINGLE_SUCCESS;
    }
}
