package net.minecraftforge.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RegisterCommandsEvent {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final Commands.CommandSelection selection;
    private final CommandBuildContext context;

    public RegisterCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection selection, CommandBuildContext context) {
        this.dispatcher = dispatcher;
        this.selection = selection;
        this.context = context;
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() { return dispatcher; }
    public Commands.CommandSelection getCommandSelection() { return selection; }
    public CommandBuildContext getBuildContext() { return context; }
}
