package com.northland.battledxuanjian.game;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * 本局与累计统计（需求 §5.5、§12.2、§13）。
 *
 * <p>本局数据仅内存保存；累计数据可持久化到 {@code stats.yml}。</p>
 */
public final class StatsManager {

    private final BattledXuanjianPlugin plugin;
    private final Map<UUID, MatchEntry> match = new LinkedHashMap<>();
    private final Map<UUID, LifetimeEntry> lifetime = new HashMap<>();

    public StatsManager(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    /** 单局数据。 */
    public static final class MatchEntry {
        public final UUID id;
        public final String name;
        public int kills;
        public int deaths;
        public int captures;
        public double damage;

        MatchEntry(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** 累计数据。 */
    public static final class LifetimeEntry {
        public String name = "";
        public int kills;
        public int deaths;
        public int captures;
        public int matches;
    }

    private MatchEntry entry(Player player) {
        return match.computeIfAbsent(player.getUniqueId(), id -> new MatchEntry(id, player.getName()));
    }

    public void beginMatch() {
        match.clear();
    }

    public void recordKill(Player killer) {
        if (killer != null) {
            entry(killer).kills++;
        }
    }

    public void recordDeath(Player victim) {
        if (victim != null) {
            entry(victim).deaths++;
        }
    }

    public void recordDamage(Player attacker, double damage) {
        if (attacker != null && damage > 0) {
            entry(attacker).damage += damage;
        }
    }

    public void recordCapture(Player player) {
        if (player != null) {
            entry(player).captures++;
        }
    }

    /** 结束本局：合并进累计数据并持久化。 */
    public void endMatch() {
        for (MatchEntry entry : match.values()) {
            LifetimeEntry life = lifetime.computeIfAbsent(entry.id, id -> new LifetimeEntry());
            life.name = entry.name;
            life.kills += entry.kills;
            life.deaths += entry.deaths;
            life.captures += entry.captures;
            life.matches++;
        }
        if (plugin.config().statsPersist) {
            save();
        }
    }

    /** 本局战报（按击杀数降序）。 */
    public List<String> summaryLines() {
        List<MatchEntry> entries = new ArrayList<>(match.values());
        entries.sort(Comparator.comparingInt((MatchEntry e) -> e.kills).reversed());
        List<String> lines = new ArrayList<>();
        for (MatchEntry entry : entries) {
            lines.add(plugin.messages().get("match-summary-line",
                    "player", entry.name,
                    "kills", entry.kills,
                    "deaths", entry.deaths,
                    "damage", (int) Math.round(entry.damage),
                    "captures", entry.captures));
        }
        return lines;
    }

    public List<String> lifetimeLines() {
        List<LifetimeEntry> entries = new ArrayList<>(lifetime.values());
        entries.sort(Comparator.comparingInt((LifetimeEntry e) -> e.kills).reversed());
        List<String> lines = new ArrayList<>();
        for (LifetimeEntry entry : entries) {
            lines.add(plugin.messages().get("stats-line",
                    "player", entry.name,
                    "kills", entry.kills,
                    "deaths", entry.deaths,
                    "captures", entry.captures,
                    "matches", entry.matches));
        }
        return lines;
    }

    public void resetLifetime() {
        lifetime.clear();
        if (plugin.config().statsPersist) {
            save();
        }
    }

    private File file() {
        return new File(plugin.getDataFolder(), plugin.config().statsFile);
    }

    public void load() {
        lifetime.clear();
        File file = file();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            LifetimeEntry entry = new LifetimeEntry();
            entry.name = yaml.getString(key + ".name", key);
            entry.kills = yaml.getInt(key + ".kills");
            entry.deaths = yaml.getInt(key + ".deaths");
            entry.captures = yaml.getInt(key + ".captures");
            entry.matches = yaml.getInt(key + ".matches");
            try {
                lifetime.put(UUID.fromString(key), entry);
            } catch (IllegalArgumentException ignored) {
                // 旧数据可能以玩家名作为键，忽略
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, LifetimeEntry> entry : lifetime.entrySet()) {
            String path = entry.getKey().toString();
            LifetimeEntry value = entry.getValue();
            yaml.set(path + ".name", value.name);
            yaml.set(path + ".kills", value.kills);
            yaml.set(path + ".deaths", value.deaths);
            yaml.set(path + ".captures", value.captures);
            yaml.set(path + ".matches", value.matches);
        }
        try {
            File file = file();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录: " + parent.getAbsolutePath());
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存统计数据失败: " + exception.getMessage());
        }
    }
}
