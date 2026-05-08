package net.minecraftforge.client.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class RegisterClientCommandsEvent {
    private final CommandDispatcher<CommandSourceStack> dispatcher;

    public RegisterClientCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher) {
        this.dispatcher = dispatcher;
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return dispatcher;
    }
}
