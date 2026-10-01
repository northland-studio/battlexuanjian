package com.northland.battledxuanjian.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.northland.battledxuanjian.game.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 占领状态机验收（需求 §7、§14.2）：
 * 敌对玩家进入后 15 秒脱离、再 15 秒占领；人数影响速度；双方在场冻结。
 */
class CapturePointTest {

    private static final double BASE = 15.0;
    private static final double SPEED = 0.5;
    private static final double TICK = 0.5;

    private static CapturePoint point(Side owner) {
        return new CapturePoint("A", new PointRegion("world", 0, 60, 0, 9, 70, 9), owner);
    }

    /** 持续推进直到目标事件，返回耗时（秒）；未发生返回 -1。 */
    private static double until(CapturePoint point, int c, int m, CapturePoint.Event target) {
        double elapsed = 0;
        for (int i = 0; i < 2000; i++) {
            CapturePoint.Result result = point.tick(c, m, BASE, SPEED, TICK);
            elapsed += TICK;
            if (result.event() == target) {
                return elapsed;
            }
        }
        return -1;
    }

    @Test
    @DisplayName("单人：15 秒脱离，再 15 秒被占领")
    void singlePlayerTimings() {
        CapturePoint point = point(Side.C);
        assertEquals(100, point.progressPercent());

        double decapture = until(point, 0, 1, CapturePoint.Event.DECAPTURED);
        assertTrue(Math.abs(decapture - BASE) < 0.6, "单人脱离耗时应为 15 秒，实际 " + decapture);
        assertEquals(null, point.owner(), "脱离后应为无主");
        assertEquals(0, point.progressPercent());

        double capture = until(point, 0, 1, CapturePoint.Event.CAPTURED);
        assertTrue(Math.abs(capture - BASE) < 0.6, "单人占领耗时应为 15 秒，实际 " + capture);
        assertEquals(Side.M, point.owner(), "占领后归属 M 军");
        assertEquals(100, point.progressPercent());
    }

    @Test
    @DisplayName("人数影响速度：2 人 10 秒、3 人约 7.5 秒完成脱离")
    void speedScalesWithPlayerCount() {
        CapturePoint two = point(Side.C);
        double twoSeconds = until(two, 0, 2, CapturePoint.Event.DECAPTURED);
        assertTrue(twoSeconds < BASE, "2 人应比单人更快");
        assertTrue(Math.abs(twoSeconds - 10.0) < 0.6, "2 人脱离耗时应约 10 秒，实际 " + twoSeconds);

        CapturePoint three = point(Side.C);
        double threeSeconds = until(three, 0, 3, CapturePoint.Event.DECAPTURED);
        assertTrue(threeSeconds < twoSeconds, "3 人应比 2 人更快");
        assertTrue(Math.abs(threeSeconds - 7.5) < 0.6, "3 人脱离耗时应约 7.5 秒，实际 " + threeSeconds);
    }

    @Test
    @DisplayName("双方同时在场时进度冻结")
    void freezeWhenContested() {
        CapturePoint point = point(Side.C);
        point.tick(0, 1, BASE, SPEED, 5.0);
        int frozen = point.progressPercent();
        for (int i = 0; i < 40; i++) {
            CapturePoint.Result result = point.tick(2, 2, BASE, SPEED, TICK);
            assertEquals(CapturePoint.Event.NONE, result.event());
        }
        assertEquals(frozen, point.progressPercent(), "双方在场进度不得变化");
        assertEquals(Side.C, point.owner());
    }

    @Test
    @DisplayName("无人时进度保持，控制方回归后重新稳固")
    void securedWhenOwnerReturns() {
        CapturePoint point = point(Side.C);
        point.tick(0, 1, BASE, SPEED, 5.0);
        int midway = point.progressPercent();
        assertTrue(midway < 100 && midway > 0, "中途进度应在 0~100 之间，实际 " + midway);

        // 无人时段：进度不变
        point.tick(0, 0, BASE, SPEED, 10.0);
        assertEquals(midway, point.progressPercent());

        double secured = until(point, 1, 0, CapturePoint.Event.SECURED);
        assertTrue(secured > 0, "控制方回归后应触发重新稳固");
        assertEquals(100, point.progressPercent());
        assertEquals(Side.C, point.owner());
    }

    @Test
    @DisplayName("反向占领：C 军可夺回 M 军据点")
    void reverseCapture() {
        CapturePoint point = point(Side.M);
        double decapture = until(point, 1, 0, CapturePoint.Event.DECAPTURED);
        assertTrue(decapture > 0);
        assertEquals(null, point.owner());

        double capture = until(point, 1, 0, CapturePoint.Event.CAPTURED);
        assertTrue(capture > 0);
        assertEquals(Side.C, point.owner());
    }

    @Test
    @DisplayName("进度条百分比与进度值一致")
    void progressPercentConsistent() {
        CapturePoint point = point(Side.C);
        point.tick(0, 1, BASE, SPEED, 5.0);
        assertEquals((int) Math.round(point.progress()), point.progressPercent());
    }
}
