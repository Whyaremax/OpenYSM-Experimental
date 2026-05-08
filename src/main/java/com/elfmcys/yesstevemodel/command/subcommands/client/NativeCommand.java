package com.elfmcys.yesstevemodel.command.subcommands.client;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeRendererDiagnostics;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.YSMMessageFormatter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class NativeCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("native")
                .then(Commands.literal("status").executes(NativeCommand::status))
                .then(Commands.literal("parity").executes(NativeCommand::parity));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack sourceStack = context.getSource();
        sourceStack.sendSystemMessage(YSMMessageFormatter.withPrefix(Component.literal("native loader: " + NativeLibLoader.getStatusSummary())));
        sourceStack.sendSystemMessage(YSMMessageFormatter.withPrefix(Component.literal("native renderer: " + NativeRendererDiagnostics.getStatusSummary())));
        return Command.SINGLE_SUCCESS;
    }

    private static int parity(CommandContext<CommandSourceStack> context) {
        CommandSourceStack sourceStack = context.getSource();
        ModelAssembly assembly = ClientModelManager.getLocalModelContext();
        if (assembly == null || assembly.getAnimationBundle() == null) {
            sourceStack.sendSystemMessage(YSMMessageFormatter.withPrefix(Component.literal("native parity: no local model is loaded")));
            return 0;
        }

        GeoModel model = assembly.getAnimationBundle().getMainModel();
        NativeRendererDiagnostics.ParityReport report = NativeRendererDiagnostics.runParityCheck(model);
        sourceStack.sendSystemMessage(YSMMessageFormatter.withPrefix(Component.literal(report.toDisplayString())));
        sourceStack.sendSystemMessage(YSMMessageFormatter.withPrefix(Component.literal("native loader: " + report.nativeStatus())));
        return report.passed() ? Command.SINGLE_SUCCESS : 0;
    }
}
