package com.snipr.megaperformance.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.snipr.megaperformance.MegaPerformancePlugin;

import javax.annotation.Nonnull;
import java.awt.Color;

public class MegaperfReloadCommand extends CommandBase {

    private final MegaPerformancePlugin plugin;
    private final OptionalArg<String> subcommand;

    public MegaperfReloadCommand(MegaPerformancePlugin plugin) {
        super("megaperf", "Manage MegaPerformance", false);
        this.plugin = plugin;
        this.setAllowsExtraArguments(true);
        this.subcommand = this.withOptionalArg("action", "megaperf.action", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String action = ctx.get(this.subcommand);
        if (action == null || action.isBlank()) {
            String[] parts = ctx.getInputString().trim().split("\\s+", 2);
            if (parts.length > 1) {
                action = parts[1];
            }
        }

        if (action == null || action.isBlank()) {
            action = "reload";
        }

        if ("reload".equalsIgnoreCase(action)) {
            boolean ok = plugin.reloadConfigFromDisk();
            Message msg = ok
                ? Message.raw("[MegaPerf] Reloaded config").color(Color.GREEN)
                : Message.raw("[MegaPerf] Failed to reload config; see logs").color(Color.RED);
            ctx.sendMessage(msg);
            return;
        }

        ctx.sendMessage(Message.join(
            Message.raw("[MegaPerf] Usage: ").color(Color.YELLOW),
            Message.raw("/megaperf reload").color(Color.WHITE)
        ));
    }
}
