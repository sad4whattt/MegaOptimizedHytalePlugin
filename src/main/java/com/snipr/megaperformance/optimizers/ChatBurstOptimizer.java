package com.snipr.megaperformance.optimizers;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.snipr.megaperformance.config.MegaPerformanceConfig;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple chat burst limiter: suppresses duplicate chat within a small window to reduce packet spam.
 * Best-effort: if a player chats again within coalesceChatMs, the message is replaced with a compact notice.
 */
public class ChatBurstOptimizer {

    private final Map<String, Long> lastChatMs = new ConcurrentHashMap<>();

    public void applyFormatter(PlayerChatEvent event, MegaPerformanceConfig.Network netCfg) {
        long now = System.currentTimeMillis();
        long window = Math.max(0, netCfg.coalesceChatMs);
        Long last = lastChatMs.get("global");
        boolean suppress = last != null && (now - last) < window;
        lastChatMs.put("global", now);

        if (!suppress) return;

        event.setFormatter((playerRef, message) -> Message.join(
            Message.raw("[Chat-Limited] ").color(Color.YELLOW),
            Message.raw(playerRef.getUsername()).color(Color.WHITE),
            Message.raw(" is sending messages too quickly.").color(Color.GRAY)
        ));
    }
}
