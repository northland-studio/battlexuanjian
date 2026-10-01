package com.northland.battledxuanjian.config;

import com.northland.battledxuanjian.util.Text;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 消息管理：内置默认值 + {@code config.yml messages} 覆盖。
 *
 * <p>所有消息都支持 {@code &} 颜色代码与 {@code {占位符}}。</p>
 */
public final class Messages {

    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("prefix", "&e[战地] &r");
        DEFAULTS.put("no-permission", "&c你没有权限执行该指令。");
        DEFAULTS.put("player-only", "&c该指令只能由玩家执行。");
        DEFAULTS.put("usage", "&7用法: &f{usage}");
        DEFAULTS.put("unknown-subcommand", "&c未知的子指令: &f{input}");
        DEFAULTS.put("reloaded", "&a配置已重载。");
        DEFAULTS.put("join-hint", "&7使用 &f/bx join &7加入战局。");
        DEFAULTS.put("joined", "&a你已加入战局等待队列。");
        DEFAULTS.put("already-joined", "&e你已经在队列中。");
        DEFAULTS.put("not-joined", "&e你还没有加入战局。");
        DEFAULTS.put("left", "&e你已离开战局。");
        DEFAULTS.put("mid-join-denied", "&c本局已开始，且未开放在中途加入。");
        DEFAULTS.put("team-joined", "&a你预选了 {color}{side}军&a。");
        DEFAULTS.put("team-assigned", "&a你被分配到 {color}{side}军&a。");
        DEFAULTS.put("team-spectate", "&7你已进入旁观模式。");
        DEFAULTS.put("not-enough-players", "&c人数不足：需要至少 {min} 名玩家（当前 {current}）。");
        DEFAULTS.put("game-running", "&c当前已有一局正在进行。");
        DEFAULTS.put("game-idle", "&e当前没有进行中的对局。");
        DEFAULTS.put("no-points", "&c还没有配置任何据点，请先用 /bx pos1、/bx pos2 与 /bx point create 创建据点。");
        DEFAULTS.put("game-stopped", "&e本局已被管理员强制结束。");
        DEFAULTS.put("game-started", "&6[战地] &f战斗开始！");
        DEFAULTS.put("countdown-chat", "&e[战地] &f距离战斗开始还有 &e{seconds} &f秒。");
        DEFAULTS.put("countdown-title", "§e{seconds}");
        DEFAULTS.put("countdown-subtitle", "§f战斗即将开始");
        DEFAULTS.put("countdown-cancelled", "&c人数不足，倒计时已取消。");
        DEFAULTS.put("lobby-set", "&a等待区大厅坐标已设置。");
        DEFAULTS.put("spawn-set", "&a{color}{side}军 &a出发点已设置。");
        DEFAULTS.put("spawn-not-set", "&e{color}{side}军 &e还没有设置出发点。");
        DEFAULTS.put("point-created", "&a据点 &f{point} &a创建成功。");
        DEFAULTS.put("point-exists", "&c据点 &f{point} &c已存在。");
        DEFAULTS.put("point-removed", "&a据点 &f{point} &a已删除。");
        DEFAULTS.put("point-not-found", "&c找不到据点 &f{point}&c。");
        DEFAULTS.put("point-list-empty", "&7当前没有配置任何据点。");
        DEFAULTS.put("point-list-header", "&6据点列表（{count}）：");
        DEFAULTS.put("point-list-line", "&f- &e{point} &7[{world}] &7归属: {color}{owner} &7进度 {progress}%");
        DEFAULTS.put("point-name-invalid", "&c据点名称只能包含字母、数字、下划线或短横线。");
        DEFAULTS.put("point-need-selection", "&c请先用 &f/bx pos1 &c和 &f/bx pos2 &c选择长方体区域。");
        DEFAULTS.put("point-selection-world-mismatch", "&c两个选区点必须在同一个世界。");
        DEFAULTS.put("pos1-set", "&a已设置选区点 1: &f{location}");
        DEFAULTS.put("pos2-set", "&a已设置选区点 2: &f{location}");
        DEFAULTS.put("kit-need-shulker", "&c请手持潜影盒（盒内放入装备）再执行该指令。");
        DEFAULTS.put("kit-saved", "&a已保存 {color}{side}军 &a装备包。");
        DEFAULTS.put("kit-missing", "&e{color}{side}军 &e尚未配置装备包。");
        DEFAULTS.put("kit-cleared", "&a已清空所有阵营装备包。");
        DEFAULTS.put("reinforcements-set", "&a攻方兵力已设置为 &f{count}&a。");
        DEFAULTS.put("time-set", "&a时间限制已设置为 &f{minutes} &a分钟。");
        DEFAULTS.put("captured", "&e[战地] {color}{side}军 &f占领了据点 &e{point}&f！");
        DEFAULTS.put("decaptured", "&e[战地] 据点 &e{point} &f已脱离 {color}{side}军 &f掌控！");
        DEFAULTS.put("secured", "&7[战地] 据点 {point} 已被 {color}{side}军 &7重新稳固。");
        DEFAULTS.put("reinforcements-left", "&f攻方兵力剩余: &c{count}");
        DEFAULTS.put("reinforcements-depleted", "&c[战地] 攻方兵力已耗尽！");
        DEFAULTS.put("time-up", "&e[战地] 时间耗尽，守方获胜。");
        DEFAULTS.put("all-points-captured", "&e[战地] 攻方占领了全部据点！");
        DEFAULTS.put("victory", "&6[战地] {color}{side}军 &6获得胜利！&7（{reason}）");
        DEFAULTS.put("reason-reinforcements", "攻方兵力耗尽");
        DEFAULTS.put("reason-time-up", "时间耗尽");
        DEFAULTS.put("reason-all-points", "占领全部据点");
        DEFAULTS.put("reason-forced", "管理员强制结束");
        DEFAULTS.put("match-summary-header", "&6===== 本局战报 =====");
        DEFAULTS.put("match-summary-line", "&f{player} &7- 击杀 &c{kills} &7/ 死亡 &f{deaths} &7/ 伤害 &e{damage} &7/ 占点 &a{captures}");
        DEFAULTS.put("status-header", "&6===== 玄剑·战争 状态 =====");
        DEFAULTS.put("status-phase", "&f阶段: &a{phase}");
        DEFAULTS.put("status-players", "&f参战玩家: &a{count} &7(C军 {c} / M军 {m})");
        DEFAULTS.put("status-reinforcements", "&f攻方兵力: &c{count}");
        DEFAULTS.put("status-time", "&f剩余时间: &a{time}");
        DEFAULTS.put("status-points", "&f据点: &a{points}");
        DEFAULTS.put("selftest-header", "&6===== 自检开始 =====");
        DEFAULTS.put("selftest-footer", "&6===== 自检结束 &f{result}&6 =====");
        DEFAULTS.put("stats-header", "&6===== 累计统计 =====");
        DEFAULTS.put("stats-line", "&f{player} &7- 击杀 &c{kills} &7/ 死亡 &f{deaths} &7/ 占点 &a{captures}");
        DEFAULTS.put("stats-empty", "&7暂无统计数据。");
        DEFAULTS.put("stats-reset", "&a统计数据已清空。");
    }

    private final Map<String, String> values = new HashMap<>(DEFAULTS);

    public void load(FileConfiguration cfg) {
        values.clear();
        values.putAll(DEFAULTS);
        ConfigurationSection section = cfg.getConfigurationSection("messages");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String raw = section.getString(key);
            if (raw != null) {
                values.put(key, raw);
            }
        }
    }

    /** 原始模板（未转换颜色）。 */
    public String raw(String key) {
        return values.getOrDefault(key, key);
    }

    /** 渲染后的消息文本（已转换颜色）。 */
    public String get(String key, Object... pairs) {
        return Text.color(Text.render(values.getOrDefault(key, key), pairs));
    }

    /** 渲染并带上插件前缀。 */
    public String prefixed(String key, Object... pairs) {
        return Text.color(values.getOrDefault("prefix", "")) + get(key, pairs);
    }

    public void send(CommandSender sender, String key, Object... pairs) {
        sender.sendMessage(prefixed(key, pairs));
    }

    public void sendRaw(CommandSender sender, String key, Object... pairs) {
        sender.sendMessage(get(key, pairs));
    }
}
