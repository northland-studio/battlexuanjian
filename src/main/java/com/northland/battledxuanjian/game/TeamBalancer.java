package com.northland.battledxuanjian.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 阵营分配算法（纯逻辑，便于单元测试）。
 *
 * <p>规则（对应需求 §5.3）：</p>
 * <ol>
 *   <li>优先尊重玩家预选阵营；</li>
 *   <li>保证两队人数差 ≤ 1（偶数均分，奇数允许差 1）；</li>
 *   <li>未预选玩家随机均匀分配；</li>
 *   <li>预选人数严重失衡时，多出的玩家会被平衡调整（记入 {@link Result#overridden()}）。</li>
 * </ol>
 */
public final class TeamBalancer {

    private TeamBalancer() {
    }

    /**
     * 分配结果。
     *
     * @param sides      玩家 → 阵营
     * @param countC     C 军人数
     * @param countM     M 军人数
     * @param overridden 因平衡而被迫改变预选的玩家数量
     */
    public record Result(Map<UUID, Side> sides, int countC, int countM, int overridden) {

        public boolean isBalanced() {
            return Math.abs(countC - countM) <= 1;
        }
    }

    public static Result balance(List<UUID> players, Map<UUID, Side> preferences, Random random) {
        List<UUID> preferC = new ArrayList<>();
        List<UUID> preferM = new ArrayList<>();
        List<UUID> undecided = new ArrayList<>();

        for (UUID id : players) {
            Side preferred = preferences == null ? null : preferences.get(id);
            if (preferred == Side.C) {
                preferC.add(id);
            } else if (preferred == Side.M) {
                preferM.add(id);
            } else {
                undecided.add(id);
            }
        }

        // 打乱未预选列表，保证随机均匀分配（随机源可注入，便于测试复现）
        if (random != null) {
            java.util.Collections.shuffle(undecided, random);
        }

        int overridden = 0;
        while (preferC.size() - preferM.size() > 1 && !preferC.isEmpty()) {
            preferM.add(preferC.remove(preferC.size() - 1));
            overridden++;
        }
        while (preferM.size() - preferC.size() > 1 && !preferM.isEmpty()) {
            preferC.add(preferM.remove(preferM.size() - 1));
            overridden++;
        }

        for (UUID id : undecided) {
            if (preferC.size() <= preferM.size()) {
                preferC.add(id);
            } else {
                preferM.add(id);
            }
        }

        Map<UUID, Side> sides = new LinkedHashMap<>();
        for (UUID id : preferC) {
            sides.put(id, Side.C);
        }
        for (UUID id : preferM) {
            sides.put(id, Side.M);
        }
        return new Result(sides, preferC.size(), preferM.size(), overridden);
    }

    /** 中途加入时选择人数较少的一方（人数相同时按随机）。 */
    public static Side pickSmallerSide(int countC, int countM, Random random) {
        if (countC < countM) {
            return Side.C;
        }
        if (countM < countC) {
            return Side.M;
        }
        return random != null && random.nextBoolean() ? Side.M : Side.C;
    }

    /** 统计某阵营人数。 */
    public static Map<Side, Integer> counts(Map<UUID, Side> sides) {
        Map<Side, Integer> counts = new HashMap<>();
        counts.put(Side.C, 0);
        counts.put(Side.M, 0);
        for (Side side : sides.values()) {
            if (side != null) {
                counts.merge(side, 1, Integer::sum);
            }
        }
        return counts;
    }
}
