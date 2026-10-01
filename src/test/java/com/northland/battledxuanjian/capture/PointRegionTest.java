package com.northland.battledxuanjian.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 据点选区验收（需求 §7.1）。 */
class PointRegionTest {

    @Test
    @DisplayName("构造时自动归一化 min/max")
    void normalizesCorners() {
        PointRegion region = new PointRegion("world", 10, 70, 10, 0, 60, 0);
        assertEquals(0, region.minX());
        assertEquals(10, region.maxX());
        assertEquals(60, region.minY());
        assertEquals(70, region.maxY());
    }

    @Test
    @DisplayName("包含判定与体积计算正确")
    void containsAndVolume() {
        PointRegion region = new PointRegion("world", 0, 64, 0, 4, 66, 4);
        assertTrue(region.contains("world", 0.0, 64.0, 0.0));
        assertTrue(region.contains("world", 4.99, 66.99, 4.99));
        assertFalse(region.contains("world", 5.5, 64.0, 0.0));
        assertFalse(region.contains("world", 0.0, 63.0, 0.0));
        assertFalse(region.contains("other", 0.0, 64.0, 0.0), "不同世界不应包含");
        assertFalse(region.contains(null, 0.0, 64.0, 0.0));
        assertEquals(5L * 3L * 5L, region.volume());
        assertEquals("5x3x5", region.sizeLabel());
    }

    @Test
    @DisplayName("方块级包含判定")
    void containsBlock() {
        PointRegion region = new PointRegion("world", 0, 64, 0, 4, 66, 4);
        assertTrue(region.containsBlock("world", 4, 66, 4));
        assertFalse(region.containsBlock("world", 5, 66, 4));
    }
}
