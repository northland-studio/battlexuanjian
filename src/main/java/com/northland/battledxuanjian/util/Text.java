package com.northland.battledxuanjian.util;

import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;

/** 文本与颜色工具。 */
public final class Text {

    private Text() {
    }

    /** 转换 {@code &} 颜色代码为 {@code §}。 */
    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String strip(String input) {
        return input == null ? "" : ChatColor.stripColor(color(input));
    }

    /**
     * 使用 {@code {key}} 占位符渲染模板。
     *
     * @param template 模板
     * @param pairs    key1, value1, key2, value2 ... 形式的键值对
     */
    public static String render(String template, Object... pairs) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String key = String.valueOf(pairs[i]);
            String value = pairs[i + 1] == null ? "" : String.valueOf(pairs[i + 1]);
            result = result.replace("{" + key + "}", value);
        }
        return result;
    }

    public static String renderMap(String template, Map<String, String> values) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    /** 秒 → {@code mm:ss}。 */
    public static String clock(int seconds) {
        if (seconds < 0) {
            return "--:--";
        }
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, secs);
    }

    /** 生成进度条字符串，例如 {@code ▉▉▉▉░░}。 */
    public static String progressBar(double progress, int length) {
        int safeLength = Math.max(1, length);
        int filled = (int) Math.round(Math.max(0.0, Math.min(100.0, progress)) / 100.0 * safeLength);
        StringBuilder builder = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            builder.append(i < filled ? '▉' : '░');
        }
        return builder.toString();
    }

    /** 将数字压缩显示，例如 1500 → 1.5k。 */
    public static String compact(double value) {
        if (value < 1000) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.1fk", value / 1000.0);
    }
}
