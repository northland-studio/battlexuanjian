package com.northland.battledxuanjian;

import com.northland.battledxuanjian.capture.PointManager;
import com.northland.battledxuanjian.command.BxCommand;
import com.northland.battledxuanjian.config.BxConfig;
import com.northland.battledxuanjian.config.Messages;
import com.northland.battledxuanjian.game.GameManager;
import com.northland.battledxuanjian.game.GamePhase;
import com.northland.battledxuanjian.game.StatsManager;
import com.northland.battledxuanjian.game.TeamManager;
import com.northland.battledxuanjian.hook.BxPlaceholderExpansion;
import com.northland.battledxuanjian.kit.KitManager;
import com.northland.battledxuanjian.listener.ChatListener;
import com.northland.battledxuanjian.listener.GameListener;
import com.northland.battledxuanjian.respawn.RespawnManager;
import com.northland.battledxuanjian.tab.TabListService;
import com.northland.battledxuanjian.ui.BossBarService;
import com.northland.battledxuanjian.ui.BroadcastService;
import com.northland.battledxuanjian.ui.SidebarService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 玄剑·战争 BattledXuanjian 主类。
 *
 * <p>类《战地》大战场玩法：随机分队、据点占领、兵力消耗、攻防胜负判定、阵营显示与隐藏 ID。
 * 目标服务端为 Paper 26.2+（Java 25）。</p>
 */
public final class BattledXuanjianPlugin extends JavaPlugin {

    private final Messages messages = new Messages();

    private BxConfig config;
    private GameManager game;
    private TeamManager teams;
    private PointManager points;
    private KitManager kits;
    private StatsManager stats;
    private SidebarService sidebar;
    private BossBarService bossBars;
    private BroadcastService broadcast;
    private TabListService tab;
    private RespawnManager respawn;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        bossBars = new BossBarService(this);
        broadcast = new BroadcastService(this);
        sidebar = new SidebarService(this);
        teams = new TeamManager(this);
        kits = new KitManager(this);
        stats = new StatsManager(this);
        points = new PointManager(this);
        game = new GameManager(this);
        respawn = new RespawnManager(this);

        kits.load();
        stats.load();
        points.load();

        BxCommand executor = new BxCommand(this);
        PluginCommand command = getCommand("bx");
        if (command == null) {
            getLogger().severe("plugin.yml 缺少 bx 指令定义，指令将不可用！");
        } else {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        tab = TabListService.create(this);
        hookPlaceholderApi();

        getLogger().info("BattledXuanjian v" + getDescription().getVersion()
                + " 玄剑·战争已启用（据点 " + points.size() + " 个，阶段 " + game.phase().display() + "）");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.shutdown();
        }
        if (tab != null) {
            tab.shutdown();
        }
        if (teams != null) {
            teams.reset();
        }
        if (bossBars != null) {
            bossBars.clearAll();
        }
        if (sidebar != null) {
            sidebar.hide();
        }
        if (points != null) {
            points.save();
        }
        if (stats != null && config != null && config.statsPersist) {
            stats.save();
        }
        getLogger().info("BattledXuanjian 已卸载。");
    }

    /** 重新读取 config.yml 与消息文本（{@code /bx reload}）。 */
    public void reloadPluginConfig() {
        reloadConfig();
        config = BxConfig.load(getConfig());
        messages.load(getConfig());
        if (game != null && game.phase() != GamePhase.RUNNING) {
            game.pool().set(config.reinforcementsM);
        }
    }

    private void hookPlaceholderApi() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            BxPlaceholderExpansion expansion = new BxPlaceholderExpansion(this);
            if (expansion.register()) {
                getLogger().info("已注册 PlaceholderAPI 变量扩展：%bx_reinforcements% / %bx_time% / %bx_point_<名称>% 等。");
            }
        } catch (Throwable throwable) {
            getLogger().warning("PlaceholderAPI 变量扩展注册失败：" + throwable);
        }
    }

    // ---------------------------------------------------------------- 访问器

    public BxConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    public GameManager game() {
        return game;
    }

    public TeamManager teams() {
        return teams;
    }

    public PointManager points() {
        return points;
    }

    public KitManager kits() {
        return kits;
    }

    public StatsManager stats() {
        return stats;
    }

    public SidebarService sidebar() {
        return sidebar;
    }

    public BossBarService bossBars() {
        return bossBars;
    }

    public BroadcastService broadcast() {
        return broadcast;
    }

    public TabListService tab() {
        return tab;
    }

    public RespawnManager respawn() {
        return respawn;
    }
}
