package com.northland.battledxuanjian.listener;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.game.Side;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 聊天栏处理（需求 §10）：
 * <ul>
 *   <li>聊天内容带阵营前缀；</li>
 *   <li>{@code teams.chat-mode: TEAM_ONLY} 时仅同阵营玩家可见。</li>
 * </ul>
 */
public final class ChatListener implements Listener {

    private final BattledXuanjianPlugin plugin;

    public ChatListener(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player source = event.getPlayer();
        Side senderSide = plugin.game().sideOf(source);
        if (senderSide == null) {
            return;
        }

        String prefix = plugin.config().teamPrefix(senderSide);
        event.renderer((player, displayName, message, viewer) -> LegacyComponentSerializer.legacySection()
                .deserialize(prefix)
                .append(Component.text("<" + player.getName() + "> "))
                .append(message));

        if (!plugin.config().teamOnlyChat()) {
            return;
        }
        event.viewers().removeIf(audience -> audience instanceof Player viewer
                && plugin.game().sideOf(viewer) != senderSide);
    }
}
