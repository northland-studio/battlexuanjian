package com.northland.battledxuanjian.tab;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.game.Side;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 基于 ProtocolLib 的 Tab 名字隐藏实现（需求 §6）。
 *
 * <p>拦截 {@code PLAYER_INFO} 包：当观察者与目标玩家属于不同阵营时，把条目显示名替换为
 * 配置的遮挡文本（默认 {@code §k}），从而只对敌方隐藏 ID，同阵营依旧可见。</p>
 *
 * <p>该类只在 ProtocolLib 存在且可用时被加载。</p>
 */
public final class ProtocolLibTabService implements TabListService {

    private final BattledXuanjianPlugin plugin;
    private final PacketAdapter adapter;

    public ProtocolLibTabService(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
        this.adapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    mask(event);
                } catch (Throwable throwable) {
                    plugin.getLogger().fine("处理 PLAYER_INFO 包失败: " + throwable.getMessage());
                }
            }
        };
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(adapter);
    }

    private void mask(PacketEvent event) {
        if (!plugin.config().hideEnemyTab) {
            return;
        }
        Player viewer = event.getPlayer();
        Side viewerSide = plugin.game().sideOf(viewer);
        if (viewerSide == null) {
            return;
        }
        PacketContainer packet = event.getPacket();
        StructureModifier<Set<EnumWrappers.PlayerInfoAction>> actionModifier = packet.getPlayerInfoActions();
        if (!actionModifier.getFields().isEmpty()) {
            Set<EnumWrappers.PlayerInfoAction> actions = actionModifier.read(0);
            if (actions != null
                    && !actions.contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER)
                    && !actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)) {
                return;
            }
        }

        StructureModifier<List<PlayerInfoData>> listModifier = packet.getPlayerInfoDataLists();
        if (listModifier.getFields().isEmpty()) {
            return;
        }
        List<PlayerInfoData> entries = listModifier.read(0);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        String mask = plugin.config().tabMask;
        List<PlayerInfoData> replaced = new ArrayList<>(entries.size());
        boolean changed = false;
        for (PlayerInfoData data : entries) {
            if (data == null || data.getProfile() == null) {
                replaced.add(data);
                continue;
            }
            UUID id = data.getProfile().getUUID();
            Player target = id == null ? null : Bukkit.getPlayer(id);
            Side targetSide = id == null ? null : plugin.game().sideOf(id);
            if (target == null || targetSide == null || targetSide == viewerSide) {
                replaced.add(data);
                continue;
            }
            WrappedChatComponent display = WrappedChatComponent.fromLegacyText(mask + target.getName());
            replaced.add(new PlayerInfoData(data.getProfile(), data.getLatency(), data.getGameMode(), display));
            changed = true;
        }
        if (changed) {
            listModifier.write(0, replaced);
        }
    }

    @Override
    public void refresh() {
        // 每个数据包都会实时判断，无需额外刷新
    }

    @Override
    public void shutdown() {
        ProtocolLibrary.getProtocolManager().removePacketListener(adapter);
    }
}
