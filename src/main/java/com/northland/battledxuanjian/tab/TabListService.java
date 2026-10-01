package com.northland.battledxuanjian.tab;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import java.lang.reflect.Constructor;
import org.bukkit.Bukkit;

/**
 * Tab 列表隐藏服务（需求 §6、§10）。
 *
 * <p>原版 Scoreboard Team 只能隐藏头顶名字，无法按观察者隐藏 Tab 列表条目，
 * 因此该功能依赖 ProtocolLib 拦截玩家信息包；未安装或版本不兼容时自动降级为空实现并给出告警。</p>
 */
public interface TabListService {

    /** 配置变更后刷新（实现可为空操作）。 */
    void refresh();

    /** 插件卸载时清理监听器。 */
    void shutdown();

    static TabListService create(BattledXuanjianPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null
                || !Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            plugin.getLogger().warning("未检测到 ProtocolLib：敌方 Tab 名字隐藏已停用（需求 §6 说明该功能需要 ProtocolLib）。");
            return new NoopTabListService();
        }
        try {
            Class.forName("com.comphenix.protocol.ProtocolLibrary", false, TabListService.class.getClassLoader());
            Constructor<?> constructor = Class
                    .forName("com.northland.battledxuanjian.tab.ProtocolLibTabService")
                    .getConstructor(BattledXuanjianPlugin.class);
            TabListService service = (TabListService) constructor.newInstance(plugin);
            plugin.getLogger().info("已接入 ProtocolLib，敌方 Tab 名字隐藏生效。");
            return service;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("ProtocolLib 已安装但无法兼容当前服务端（" + throwable
                    + "）：敌方 Tab 名字隐藏已降级停用。");
            return new NoopTabListService();
        }
    }
}
