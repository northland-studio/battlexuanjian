package com.northland.battledxuanjian.hook;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.capture.CapturePoint;
import com.northland.battledxuanjian.game.Side;
import com.northland.battledxuanjian.util.Text;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI 变量扩展（需求 §13）。
 *
 * <p>提供：{@code %bx_reinforcements%}、{@code %bx_time%}、{@code %bx_phase%}、
 * {@code %bx_team%}、{@code %bx_points_c%}、{@code %bx_point_A%} 等。</p>
 */
public final class BxPlaceholderExpansion extends PlaceholderExpansion {

    private final BattledXuanjianPlugin plugin;

    public BxPlaceholderExpansion(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "bx";
    }

    @Override
    public String getAuthor() {
        return "Northland Studio";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return null;
        }
        String key = params.toLowerCase(Locale.ROOT);
        int time = plugin.game().timeRemaining();
        switch (key) {
            case "reinforcements":
                return plugin.game().pool().display();
            case "reinforcements_raw":
                return Integer.toString(plugin.game().pool().remaining());
            case "time":
                return time < 0 ? "∞" : Text.clock(time);
            case "time_seconds":
                return Integer.toString(Math.max(0, time));
            case "phase":
                return plugin.game().phase().display();
            case "phase_id":
                return plugin.game().phase().name();
            case "participants", "players":
                return Integer.toString(plugin.game().participants().size());
            case "team":
                return teamName(player, false);
            case "team_id":
                return teamName(player, true);
            case "points_total":
                return Integer.toString(plugin.points().size());
            case "points_c":
                return Integer.toString(plugin.points().countOwnedBy(Side.C));
            case "points_m":
                return Integer.toString(plugin.points().countOwnedBy(Side.M));
            case "winner":
                return plugin.game().winner() == null ? "-" : plugin.config().teamName(plugin.game().winner());
            default:
                break;
        }
        if (key.startsWith("point_")) {
            String name = params.substring("point_".length());
            CapturePoint point = plugin.points().get(name);
            if (point == null) {
                return null;
            }
            if (key.endsWith("_progress")) {
                return Integer.toString(point.progressPercent());
            }
            return point.owner() == null ? "无主" : plugin.config().teamName(point.owner());
        }
        return null;
    }

    private String teamName(OfflinePlayer player, boolean idOnly) {
        if (player == null) {
            return "-";
        }
        Side side = plugin.game().sideOf(player.getUniqueId());
        if (side == null) {
            return "-";
        }
        return idOnly ? side.id() : plugin.config().teamName(side);
    }
}
