package com.northland.battledxuanjian.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 阵营分配算法验收（需求 §5.3：两队人数差 ≤ 1，优先尊重预选）。 */
class TeamBalancerTest {

    private static List<UUID> players(int count) {
        List<UUID> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(UUID.randomUUID());
        }
        return list;
    }

    @Test
    @DisplayName("无预选时偶数人数均分，奇数允许差 1")
    void balancedWithoutPreferences() {
        for (int count = 0; count <= 11; count++) {
            TeamBalancer.Result result = TeamBalancer.balance(players(count), Map.of(), new Random(count));
            assertTrue(result.isBalanced(), "人数 " + count + " 分配不平衡: " + result.countC() + "/" + result.countM());
            assertEquals(count, result.countC() + result.countM(), "分配后人数总和应保持不变");
        }
    }

    @Test
    @DisplayName("预选人数均衡时完全尊重玩家选择")
    void respectsPreferencesWhenBalanced() {
        List<UUID> roster = players(6);
        Map<UUID, Side> preferences = new HashMap<>();
        preferences.put(roster.get(0), Side.C);
        preferences.put(roster.get(1), Side.C);
        preferences.put(roster.get(2), Side.C);
        preferences.put(roster.get(3), Side.M);
        preferences.put(roster.get(4), Side.M);
        preferences.put(roster.get(5), Side.M);

        TeamBalancer.Result result = TeamBalancer.balance(roster, preferences, new Random(1));
        assertEquals(0, result.overridden(), "均衡预选不应被调整");
        for (UUID id : List.of(roster.get(0), roster.get(1), roster.get(2))) {
            assertEquals(Side.C, result.sides().get(id));
        }
        for (UUID id : List.of(roster.get(3), roster.get(4), roster.get(5))) {
            assertEquals(Side.M, result.sides().get(id));
        }
    }

    @Test
    @DisplayName("全员预选同一阵营时强制平衡，人数差仍 ≤ 1")
    void overridesSkewedPreferences() {
        List<UUID> roster = players(7);
        Map<UUID, Side> preferences = new HashMap<>();
        for (UUID id : roster) {
            preferences.put(id, Side.C);
        }
        TeamBalancer.Result result = TeamBalancer.balance(roster, preferences, new Random(3));
        assertTrue(result.isBalanced(), "强制平衡失败: " + result.countC() + "/" + result.countM());
        assertTrue(result.overridden() > 0, "应记录被调整的玩家数");
        assertEquals(7, result.countC() + result.countM());
    }

    @Test
    @DisplayName("未预选玩家补齐人数较少的一方")
    void undecidedFillSmallerSide() {
        List<UUID> roster = players(8);
        Map<UUID, Side> preferences = new HashMap<>();
        preferences.put(roster.get(0), Side.C);
        preferences.put(roster.get(1), Side.C);
        preferences.put(roster.get(2), Side.C);
        preferences.put(roster.get(3), Side.C);

        TeamBalancer.Result result = TeamBalancer.balance(roster, preferences, new Random(5));
        assertTrue(result.isBalanced(), "补齐后应平衡: " + result.countC() + "/" + result.countM());
        assertEquals(4, result.countC(), "4 人预选 C 军且总人数 8，均衡后 C 军应为 4 人");
        assertEquals(4, result.countM(), "其余 4 人应补齐 M 军");
    }

    @Test
    @DisplayName("中途加入选择人数较少的一方")
    void pickSmallerSide() {
        assertEquals(Side.C, TeamBalancer.pickSmallerSide(1, 3, new Random(1)));
        assertEquals(Side.M, TeamBalancer.pickSmallerSide(4, 2, new Random(1)));
    }
}
