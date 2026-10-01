package com.northland.battledxuanjian.game;

/**
 * 攻方公共兵力池（纯逻辑，便于单元测试）。
 *
 * <p>约定：初始值小于 0 表示无限兵力。</p>
 */
public final class ReinforcementPool {

    private final boolean unlimited;
    private int remaining;

    public ReinforcementPool(int initial) {
        this.unlimited = initial < 0;
        this.remaining = Math.max(0, initial);
    }

    public boolean unlimited() {
        return unlimited;
    }

    public int remaining() {
        return unlimited ? -1 : remaining;
    }

    /** 重新设置兵力（负数表示无限）。 */
    public void set(int value) {
        if (unlimited) {
            return;
        }
        this.remaining = Math.max(0, value);
    }

    /**
     * 消耗一点兵力。
     *
     * @return {@code true} 表示消耗成功；无限兵力或已耗尽时返回 {@code false}
     */
    public boolean consume() {
        if (unlimited) {
            return false;
        }
        if (remaining <= 0) {
            return false;
        }
        remaining--;
        return true;
    }

    public boolean depleted() {
        return !unlimited && remaining <= 0;
    }

    public String display() {
        return unlimited ? "∞" : Integer.toString(remaining);
    }
}
