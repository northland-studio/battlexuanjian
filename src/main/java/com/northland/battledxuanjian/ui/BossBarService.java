package com.northland.battledxuanjian.ui;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.capture.CapturePoint;
import com.northland.battledxuanjian.game.Side;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * BossBar 服务（需求 §7.3、§11）：
 * 每个据点一条独立 BossBar，标题形如 {@code 据点 A | C军控制 100%}，颜色随归属变化。
 */
public final class BossBarService {

    private final BattledXuanjianPlugin plugin;
    private final Map<String, BossBar> pointBars = new LinkedHashMap<>();
    private BossBar announceBar;
    private BukkitTask announceTask;

    public BossBarService(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    /** 确保据点 BossBar 存在。 */
    public void ensurePointBar(String name) {
        pointBars.computeIfAbsent(name, key -> {
            BossBar bar = Bukkit.createBossBar(titleFor(key, null), BarColor.WHITE, BarStyle.SOLID);
            bar.setVisible(true);
            for (Player player : Bukkit.getOnlinePlayers()) {
                bar.addPlayer(player);
            }
            return bar;
        });
    }

    public void removePointBar(String name) {
        BossBar bar = pointBars.remove(name);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    /** 用最新的据点列表同步 BossBar（新增/删除/顺序）。 */
    public void syncPointBars(Collection<CapturePoint> points) {
        for (CapturePoint point : points) {
            ensurePointBar(point.name());
            updatePointBar(point);
        }
        for (String stale : new ArrayList<>(pointBars.keySet())) {
            boolean exists = points.stream().anyMatch(point -> point.name().equals(stale));
            if (!exists) {
                removePointBar(stale);
            }
        }
    }

    /** 刷新单条据点 BossBar 的标题、进度与颜色。 */
    public void updatePointBar(CapturePoint point) {
        if (point == null) {
            return;
        }
        BossBar bar = pointBars.get(point.name());
        if (bar == null) {
            return;
        }
        bar.setTitle(titleFor(point.name(), point));
        bar.setProgress(Math.max(0.0, Math.min(1.0, point.progress() / 100.0)));
        bar.setColor(colorFor(point));
        syncViewers(bar);
    }

    private void syncViewers(BossBar bar) {
        if (!plugin.config().broadcastBossBar || !plugin.config().bossBarVisibleToAll) {
            // 关闭广播或未开启“所有人可见”时，直接清空观众（管理员可用 sidebar 查看进度）
            bar.removeAll();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
        for (Player player : new ArrayList<>(bar.getPlayers())) {
            if (!player.isOnline()) {
                bar.removePlayer(player);
            }
        }
    }

    private String titleFor(String name, CapturePoint point) {
        if (point == null) {
            return "§e据点 " + name;
        }
        var cfg = plugin.config();
        String status;
        if (point.owner() != null) {
            status = cfg.teamColor(point.owner()) + cfg.teamName(point.owner()) + "控制";
        } else if (point.contestant() != null) {
            status = cfg.teamColor(point.contestant()) + "争夺中";
        } else {
            status = "§7无主";
        }
        return "§e据点 " + point.name() + " §7| " + status + " §7" + point.progressPercent() + "%";
    }

    private BarColor colorFor(CapturePoint point) {
        Side owner = point.owner();
        if (owner == Side.C) {
            return BarColor.BLUE;
        }
        if (owner == Side.M) {
            return BarColor.RED;
        }
        Side contestant = point.contestant();
        if (contestant == Side.C) {
            return BarColor.BLUE;
        }
        if (contestant == Side.M) {
            return BarColor.RED;
        }
        return BarColor.WHITE;
    }

    /** 临时全服 BossBar 宣告。 */
    public void announce(String text, int seconds) {
        if (!plugin.config().broadcastBossBar) {
            return;
        }
        if (announceBar == null) {
            announceBar = Bukkit.createBossBar(text, BarColor.YELLOW, BarStyle.SEGMENTED_10);
        }
        announceBar.setTitle(text);
        announceBar.setProgress(1.0);
        announceBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            announceBar.addPlayer(player);
        }
        if (announceTask != null) {
            announceTask.cancel();
        }
        announceTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (announceBar != null) {
                announceBar.removeAll();
                announceBar.setVisible(false);
            }
        }, Math.max(20L, seconds * 20L));
    }

    /** 清理所有 BossBar（对局重置时调用）。 */
    public void clearAll() {
        for (BossBar bar : pointBars.values()) {
            bar.removeAll();
            bar.setVisible(false);
        }
        pointBars.clear();
        if (announceBar != null) {
            announceBar.removeAll();
            announceBar.setVisible(false);
            announceBar = null;
        }
        if (announceTask != null) {
            announceTask.cancel();
            announceTask = null;
        }
    }
}
