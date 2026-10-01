package com.northland.battledxuanjian.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 文本工具验收（需求 §11 侧边栏与消息渲染）。 */
class TextTest {

    @Test
    @DisplayName("时间格式化为 mm:ss")
    void clock() {
        assertEquals("00:00", Text.clock(0));
        assertEquals("00:09", Text.clock(9));
        assertEquals("05:00", Text.clock(300));
        assertEquals("10:05", Text.clock(605));
        assertEquals("--:--", Text.clock(-1));
    }

    @Test
    @DisplayName("占位符渲染")
    void render() {
        assertEquals("据点 A: C军 100%",
                Text.render("据点 {point}: {side} {progress}%", "point", "A", "side", "C军", "progress", 100));
        assertEquals("据点 {missing}", Text.render("据点 {missing}", "point", "A"));
        assertEquals("", Text.render(null));
        assertEquals("空值: ", Text.render("空值: {value}", "value", null));
    }

    @Test
    @DisplayName("Map 渲染")
    void renderMap() {
        assertEquals("A 属于 C",
                Text.renderMap("{point} 属于 {side}", Map.of("point", "A", "side", "C")));
    }

    @Test
    @DisplayName("进度条长度与填充")
    void progressBar() {
        assertEquals(10, Text.progressBar(0, 10).length());
        assertEquals("░░░░░░░░░░", Text.progressBar(0, 10));
        assertEquals("▉▉▉▉▉▉▉▉▉▉", Text.progressBar(100, 10));
        assertEquals("▉▉▉▉▉░░░░░", Text.progressBar(50, 10));
        assertTrue(Text.progressBar(120, 10).chars().allMatch(c -> c == '▉'), "超过 100% 应被裁剪");
    }

    @Test
    @DisplayName("数字压缩")
    void compact() {
        assertEquals("999", Text.compact(999));
        assertEquals("1.5k", Text.compact(1500));
    }
}
