package com.northland.battledxuanjian.command;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.capture.CapturePoint;
import com.northland.battledxuanjian.capture.PointRegion;
import com.northland.battledxuanjian.game.Side;
import com.northland.battledxuanjian.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * {@code /bx} 指令总入口（需求 §3）。
 *
 * <p>管理员指令权限：{@code battledxuanjian.admin}；玩家指令权限：{@code battledxuanjian.player}。</p>
 */
public final class BxCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "battledxuanjian.admin";
    private static final String PLAYER = "battledxuanjian.player";
    private static final List<String> ROOT = List.of(
            "help", "join", "leave", "team", "status", "stats",
            "start", "stop", "reload", "pos1", "pos2", "point", "spawn", "kithand", "set", "lobby", "selftest");

    private final BattledXuanjianPlugin plugin;

    public BxCommand(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "help" -> help(sender);
                case "join" -> join(sender);
                case "leave" -> leave(sender);
                case "team" -> team(sender, args);
                case "status" -> status(sender);
                case "stats" -> stats(sender, args);
                case "start" -> start(sender);
                case "stop" -> stop(sender);
                case "reload" -> reload(sender);
                case "pos1" -> setPos(sender, true);
                case "pos2" -> setPos(sender, false);
                case "point" -> point(sender, args);
                case "spawn" -> spawn(sender, args);
                case "kithand" -> kithand(sender, args);
                case "set" -> set(sender, args);
                case "lobby" -> lobby(sender, args);
                case "selftest" -> selftest(sender);
                default -> plugin.messages().send(sender, "unknown-subcommand", "input", args[0]);
            }
        } catch (Exception exception) {
            sender.sendMessage("§c指令执行出错: " + exception);
            plugin.getLogger().warning("执行 /bx " + String.join(" ", args) + " 失败: " + exception);
        }
        return true;
    }

    // ---------------------------------------------------------------- 基础

    private void help(CommandSender sender) {
        boolean admin = sender.hasPermission(ADMIN);
        sender.sendMessage("§6===== 玄剑·战争 BattledXuanjian =====");
        sender.sendMessage("§e/bx join §7- 加入战局（未开始时进入等待区）");
        sender.sendMessage("§e/bx leave §7- 离开战局");
        sender.sendMessage("§e/bx team join <C/M> §7- 选择加入 C 军（守）或 M 军（攻）");
        sender.sendMessage("§e/bx status §7- 查看当前战局状态");
        sender.sendMessage("§e/bx stats [reset] §7- 查看/清空累计统计");
        if (admin) {
            sender.sendMessage("§e/bx start §7- 开始一局游戏（进入倒计时）");
            sender.sendMessage("§e/bx stop §7- 强制结束并重置当前对局");
            sender.sendMessage("§e/bx reload §7- 重载 config.yml");
            sender.sendMessage("§e/bx pos1 | /bx pos2 §7- 设置据点选区对角点");
            sender.sendMessage("§e/bx point create|remove|list <名称> §7- 据点管理");
            sender.sendMessage("§e/bx spawn <C/M> §7- 设置阵营出发点");
            sender.sendMessage("§e/bx kithand <C/M> §7- 用手中潜影盒设置阵营装备包");
            sender.sendMessage("§e/bx set reinforcements <数量> §7- 设置攻方公共兵力");
            sender.sendMessage("§e/bx set time <分钟> §7- 设置时间限制（0 = 无限制）");
            sender.sendMessage("§e/bx lobby set §7- 设置等待区大厅坐标");
            sender.sendMessage("§e/bx selftest §7- 运行插件自检（CI/排障用）");
        }
    }

    private void join(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) {
            plugin.game().join(player);
        }
    }

    private void leave(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) {
            plugin.game().leave(player);
        }
    }

    private void team(CommandSender sender, String[] args) {
        if (args.length < 3 || !"join".equalsIgnoreCase(args[1])) {
            plugin.messages().send(sender, "usage", "usage", "/bx team join <C/M>");
            return;
        }
        if (!sender.hasPermission(PLAYER)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        Side side = Side.byId(args[2]);
        if (side == null) {
            plugin.messages().send(sender, "usage", "usage", "/bx team join <C/M>");
            return;
        }
        Player player = requirePlayer(sender);
        if (player != null) {
            plugin.game().preselect(player, side);
        }
    }

    private void status(CommandSender sender) {
        for (String line : plugin.game().statusLines()) {
            sender.sendMessage(line);
        }
    }

    private void stats(CommandSender sender, String[] args) {
        if (args.length > 1 && "reset".equalsIgnoreCase(args[1])) {
            if (!sender.hasPermission(ADMIN)) {
                plugin.messages().send(sender, "no-permission");
                return;
            }
            plugin.stats().resetLifetime();
            plugin.messages().send(sender, "stats-reset");
            return;
        }
        List<String> lines = plugin.stats().lifetimeLines();
        if (lines.isEmpty()) {
            plugin.messages().send(sender, "stats-empty");
            return;
        }
        sender.sendMessage(plugin.messages().get("stats-header"));
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    // ---------------------------------------------------------------- 管理员

    private void start(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        plugin.game().start(sender);
    }

    private void stop(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        plugin.game().forceStop(sender);
    }

    private void reload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        plugin.reloadPluginConfig();
        plugin.kits().load();
        plugin.stats().load();
        plugin.points().load();
        plugin.teams().refreshAll();
        plugin.messages().send(sender, "reloaded");
    }

    private void setPos(CommandSender sender, boolean first) {
        if (!requireAdmin(sender)) {
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Location location = player.getLocation();
        if (first) {
            plugin.points().setPos1(player, location);
            plugin.messages().send(sender, "pos1-set", "location", format(location));
        } else {
            plugin.points().setPos2(player, location);
            plugin.messages().send(sender, "pos2-set", "location", format(location));
        }
    }

    private void point(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "usage", "usage", "/bx point create|remove|list [名称]");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "list" -> {
                if (plugin.points().size() == 0) {
                    plugin.messages().send(sender, "point-list-empty");
                    return;
                }
                sender.sendMessage(plugin.messages().get("point-list-header", "count", plugin.points().size()));
                for (CapturePoint point : plugin.points().points()) {
                    PointRegion region = point.region();
                    String owner = point.owner() == null ? "无主" : plugin.config().teamName(point.owner());
                    sender.sendMessage(plugin.messages().get("point-list-line",
                            "point", point.name(),
                            "world", region.world(),
                            "owner", owner,
                            "color", point.owner() == null ? "§7" : plugin.config().teamColor(point.owner()),
                            "progress", point.progressPercent()));
                }
            }
            case "create" -> {
                if (args.length < 3) {
                    plugin.messages().send(sender, "usage", "usage", "/bx point create <名称>");
                    return;
                }
                String name = args[2];
                if (!name.matches("[A-Za-z0-9_-]{1,16}")) {
                    plugin.messages().send(sender, "point-name-invalid");
                    return;
                }
                Player player = sender instanceof Player p ? p : null;
                if (plugin.points().get(name) != null) {
                    plugin.messages().send(sender, "point-exists", "point", name);
                    return;
                }
                PointRegion region = player == null ? null : plugin.points().selectionRegion(player.getUniqueId());
                if (region == null) {
                    if (player != null && plugin.points().selectionHasDifferentWorlds(player.getUniqueId())) {
                        plugin.messages().send(sender, "point-selection-world-mismatch");
                    } else {
                        plugin.messages().send(sender, "point-need-selection");
                    }
                    return;
                }
                plugin.points().create(name, region);
                plugin.messages().send(sender, "point-created", "point", name, "size", region.sizeLabel());
            }
            case "remove" -> {
                if (args.length < 3) {
                    plugin.messages().send(sender, "usage", "usage", "/bx point remove <名称>");
                    return;
                }
                if (plugin.points().remove(args[2])) {
                    plugin.messages().send(sender, "point-removed", "point", args[2]);
                } else {
                    plugin.messages().send(sender, "point-not-found", "point", args[2]);
                }
            }
            default -> plugin.messages().send(sender, "usage", "usage", "/bx point create|remove|list [名称]");
        }
    }

    private void spawn(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "usage", "usage", "/bx spawn <C/M> [攻/守]");
            return;
        }
        Side side = Side.byId(args[1]);
        if (side == null) {
            plugin.messages().send(sender, "usage", "usage", "/bx spawn <C/M> [攻/守]");
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        plugin.points().setSpawn(side, player.getLocation());
        plugin.messages().send(sender, "spawn-set", "side", side.id(), "color", plugin.config().teamColor(side));
    }

    private void kithand(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "usage", "usage", "/bx kithand <C/M>");
            return;
        }
        Side side = Side.byId(args[1]);
        if (side == null) {
            plugin.messages().send(sender, "usage", "usage", "/bx kithand <C/M>");
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!plugin.kits().captureFromMainHand(player, side)) {
            plugin.messages().send(sender, "kit-need-shulker");
            return;
        }
        var kit = plugin.kits().kitOf(side);
        plugin.messages().send(sender, "kit-saved",
                "side", side.id(),
                "color", plugin.config().teamColor(side),
                "count", kit == null ? 0 : kit.contentCount(),
                "armor", kit == null ? 0 : kit.armorCount(),
                "offhand", kit == null ? 0 : kit.offhandCount());
    }

    private void set(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "usage", "usage", "/bx set reinforcements <数量> | /bx set time <分钟>");
            return;
        }
        String key = args[1].toLowerCase(Locale.ROOT);
        try {
            switch (key) {
                case "reinforcements", "reinf" -> {
                    int count = Integer.parseInt(args[2]);
                    plugin.getConfig().set("reinforcements.M", count);
                    plugin.saveConfig();
                    plugin.reloadPluginConfig();
                    if (plugin.game().phase() != com.northland.battledxuanjian.game.GamePhase.RUNNING) {
                        plugin.game().pool().set(count);
                    }
                    plugin.messages().send(sender, "reinforcements-set", "count", count);
                }
                case "time" -> {
                    int minutes = Integer.parseInt(args[2]);
                    plugin.getConfig().set("game.time-limit", Math.max(0, minutes));
                    plugin.saveConfig();
                    plugin.reloadPluginConfig();
                    plugin.messages().send(sender, "time-set", "minutes", minutes);
                }
                default -> plugin.messages().send(sender, "usage",
                        "usage", "/bx set reinforcements <数量> | /bx set time <分钟>");
            }
        } catch (NumberFormatException exception) {
            plugin.messages().send(sender, "usage",
                    "usage", "/bx set reinforcements <数量> | /bx set time <分钟>");
        }
    }

    private void lobby(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2 || !"set".equalsIgnoreCase(args[1])) {
            plugin.messages().send(sender, "usage", "usage", "/bx lobby set");
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Location location = player.getLocation();
        plugin.getConfig().set("game.lobby.world", location.getWorld() == null ? "" : location.getWorld().getName());
        plugin.getConfig().set("game.lobby.x", location.getX());
        plugin.getConfig().set("game.lobby.y", location.getY());
        plugin.getConfig().set("game.lobby.z", location.getZ());
        plugin.getConfig().set("game.lobby.yaw", (double) location.getYaw());
        plugin.getConfig().set("game.lobby.pitch", (double) location.getPitch());
        plugin.saveConfig();
        plugin.reloadPluginConfig();
        plugin.messages().send(sender, "lobby-set");
    }

    private void selftest(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        new SelfTest(plugin).run(sender);
    }

    // ---------------------------------------------------------------- 工具

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission(ADMIN)) {
            return true;
        }
        plugin.messages().send(sender, "no-permission");
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        plugin.messages().send(sender, "player-only");
        return null;
    }

    private static String format(Location location) {
        return (location.getWorld() == null ? "?" : location.getWorld().getName())
                + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    // ---------------------------------------------------------------- 补全

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        boolean admin = sender.hasPermission(ADMIN);
        if (args.length == 1) {
            for (String option : ROOT) {
                if (isAdminSub(option) && !admin) {
                    continue;
                }
                addIfMatches(result, option, args[0]);
            }
            return result;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "team" -> {
                if (args.length == 2) {
                    addIfMatches(result, "join", args[1]);
                } else if (args.length == 3) {
                    addIfMatches(result, "C", args[2]);
                    addIfMatches(result, "M", args[2]);
                }
            }
            case "point" -> {
                if (args.length == 2) {
                    addIfMatches(result, "create", args[1]);
                    addIfMatches(result, "remove", args[1]);
                    addIfMatches(result, "list", args[1]);
                } else if (args.length == 3 && "remove".equalsIgnoreCase(args[1])) {
                    for (String name : plugin.points().names()) {
                        addIfMatches(result, name, args[2]);
                    }
                }
            }
            case "spawn", "kithand" -> {
                if (args.length == 2) {
                    addIfMatches(result, "C", args[1]);
                    addIfMatches(result, "M", args[1]);
                }
            }
            case "set" -> {
                if (args.length == 2) {
                    addIfMatches(result, "reinforcements", args[1]);
                    addIfMatches(result, "time", args[1]);
                }
            }
            case "lobby" -> {
                if (args.length == 2) {
                    addIfMatches(result, "set", args[1]);
                }
            }
            case "stats" -> {
                if (args.length == 2 && admin) {
                    addIfMatches(result, "reset", args[1]);
                }
            }
            default -> {
                // 无额外补全
            }
        }
        return result;
    }

    private static boolean isAdminSub(String option) {
        return switch (option) {
            case "start", "stop", "reload", "pos1", "pos2", "point", "spawn", "kithand", "set", "lobby", "selftest" -> true;
            default -> false;
        };
    }

    private static void addIfMatches(List<String> result, String option, String prefix) {
        if (option.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
            result.add(option);
        }
    }

    /** 便于自检输出使用的文本工具。 */
    static String colorize(String text) {
        return Text.color(text);
    }
}
