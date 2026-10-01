package com.northland.battledxuanjian.command;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.capture.CapturePoint;
import com.northland.battledxuanjian.capture.PointRegion;
import com.northland.battledxuanjian.game.GamePhase;
import com.northland.battledxuanjian.game.ReinforcementPool;
import com.northland.battledxuanjian.game.Side;
import com.northland.battledxuanjian.game.TeamBalancer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * 运行期自检（CI 冒烟测试与线上排障使用）。
 *
 * <p>在真实服务端中验证配置解析、计分板/队伍/BossBar API、占领状态机、阵营分配、
 * 兵力池、据点持久化、装备标记与指令注册等关键路径。</p>
 */
public final class SelfTest {

    private static final String TAG = "[BX-SELFTEST]";

    private final BattledXuanjianPlugin plugin;
    private final List<String> results = new ArrayList<>();
    private int passed;
    private int failed;
    private int warned;

    public SelfTest(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    /** 执行全部检查。 */
    public boolean run(CommandSender sender) {
        results.clear();
        passed = 0;
        failed = 0;
        warned = 0;

        say("START version=" + plugin.getDescription().getVersion()
                + " server=" + Bukkit.getVersion()
                + " bukkit=" + Bukkit.getBukkitVersion()
                + " phase=" + plugin.game().phase().display());

        check("config-loaded", () -> {
            var cfg = plugin.config();
            require(cfg.minPlayers >= 1, "min-players 必须 >= 1");
            require(cfg.captureBaseSeconds > 0, "capture.base-time 必须 > 0");
            require(cfg.respawnDelaySeconds >= 0, "respawn.delay 必须 >= 0");
            return "min-players=" + cfg.minPlayers + " countdown=" + cfg.countdownSeconds
                    + " base-time=" + cfg.captureBaseSeconds + " speed=" + cfg.captureSpeedPerPlayer;
        });

        check("messages-render", () -> {
            String text = plugin.messages().get("victory", "side", "M", "color", "§c", "reason", "测试");
            require(text != null && !text.isBlank(), "胜利消息渲染为空");
            require(!text.contains("{"), "消息占位符未被替换: " + text);
            return text;
        });

        check("command-registered", () -> {
            var command = plugin.getCommand("bx");
            require(command != null, "/bx 指令未注册");
            require(command.getExecutor() != null, "/bx 未设置执行器");
            require(command.getTabCompleter() != null, "/bx 未设置补全器");
            return "aliases=" + command.getAliases();
        });

        check("world-available", () -> {
            require(!Bukkit.getWorlds().isEmpty(), "服务端没有任何世界");
            var world = Bukkit.getWorlds().get(0);
            var spawn = world.getSpawnLocation();
            PointRegion region = new PointRegion(world.getName(),
                    spawn.getBlockX() - 1, spawn.getBlockY() - 1, spawn.getBlockZ() - 1,
                    spawn.getBlockX() + 1, spawn.getBlockY() + 1, spawn.getBlockZ() + 1);
            require(region.contains(world.getName(), spawn.getX(), spawn.getY(), spawn.getZ()), "选区包含判断失败");
            require(region.volume() == 27, "选区体积计算失败: " + region.volume());
            return "world=" + world.getName() + " spawn=" + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ();
        });

        check("scoreboard-team-api", () -> {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = board.getTeam("SDT");
            if (team != null) {
                team.unregister();
            }
            team = board.registerNewTeam("SDT");
            team.setPrefix("§9[自检] ");
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
            require("§9[自检] ".equals(team.getPrefix()), "队伍前缀设置失败");
            require(team.getOption(Team.Option.NAME_TAG_VISIBILITY) == Team.OptionStatus.FOR_OWN_TEAM,
                    "队伍头顶名隐藏选项设置失败");
            team.unregister();
            return "prefix/option ok";
        });

        check("bossbar-api", () -> {
            var bar = Bukkit.createBossBar("§e自检 BossBar", org.bukkit.boss.BarColor.YELLOW, org.bukkit.boss.BarStyle.SOLID);
            bar.setProgress(0.42);
            require(Math.abs(bar.getProgress() - 0.42) < 1e-6, "BossBar 进度设置失败");
            bar.setColor(org.bukkit.boss.BarColor.RED);
            bar.setTitle("自检完成");
            bar.removeAll();
            bar.setVisible(false);
            return "progress/color/title ok";
        });

        boolean destructive = plugin.game().phase() != GamePhase.RUNNING;
        if (destructive) {
            check("sidebar-api", () -> {
                Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
                Objective objective = board.getObjective("SDT_OBJ");
                if (objective == null) {
                    objective = board.registerNewObjective("SDT_OBJ", Criteria.DUMMY, "§6自检");
                }
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                objective.getScore("§f自检行 1").setScore(2);
                objective.getScore("§f自检行 2").setScore(1);
                objective.setDisplaySlot(null);
                objective.unregister();
                return "objective/score ok";
            });

            check("point-persistence", () -> {
                String name = "SELFTEST";
                plugin.points().remove(name);
                PointRegion region = new PointRegion(Bukkit.getWorlds().get(0).getName(), 0, 60, 0, 4, 70, 4);
                require(plugin.points().create(name, region), "据点创建失败");
                plugin.points().save();
                plugin.points().load();
                CapturePoint loaded = plugin.points().get(name);
                require(loaded != null, "据点重载后丢失");
                require(loaded.region().volume() == region.volume(), "据点范围重载后不一致");
                require(loaded.owner() == Side.C, "据点初始归属应为 C 军");
                plugin.points().remove(name);
                return "create/save/load/remove ok";
            });
        } else {
            warn("sidebar-api", "对局进行中，跳过（避免干扰现场）");
            warn("point-persistence", "对局进行中，跳过（避免干扰现场）");
        }

        check("capture-state-machine", () -> {
            var cfg = plugin.config();
            CapturePoint point = new CapturePoint("T", new PointRegion("world", 0, 0, 0, 1, 1, 1), Side.C);
            double tickSeconds = 0.5;
            double decaptureAt = simulate(point, Side.M, 1, cfg.captureBaseSeconds, cfg.captureSpeedPerPlayer,
                    tickSeconds, 400, CapturePoint.Event.DECAPTURED);
            require(decaptureAt > 0, "15 秒内未完成脱离阶段");
            require(Math.abs(decaptureAt - cfg.captureBaseSeconds) < 1.0,
                    "单人脱离耗时异常: " + decaptureAt + "s (期望 " + cfg.captureBaseSeconds + "s)");
            double captureAt = simulate(point, Side.M, 1, cfg.captureBaseSeconds, cfg.captureSpeedPerPlayer,
                    tickSeconds, 400, CapturePoint.Event.CAPTURED);
            require(captureAt > 0, "脱离后未完成占领");
            require(point.owner() == Side.M, "占领后归属应为 M 军");
            return "单人脱离=" + round(decaptureAt) + "s 占领=" + round(captureAt) + "s";
        });

        check("capture-speed-scaling", () -> {
            var cfg = plugin.config();
            CapturePoint point = new CapturePoint("T2", new PointRegion("world", 0, 0, 0, 1, 1, 1), Side.C);
            double fast = simulate(point, Side.M, 2, cfg.captureBaseSeconds, cfg.captureSpeedPerPlayer,
                    0.5, 400, CapturePoint.Event.DECAPTURED);
            require(fast > 0, "多人未完成脱离阶段");
            require(fast < cfg.captureBaseSeconds, "多人时应该更快: " + fast);
            return "2 人脱离=" + round(fast) + "s < 单人 " + cfg.captureBaseSeconds + "s";
        });

        check("capture-freeze", () -> {
            CapturePoint point = new CapturePoint("T3", new PointRegion("world", 0, 0, 0, 1, 1, 1), Side.C);
            CapturePoint.Result result = point.tick(2, 2, 15, 0.5, 10);
            require(result.event() == CapturePoint.Event.NONE, "双方同时在场时应冻结");
            require(point.progressPercent() == 100, "冻结时进度不应变化");
            return "双方在场进度冻结";
        });

        check("team-balancer", () -> {
            List<UUID> players = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                players.add(UUID.randomUUID());
            }
            TeamBalancer.Result even = TeamBalancer.balance(players, Map.of(), new Random(42));
            require(even.isBalanced(), "随机分配人数差 > 1: " + even.countC() + "/" + even.countM());
            Map<UUID, Side> preferences = new HashMap<>();
            for (UUID id : players) {
                preferences.put(id, Side.C);
            }
            TeamBalancer.Result skewed = TeamBalancer.balance(players, preferences, new Random(7));
            require(skewed.isBalanced(), "全员预选 C 时人数差 > 1: " + skewed.countC() + "/" + skewed.countM());
            require(skewed.overridden() > 0, "全员预选时应触发平衡调整");
            return "随机 " + even.countC() + "/" + even.countM()
                    + "，全员预选 C → " + skewed.countC() + "/" + skewed.countM()
                    + "（调整 " + skewed.overridden() + " 人）";
        });

        check("reinforcement-pool", () -> {
            ReinforcementPool pool = new ReinforcementPool(50);
            require(pool.consume(), "兵力消耗失败");
            require(pool.remaining() == 49, "兵力扣减错误: " + pool.remaining());
            pool.set(1);
            require(pool.consume(), "最后一次消耗失败");
            require(pool.depleted(), "兵力归零后应判定耗尽");
            ReinforcementPool unlimited = new ReinforcementPool(-1);
            require(unlimited.unlimited() && !unlimited.depleted(), "无限兵力判定错误");
            return "50→49→0，-1 为无限";
        });

        check("kit-item-tagging", () -> {
            ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            ItemStack tagged = plugin.kits().tag(sword, Side.M);
            require(tagged != null, "装备标记失败");
            require(plugin.kits().kitSide(tagged) == Side.M, "装备阵营标记读取失败");
            require(plugin.kits().kitSide(sword) == null, "未标记物品应返回 null");
            return "PDC 阵营标记读写正常";
        });

        check("sidebar-lines", () -> {
            List<String> lines = plugin.game().sidebarLines();
            require(lines.size() >= 2, "侧边栏内容过少");
            for (String line : lines) {
                require(!line.contains("{"), "侧边栏占位符未替换: " + line);
            }
            return lines.size() + " 行";
        });

        check("optional-hooks", () -> {
            boolean protocol = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
            boolean placeholder = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
            String detail = "ProtocolLib=" + (protocol ? "已安装" : "未安装")
                    + " PlaceholderAPI=" + (placeholder ? "已安装" : "未安装")
                    + " tab-service=" + plugin.tab().getClass().getSimpleName();
            if (!protocol && plugin.config().hideEnemyTab) {
                warn("optional-hooks", detail + "（Tab 名字隐藏需要 ProtocolLib，已按需求降级）");
                return null;
            }
            return detail;
        });

        int total = passed + failed + warned;
        StringBuilder summary = new StringBuilder();
        summary.append("SUMMARY total=").append(total)
                .append(" pass=").append(passed)
                .append(" warn=").append(warned)
                .append(" fail=").append(failed);
        say(summary.toString());
        say("RESULT=" + (failed == 0 ? "PASS" : "FAIL"));

        if (sender != null) {
            sender.sendMessage(plugin.messages().get("selftest-header"));
            for (String line : results) {
                sender.sendMessage(line);
            }
            sender.sendMessage(plugin.messages().get("selftest-footer", "result", failed == 0 ? "PASS" : "FAIL"));
        }
        return failed == 0;
    }

    /** 持续推进状态机直到目标事件发生，返回耗时（秒），未发生返回 -1。 */
    private static double simulate(CapturePoint point, Side side, int count, double baseSeconds,
            double speedPerPlayer, double tickSeconds, int maxTicks, CapturePoint.Event target) {
        int c = side == Side.C ? count : 0;
        int m = side == Side.M ? count : 0;
        double elapsed = 0;
        for (int i = 0; i < maxTicks; i++) {
            CapturePoint.Result result = point.tick(c, m, baseSeconds, speedPerPlayer, tickSeconds);
            elapsed += tickSeconds;
            if (result.event() == target) {
                return elapsed;
            }
        }
        return -1;
    }

    private interface Check {
        String run() throws Exception;
    }

    private void check(String name, Check body) {
        try {
            String detail = body.run();
            if (detail == null) {
                return;
            }
            passed++;
            say("PASS " + name + " :: " + detail);
        } catch (Throwable throwable) {
            failed++;
            say("FAIL " + name + " :: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private void warn(String name, String detail) {
        warned++;
        say("WARN " + name + " :: " + detail);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void say(String message) {
        String line = TAG + " " + message;
        results.add(line);
        plugin.getLogger().info(line);
    }
}
