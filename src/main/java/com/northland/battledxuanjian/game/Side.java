package com.northland.battledxuanjian.game;

import java.util.Locale;

/**
 * 战局阵营。
 *
 * <p>C 军为守方（Defender），M 军为攻方（Attacker）。阵营 id 同时用作原版 Scoreboard Team
 * 名称，因此必须保持为单字符 {@code C} / {@code M}，以便与原版 {@code /team} 指令兼容。</p>
 */
public enum Side {
    C("C", "守"),
    M("M", "攻");

    private final String id;
    private final String role;

    Side(String id, String role) {
        this.id = id;
        this.role = role;
    }

    public String id() {
        return id;
    }

    /** 攻 / 守 */
    public String role() {
        return role;
    }

    public Side opponent() {
        return this == C ? M : C;
    }

    public boolean isAttacker() {
        return this == M;
    }

    /**
     * 解析阵营参数，兼容 {@code C/M}、{@code c/m}、{@code 攻/守} 等写法。
     *
     * @return 解析出的阵营，无法识别时返回 {@code null}
     */
    public static Side byId(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "C", "守", "守方", "DEF", "DEFENDER", "DEFENCE", "DEFENSE" -> C;
            case "M", "攻", "攻方", "ATK", "ATTACK", "ATTACKER" -> M;
            default -> null;
        };
    }

    public static String ids() {
        return "C/M";
    }
}
