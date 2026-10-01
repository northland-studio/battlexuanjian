package com.northland.battledxuanjian.ui;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.config.BxConfig;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * 侧边栏计分板（需求 §11）：显示时间、攻方兵力、各据点归属与占领进度。
 *
 * <p>使用主计分板上的 DUMMY 目标，条目文本通过尾部颜色代码去重，避免重复行被合并。</p>
 */
public final class SidebarService {

    private static final String OBJECTIVE_NAME = "bx_sidebar";
    private static final String UNIQUE_CHARS = "0123456789abcdef";

    private final BattledXuanjianPlugin plugin;
    private final List<String> rendered = new ArrayList<>();
    private Objective objective;

    public SidebarService(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean active() {
        return objective != null && objective.getScoreboard() != null;
    }

    /** 创建并显示侧边栏。 */
    public void show() {
        BxConfig cfg = plugin.config();
        if (!cfg.sidebarEnabled) {
            return;
        }
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Text.color(cfg.sidebarTitle));
        }
        objective.setDisplayName(Text.color(cfg.sidebarTitle));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /** 隐藏侧边栏。 */
    public void hide() {
        if (objective == null) {
            return;
        }
        Scoreboard board = objective.getScoreboard();
        try {
            objective.unregister();
        } catch (IllegalStateException ignored) {
            // 目标已被外部移除
        }
        if (board != null) {
            board.clearSlot(DisplaySlot.SIDEBAR);
            for (String entry : new ArrayList<>(rendered)) {
                board.resetScores(entry);
            }
        }
        rendered.clear();
        objective = null;
    }

    /** 刷新侧边栏内容。 */
    public void update(List<String> lines) {
        if (!plugin.config().sidebarEnabled || !plugin.config().broadcastSidebar) {
            return;
        }
        if (objective == null) {
            show();
        }
        if (objective == null) {
            return;
        }

        List<String> next = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            next.add(unique(lines.get(i), i));
        }

        Scoreboard board = objective.getScoreboard();
        Set<String> nextSet = new LinkedHashSet<>(next);
        if (board != null) {
            for (String old : new ArrayList<>(rendered)) {
                if (!nextSet.contains(old)) {
                    board.resetScores(old);
                }
            }
        }
        int score = next.size();
        for (String entry : next) {
            objective.getScore(entry).setScore(score--);
        }
        rendered.clear();
        rendered.addAll(next);
    }

    /** 用不可见颜色代码保证同一文本在多行出现时仍然唯一。 */
    private static String unique(String line, int index) {
        String text = line == null ? "" : line;
        char suffix = UNIQUE_CHARS.charAt(index % UNIQUE_CHARS.length());
        int repeat = index / UNIQUE_CHARS.length();
        StringBuilder builder = new StringBuilder(text);
        for (int i = 0; i <= repeat; i++) {
            builder.append('§').append(suffix);
        }
        return builder.toString();
    }
}
