package com.northland.battledxuanjian.game;

/** 战局阶段。 */
public enum GamePhase {
    /** 未开局，也没有等待中的玩家。 */
    IDLE("空闲"),
    /** 等待玩家加入。 */
    WAITING("等待中"),
    /** 开局倒计时。 */
    COUNTDOWN("倒计时"),
    /** 战斗进行中。 */
    RUNNING("进行中"),
    /** 结算与重置中。 */
    ENDING("结算中");

    private final String display;

    GamePhase(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
