package com.northland.battledxuanjian.game;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.config.BxConfig;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * 队伍系统（需求 §6、§10）。
 *
 * <p>使用原版 Scoreboard Team（队伍名固定为 {@code C} / {@code M}），因此与原版
 * {@code /team} 指令天然互通；插件会监听原版指令并按配置重新同步。</p>
 */
public final class TeamManager {

    private final BattledXuanjianPlugin plugin;
    private final Set<String> createdTeams = new HashSet<>();

    public TeamManager(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    private Scoreboard board() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    /** 获取（必要时创建）阵营队伍，并应用配置。 */
    public Team teamOf(Side side) {
        Scoreboard scoreboard = board();
        Team team = scoreboard.getTeam(side.id());
        if (team == null) {
            team = scoreboard.registerNewTeam(side.id());
            createdTeams.add(side.id());
        }
        configure(team, side);
        return team;
    }

    private void configure(Team team, Side side) {
        BxConfig cfg = plugin.config();
        team.setDisplayName(cfg.teamName(side));
        team.setPrefix(cfg.teamPrefix(side));
        team.setColor(colorOf(cfg.teamColor(side)));
        team.setAllowFriendlyFire(cfg.friendlyFire);
        team.setCanSeeFriendlyInvisibles(true);
        // HIDE_FOR_OTHER_TEAMS 对应 OptionStatus.FOR_OWN_TEAM：只有同队可见头顶 ID
        team.setOption(Team.Option.NAME_TAG_VISIBILITY,
                cfg.hideEnemyNametag ? Team.OptionStatus.FOR_OWN_TEAM : Team.OptionStatus.ALWAYS);
        team.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
    }

    private static ChatColor colorOf(String legacyColor) {
        if (legacyColor == null || legacyColor.isEmpty()) {
            return ChatColor.WHITE;
        }
        ChatColor color = ChatColor.getByChar(legacyColor.charAt(legacyColor.length() - 1));
        return color == null ? ChatColor.WHITE : color;
    }

    /** 把玩家放入正确队伍（自动从另一队移除）。 */
    public void applyPlayer(Player player, Side side) {
        if (player == null || side == null) {
            return;
        }
        Team own = teamOf(side);
        Team other = teamOf(side.opponent());
        if (other.hasEntry(player.getName())) {
            other.removeEntry(player.getName());
        }
        if (!own.hasEntry(player.getName())) {
            own.addEntry(player.getName());
        }
    }

    /** 把玩家从两个阵营队伍中移除。 */
    public void removePlayer(Player player) {
        if (player == null) {
            return;
        }
        removeEntry(player.getName());
    }

    public void removeEntry(String entry) {
        if (entry == null) {
            return;
        }
        for (Side side : Side.values()) {
            Team team = board().getTeam(side.id());
            if (team != null && team.hasEntry(entry)) {
                team.removeEntry(entry);
            }
        }
    }

    /** 重新应用全部队伍配置与成员（用于 {@code /bx reload} 或原版 {@code /team} 干扰后同步）。 */
    public void refreshAll() {
        for (Side side : Side.values()) {
            teamOf(side);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Side side = plugin.game().sideOf(player);
            if (side != null) {
                applyPlayer(player, side);
            }
        }
    }

    /** 清空队伍成员；插件自己创建的队伍会被注销。 */
    public void reset() {
        for (Side side : Side.values()) {
            Team team = board().getTeam(side.id());
            if (team == null) {
                continue;
            }
            for (String entry : new HashSet<>(team.getEntries())) {
                team.removeEntry(entry);
            }
            if (createdTeams.contains(side.id())) {
                try {
                    team.unregister();
                } catch (IllegalStateException ignored) {
                    // 已被外部注销
                }
            }
        }
        createdTeams.clear();
    }

    public String prefixOf(Side side) {
        return plugin.config().teamPrefix(side);
    }

    public static String displayName(BattledXuanjianPlugin plugin, Side side) {
        return side == null ? "" : plugin.config().teamName(side);
    }
}
