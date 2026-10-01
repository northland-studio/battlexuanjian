package com.northland.battledxuanjian.capture;

import com.northland.battledxuanjian.game.Side;

/**
 * 据点占领状态机（纯逻辑，便于单元测试）。
 *
 * <p>状态语义（对应需求 §7.2）：</p>
 * <ul>
 *   <li>{@code owner != null && progress == 100}：该阵营完全控制；</li>
 *   <li>{@code owner == null}：无主（progress 从 0 开始被争夺方抬升）；</li>
 *   <li>只有敌方在场：脱离阶段，progress 从 100 下降，归零时变为无主；</li>
 *   <li>双方同时在场或无人在场：进度冻结；</li>
 *   <li>控制方重新回到据点且进度未满：进度回升至 100（重新稳固）。</li>
 * </ul>
 *
 * <p>速度公式：{@code 速率 = 100 / 基础时间 × (1 + (在场人数 - 1) × speed-per-player)}，
 * 即单人时正好是配置的基础时间，人数越多越快（需求 §7.2：基础时间 15 秒为一人时的耗时）。</p>
 */
public final class CapturePoint {

    /** 单次 tick 产生的事件。 */
    public enum Event {
        /** 无变化（推进中或冻结）。 */
        NONE,
        /** 脱离完成，据点变为无主。 */
        DECAPTURED,
        /** 占领完成，据点归属争夺方。 */
        CAPTURED,
        /** 控制方重新稳固（进度从中间值回到 100）。 */
        SECURED
    }

    /**
     * tick 结果。
     *
     * @param event         事件类型
     * @param previousOwner 事件发生前的归属（DECAPTURED 时有效）
     */
    public record Result(Event event, Side previousOwner) {

        public static final Result NONE = new Result(Event.NONE, null);
    }

    private final String name;
    private final PointRegion region;

    private Side owner;
    private double progress;
    private Side contestant;

    public CapturePoint(String name, PointRegion region, Side owner) {
        this.name = name;
        this.region = region;
        setOwner(owner);
    }

    public String name() {
        return name;
    }

    public PointRegion region() {
        return region;
    }

    public Side owner() {
        return owner;
    }

    public double progress() {
        return progress;
    }

    public int progressPercent() {
        return (int) Math.round(progress);
    }

    /** 最近一次推进进度的阵营（用于 UI 显示争夺方），可能为 {@code null}。 */
    public Side contestant() {
        return contestant;
    }

    public boolean neutral() {
        return owner == null;
    }

    public boolean ownedBy(Side side) {
        return owner == side;
    }

    public void setOwner(Side newOwner) {
        this.owner = newOwner;
        this.progress = newOwner == null ? 0.0 : 100.0;
        this.contestant = null;
    }

    /**
     * 推进一次占领逻辑。
     *
     * @param cCount         据点内的 C 军人数
     * @param mCount         据点内的 M 军人数
     * @param baseSeconds    单人基础占领/脱离时间（秒）
     * @param speedPerPlayer 每多一名玩家增加的速率比例
     * @param seconds        本次推进代表的时间（秒）
     */
    public Result tick(int cCount, int mCount, double baseSeconds, double speedPerPlayer, double seconds) {
        Side present;
        int count;
        if (cCount > 0 && mCount == 0) {
            present = Side.C;
            count = cCount;
        } else if (mCount > 0 && cCount == 0) {
            present = Side.M;
            count = mCount;
        } else {
            // 双方同时在场（或无人）：进度冻结
            return Result.NONE;
        }

        double multiplier = 1.0 + Math.max(0, count - 1) * speedPerPlayer;
        double rate = 100.0 / Math.max(0.01, baseSeconds) * multiplier;
        double delta = rate * Math.max(0.0, seconds);
        contestant = present;

        if (owner == null) {
            progress = Math.min(100.0, progress + delta);
            if (progress >= 100.0) {
                progress = 100.0;
                owner = present;
                return new Result(Event.CAPTURED, null);
            }
            return Result.NONE;
        }

        if (owner == present) {
            if (progress < 100.0) {
                progress = Math.min(100.0, progress + delta);
                if (progress >= 100.0) {
                    progress = 100.0;
                    return new Result(Event.SECURED, present);
                }
            }
            return Result.NONE;
        }

        progress -= delta;
        if (progress <= 0.0) {
            progress = 0.0;
            Side previous = owner;
            owner = null;
            contestant = present;
            return new Result(Event.DECAPTURED, previous);
        }
        return Result.NONE;
    }
}
