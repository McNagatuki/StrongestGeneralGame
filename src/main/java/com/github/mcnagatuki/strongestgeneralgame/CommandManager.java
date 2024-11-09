package com.github.mcnagatuki.strongestgeneralgame;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * - ready
 * - start
 * - stop
 * - ignore set <team>
 * - ignore delete
 * - ignore get
 */

/*
  sendSuccess(text, true) の場合: 送信者には、白文字で結果が出力。サーバーコンソールにはコマンド実行の表示あり。
  sendSuccess(text, false) の場合: 送信者には、白文字で結果が出力。サーバーコンソールにはコマンド実行の表示なし。
  sendFailure(text) の場合: 送信者には赤文字で結果が出力。サーバーにはコマンド実行結果の表示なし。
  sendSystemMessage(text) の場合: 送信者には、白文字で結果が出力。サーバーコンソールにはコマンド実行の表示なし。
 */


@Mod.EventBusSubscriber(modid = StrongestGeneralGame.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class CommandManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public CommandManager() {
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("sgg")

                .then(Commands.literal("ready")
                        .executes(CommandManager::executeReady))
                .then(Commands.literal("start")
                        .executes(CommandManager::executeStart))
                .then(Commands.literal("stop")
                        .executes(CommandManager::executeStop))
                .then(Commands.literal("ignore")
                        .then(Commands.literal("set")
                                .then(Commands.argument("team", TeamArgument.team())
                                        .executes(CommandManager::executeIgnoreSet)))
                        .then(Commands.literal("del")
                                .executes(CommandManager::executeIgnoreDelete))
                        .then(Commands.literal("get")
                                .executes(CommandManager::executeIgnoreGet))
                );

        event.getDispatcher().register(builder);
    }

    public static int executeReady(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MainLogic.setReady(source);
        return Command.SINGLE_SUCCESS;
    }

    public static int executeStart(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MainLogic.startGame(source);
        return Command.SINGLE_SUCCESS;
    }

    public static int executeStop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MainLogic.endGame(source);
        return Command.SINGLE_SUCCESS;
    }

    public static int executeIgnoreSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PlayerTeam team = TeamArgument.getTeam(context, "team");
        String teamName = team.getName();
        MainLogic.setIgnoreTeam(teamName);

        CommandSourceStack source = context.getSource();
        String text = "[SGG] Team '" + teamName + "' will be ignored.";
        Supplier<Component> supplier = () -> Component.literal(text);
        source.sendSuccess(supplier, true);

        return Command.SINGLE_SUCCESS;
    }

    public static int executeIgnoreDelete(CommandContext<CommandSourceStack> context) {
        MainLogic.delIgnoreTeam();

        CommandSourceStack source = context.getSource();
        String text = "[SGG] Ignore team deleted.";
        Supplier<Component> supplier = () -> Component.literal(text);
        source.sendSuccess(supplier, true);

        return Command.SINGLE_SUCCESS;
    }

    public static int executeIgnoreGet(CommandContext<CommandSourceStack> context) {
        String ignoreTeamName = MainLogic.getIgnoreTeam();

        CommandSourceStack source = context.getSource();
        String text = "[SGG] Ignore team: '" + ignoreTeamName + "'";
        Supplier<Component> supplier = () -> Component.literal(text);
        source.sendSuccess(supplier, true);

        return Command.SINGLE_SUCCESS;
    }
}
