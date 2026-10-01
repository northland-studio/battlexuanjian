package com.northland.battledxuanjian.capture;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.config.BxConfig;
import com.northland.battledxuanjian.game.Side;
import com.northland.battledxuanjian.util.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * 据点管理（需求 §7）：选区、创建/删除、持久化、占领推进、BossBar 与宣告。
 */
public final class PointManager {

    private final BattledXuanjianPlugin plugin;
    private final Map<String, CapturePoint> points = new LinkedHashMap<>();
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final Map<Side, Location> spawns = new EnumMap<>(Side.class);

    public PointManager(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- 数据读写

    private File file() {
        return new File(plugin.getDataFolder(), plugin.config().pointsFile);
    }

    public void load() {
        points.clear();
        spawns.clear();
        pos1.clear();
        pos2.clear();
        File file = file();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("points");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                ConfigurationSection pointSection = section.getConfigurationSection(name);
                if (pointSection == null) {
                    continue;
                }
                PointRegion region = readRegion(pointSection);
                if (region == null) {
                    plugin.getLogger().warning("据点 " + name + " 的选区数据不完整，已跳过。");
                    continue;
                }
                Side owner = Side.byId(pointSection.getString("owner", "C"));
                points.put(name.toLowerCase(Locale.ROOT), new CapturePoint(name, region, owner == null ? Side.C : owner));
            }
        }
        ConfigurationSection spawnSection = yaml.getConfigurationSection("spawns");
        if (spawnSection != null) {
            for (Side side : Side.values()) {
                Location location = readLocation(spawnSection, side.id());
                if (location != null) {
                    spawns.put(side, location);
                }
            }
        }
        plugin.bossBars().syncPointBars(points.values());
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (CapturePoint point : points.values()) {
            String path = "points." + point.name();
            PointRegion region = point.region();
            yaml.set(path + ".world", region.world());
            yaml.set(path + ".min", List.of(region.minX(), region.minY(), region.minZ()));
            yaml.set(path + ".max", List.of(region.maxX(), region.maxY(), region.maxZ()));
            yaml.set(path + ".owner", point.owner() == null ? "NONE" : point.owner().id());
        }
        for (Map.Entry<Side, Location> entry : spawns.entrySet()) {
            writeLocation(yaml, "spawns." + entry.getKey().id(), entry.getValue());
        }
        try {
            File file = file();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录: " + parent.getAbsolutePath());
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存据点数据失败: " + exception.getMessage());
        }
    }

    private static PointRegion readRegion(ConfigurationSection section) {
        String world = section.getString("world");
        List<Integer> min = section.getIntegerList("min");
        List<Integer> max = section.getIntegerList("max");
        if (world == null || min.size() < 3 || max.size() < 3) {
            return null;
        }
        return new PointRegion(world, min.get(0), min.get(1), min.get(2), max.get(0), max.get(1), max.get(2));
    }

    static void writeLocation(ConfigurationSection section, String path, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        section.set(path + ".world", location.getWorld().getName());
        section.set(path + ".x", location.getX());
        section.set(path + ".y", location.getY());
        section.set(path + ".z", location.getZ());
        section.set(path + ".yaw", location.getYaw());
        section.set(path + ".pitch", location.getPitch());
    }

    static Location readLocation(ConfigurationSection section, String path) {
        String worldName = section.getString(path + ".world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world,
                section.getDouble(path + ".x"),
                section.getDouble(path + ".y"),
                section.getDouble(path + ".z"),
                (float) section.getDouble(path + ".yaw"),
                (float) section.getDouble(path + ".pitch"));
    }

    // ---------------------------------------------------------------- 选区

    public void setPos1(Player player, Location location) {
        pos1.put(player.getUniqueId(), location.clone());
    }

    public void setPos2(Player player, Location location) {
        pos2.put(player.getUniqueId(), location.clone());
    }

    public Location pos1(UUID player) {
        return pos1.get(player);
    }

    public Location pos2(UUID player) {
        return pos2.get(player);
    }

    /** 用玩家当前选区生成区域；缺少任一点或跨世界时返回 {@code null}。 */
    public PointRegion selectionRegion(UUID player) {
        Location first = pos1.get(player);
        Location second = pos2.get(player);
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return null;
        }
        if (!first.getWorld().equals(second.getWorld())) {
            return null;
        }
        return new PointRegion(first.getWorld().getName(),
                first.getBlockX(), first.getBlockY(), first.getBlockZ(),
                second.getBlockX(), second.getBlockY(), second.getBlockZ());
    }

    public boolean selectionHasDifferentWorlds(UUID player) {
        Location first = pos1.get(player);
        Location second = pos2.get(player);
        return first != null && second != null && first.getWorld() != null && second.getWorld() != null
                && !first.getWorld().equals(second.getWorld());
    }

    // ---------------------------------------------------------------- 据点

    public CapturePoint get(String name) {
        return name == null ? null : points.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<CapturePoint> points() {
        return points.values();
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (CapturePoint point : points.values()) {
            names.add(point.name());
        }
        return names;
    }

    public int size() {
        return points.size();
    }

    public boolean create(String name, PointRegion region) {
        String key = name.toLowerCase(Locale.ROOT);
        if (points.containsKey(key)) {
            return false;
        }
        CapturePoint point = new CapturePoint(name, region, Side.C);
        points.put(key, point);
        plugin.bossBars().ensurePointBar(name);
        plugin.bossBars().updatePointBar(point);
        save();
        return true;
    }

    public boolean remove(String name) {
        CapturePoint removed = points.remove(name.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        plugin.bossBars().removePointBar(removed.name());
        save();
        return true;
    }

    public void reset(Side owner) {
        for (CapturePoint point : points.values()) {
            point.setOwner(owner);
            plugin.bossBars().updatePointBar(point);
        }
    }

    public boolean allOwnedBy(Side side) {
        if (points.isEmpty()) {
            return false;
        }
        for (CapturePoint point : points.values()) {
            if (point.owner() != side) {
                return false;
            }
        }
        return true;
    }

    public int countOwnedBy(Side side) {
        int count = 0;
        for (CapturePoint point : points.values()) {
            if (point.owner() == side) {
                count++;
            }
        }
        return count;
    }

    // ---------------------------------------------------------------- 出发点

    public void setSpawn(Side side, Location location) {
        spawns.put(side, location.clone());
        save();
    }

    public Location spawn(Side side) {
        return spawns.get(side);
    }

    // ---------------------------------------------------------------- 占领推进

    /** 推进所有据点的占领进度，并处理事件与胜利判定。 */
    public void tick() {
        if (points.isEmpty()) {
            return;
        }
        BxConfig cfg = plugin.config();
        double seconds = cfg.captureIntervalTicks / 20.0;
        for (CapturePoint point : points.values()) {
            int countC = countInside(point, Side.C);
            int countM = countInside(point, Side.M);
            CapturePoint.Result result = point.tick(countC, countM,
                    cfg.captureBaseSeconds, cfg.captureSpeedPerPlayer, seconds);
            handleResult(point, result, countC, countM);
            plugin.bossBars().updatePointBar(point);
        }
        if (allOwnedBy(Side.M)) {
            plugin.game().endMatch(Side.M, "all-points");
        }
    }

    private int countInside(CapturePoint point, Side side) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.game().sideOf(player) != side) {
                continue;
            }
            Location location = player.getLocation();
            if (location.getWorld() == null) {
                continue;
            }
            if (point.region().contains(location.getWorld().getName(), location.getX(), location.getY(), location.getZ())) {
                count++;
            }
        }
        return count;
    }

    private void handleResult(CapturePoint point, CapturePoint.Result result, int countC, int countM) {
        BxConfig cfg = plugin.config();
        if (result.event() == CapturePoint.Event.NONE) {
            return;
        }
        Side actor = countM > 0 ? Side.M : Side.C;
        String color = cfg.teamColor(actor);
        String name = cfg.teamName(actor);
        switch (result.event()) {
            case DECAPTURED -> {
                Side previous = result.previousOwner() == null ? actor.opponent() : result.previousOwner();
                String text = plugin.messages().get("decaptured",
                        "point", point.name(),
                        "side", cfg.teamName(previous),
                        "color", cfg.teamColor(previous));
                plugin.broadcast().chat(text);
                plugin.broadcast().actionBar(text);
                plugin.broadcast().sound(cfg.soundDecapture, 1.0f, 1.0f);
                plugin.bossBars().announce(text, 3);
            }
            case CAPTURED -> {
                String text = plugin.messages().get("captured",
                        "point", point.name(), "side", name, "color", color);
                plugin.broadcast().chat(text);
                plugin.broadcast().actionBar(text);
                plugin.broadcast().sound(cfg.soundCapture, 1.0f, 1.0f);
                plugin.bossBars().announce(text, 3);
                creditCapture(point, actor);
            }
            case SECURED -> plugin.broadcast().chat(plugin.messages().get("secured",
                    "point", point.name(), "side", name, "color", color));
            default -> {
                // 不需要处理
            }
        }
    }

    private void creditCapture(CapturePoint point, Side side) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.game().sideOf(player) != side) {
                continue;
            }
            Location location = player.getLocation();
            if (location.getWorld() != null
                    && point.region().contains(location.getWorld().getName(), location.getX(), location.getY(), location.getZ())) {
                plugin.stats().recordCapture(player);
            }
        }
    }

    /** 据点状态文本（用于指令输出）。 */
    public String describe(CapturePoint point) {
        BxConfig cfg = plugin.config();
        String owner = point.owner() == null ? "§7无主" : cfg.teamColor(point.owner()) + cfg.teamName(point.owner());
        return point.name() + " " + owner + " " + point.progressPercent() + "% "
                + Text.progressBar(point.progress(), 10);
    }
}
