package com.northland.battledxuanjian.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 兵力池验收（需求 §8、§14.3）。 */
class ReinforcementPoolTest {

    @Test
    @DisplayName("攻方兵力 50 点，死亡重生扣 1")
    void consumeDecrements() {
        ReinforcementPool pool = new ReinforcementPool(50);
        assertEquals(50, pool.remaining());
        assertTrue(pool.consume());
        assertEquals(49, pool.remaining());
        assertFalse(pool.depleted());
    }

    @Test
    @DisplayName("兵力归零判定耗尽")
    void depletedAtZero() {
        ReinforcementPool pool = new ReinforcementPool(2);
        assertTrue(pool.consume());
        assertTrue(pool.consume());
        assertTrue(pool.depleted());
        assertFalse(pool.consume(), "耗尽后不应继续消耗");
        assertEquals(0, pool.remaining());
    }

    @Test
    @DisplayName("负数表示无限兵力（守方 C 军）")
    void unlimited() {
        ReinforcementPool pool = new ReinforcementPool(-1);
        assertTrue(pool.unlimited());
        assertFalse(pool.depleted());
        assertFalse(pool.consume(), "无限兵力无需消耗");
        assertEquals("∞", pool.display());
    }

    @Test
    @DisplayName("set 可动态调整兵力，仅对有限兵力生效")
    void setValue() {
        ReinforcementPool pool = new ReinforcementPool(10);
        pool.set(25);
        assertEquals(25, pool.remaining());
        pool.set(0);
        assertTrue(pool.depleted());

        ReinforcementPool unlimited = new ReinforcementPool(-1);
        unlimited.set(5);
        assertTrue(unlimited.unlimited(), "无限兵力不应被 set 改变");
    }
}
