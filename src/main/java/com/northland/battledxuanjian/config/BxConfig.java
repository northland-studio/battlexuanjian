package com.northland.battledxuanjian.config;

import com.northland.battledxuanjian.game.Side;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 配置快照。每次 {@code /bx reload} 都会重新解析生成新实例，其余组件通过
 * {@code plugin.config()} 读取，天然支持热重载。
 */
public final class BxConfig {

    // ---------------- game ----------------
    public final int minPlayers;
    public final int countdownSeconds;
    public final int timeLimitSeconds;
    public final boolean allowMidJoin;
    public final boolean autoBalance;
    public final boolean autoNextRound;
    private final String lobbyWorld;
    private final double lobbyX;
    private final double lobbyY;
    private final double lobbyZ;
    private final float lobbyYaw;
    private final float lobbyPitch;

    // ---------------- reinforcements ----------------
    public final int reinforcementsM;
    public final int reinforcementsC;

    // ---------------- respawn ----------------
    public final int respawnDelaySeconds;
    public final double respawnMinDistance;
    public final int safeSearchAttempts;

    // ---------------- capture ----------------
    public final double captureBaseSeconds;
    public final double captureSpeedPerPlayer;
    public final int captureIntervalTicks;
    public final boolean bossBarVisibleToAll;

    // ---------------- teams ----------------
    private final String[] teamName = new String[2];
    private final String[] teamColor = new String[2];
    private final String[] teamPrefix = new String[2];
    public final boolean hideEnemyNametag;
    public final boolean hideEnemyTab;
    public final String tabMask;
    public final String chatMode;
    public final boolean friendlyFire;
    public final boolean syncVanillaTeamCommand;

    // ---------------- kits ----------------
    public final boolean preventEnemyPickup;
    public final boolean preventDrop;
    public final boolean clearEffectsOnRespawn;

    // ---------------- sidebar ----------------
    public final boolean sidebarEnabled;
    public final String sidebarTitle;
    public final String lineTime;
    public final String lineReinforcements;
    public final String linePointOwned;
    public final String linePointNeutral;
    public final String linePointContesting;
    public final String lineFooter;

    // ---------------- broadcast ----------------
    public final boolean broadcastChat;
    public final boolean broadcastTitle;
    public final boolean broadcastActionBar;
    public final boolean broadcastSound;
    public final boolean broadcastBossBar;
    public final boolean broadcastSidebar;
    public final String soundCountdown;
    public final String soundStart;
    public final String soundCapture;
    public final String soundDecapture;
    public final String soundVictory;
    public final String soundDefeat;

    // ---------------- storage ----------------
    public final boolean statsPersist;
    public final String statsFile;
    public final String pointsFile;
    public final String itemsFile;

    private BxConfig(FileConfiguration cfg) {
        minPlayers = Math.max(1, cfg.getInt("game.min-players", 4));
        countdownSeconds = Math.max(0, cfg.getInt("game.countdown", 30));
        timeLimitSeconds = Math.max(0, cfg.getInt("game.time-limit", 0)) * 60;
        allowMidJoin = cfg.getBoolean("game.allow-mid-join", true);
        autoBalance = cfg.getBoolean("game.auto-balance", true);
        autoNextRound = cfg.getBoolean("game.auto-next-round", false);
        lobbyWorld = cfg.getString("game.lobby.world", "");
        lobbyX = cfg.getDouble("game.lobby.x", 0.0);
        lobbyY = cfg.getDouble("game.lobby.y", 64.0);
        lobbyZ = cfg.getDouble("game.lobby.z", 0.0);
        lobbyYaw = (float) cfg.getDouble("game.lobby.yaw", 0.0);
        lobbyPitch = (float) cfg.getDouble("game.lobby.pitch", 0.0);

        reinforcementsM = cfg.getInt("reinforcements.M", 50);
        reinforcementsC = cfg.getInt("reinforcements.C", -1);

        respawnDelaySeconds = Math.max(0, cfg.getInt("respawn.delay", 5));
        respawnMinDistance = Math.max(0.0, cfg.getDouble("respawn.min-distance", 20.0));
        safeSearchAttempts = Math.max(1, cfg.getInt("respawn.safe-search-attempts", 32));

        captureBaseSeconds = Math.max(0.5, cfg.getDouble("capture.base-time", 15.0));
        captureSpeedPerPlayer = Math.max(0.0, cfg.getDouble("capture.speed-per-player", 0.5));
        captureIntervalTicks = Math.max(1, cfg.getInt("capture.update-interval-ticks", 10));
        bossBarVisibleToAll = cfg.getBoolean("capture.bossbar.visible-to-all", true);

        teamName[Side.C.ordinal()] = cfg.getString("teams.C.name", "C军");
        teamColor[Side.C.ordinal()] = cfg.getString("teams.C.color", "§9");
        teamPrefix[Side.C.ordinal()] = cfg.getString("teams.C.prefix", "§9[C] ");
        teamName[Side.M.ordinal()] = cfg.getString("teams.M.name", "M军");
        teamColor[Side.M.ordinal()] = cfg.getString("teams.M.color", "§c");
        teamPrefix[Side.M.ordinal()] = cfg.getString("teams.M.prefix", "§c[M] ");
        hideEnemyNametag = cfg.getBoolean("teams.hide-enemy-nametag", true);
        hideEnemyTab = cfg.getBoolean("teams.hide-enemy-tab", true);
        tabMask = cfg.getString("teams.tab-mask", "§k");
        chatMode = cfg.getString("teams.chat-mode", "GLOBAL").trim().toUpperCase(Locale.ROOT);
        friendlyFire = cfg.getBoolean("teams.friendly-fire", false);
        syncVanillaTeamCommand = cfg.getBoolean("teams.sync-vanilla-team-command", true);

        preventEnemyPickup = cfg.getBoolean("kits.prevent-enemy-pickup", true);
        preventDrop = cfg.getBoolean("kits.prevent-drop", true);
        clearEffectsOnRespawn = cfg.getBoolean("kits.clear-effects-on-respawn", true);

        sidebarEnabled = cfg.getBoolean("sidebar.enabled", true);
        sidebarTitle = cfg.getString("sidebar.title", "§6玄剑·战争");
        lineTime = cfg.getString("sidebar.lines.time", "§f时间: §a{time}");
        lineReinforcements = cfg.getString("sidebar.lines.reinforcements", "§f攻方兵力: §c{count}");
        linePointOwned = cfg.getString("sidebar.lines.point-owned", "§f据点 {point}: {color}{side}军");
        linePointNeutral = cfg.getString("sidebar.lines.point-neutral", "§f据点 {point}: §7无主");
        linePointContesting = cfg.getString("sidebar.lines.point-contesting", "§f据点 {point}: {color}争夺中 {progress}%");
        lineFooter = cfg.getString("sidebar.lines.footer", "§7/bx help 查看指令");

        broadcastChat = cfg.getBoolean("broadcast.chat", true);
        broadcastTitle = cfg.getBoolean("broadcast.title", true);
        broadcastActionBar = cfg.getBoolean("broadcast.actionbar", true);
        broadcastSound = cfg.getBoolean("broadcast.sound", true);
        broadcastBossBar = cfg.getBoolean("broadcast.bossbar", true);
        broadcastSidebar = cfg.getBoolean("broadcast.sidebar", true);
        soundCountdown = cfg.getString("broadcast.sounds.countdown", "block.note_block.hat");
        soundStart = cfg.getString("broadcast.sounds.start", "entity.player.levelup");
        soundCapture = cfg.getString("broadcast.sounds.capture", "block.note_block.pling");
        soundDecapture = cfg.getString("broadcast.sounds.decapture", "block.note_block.bass");
        soundVictory = cfg.getString("broadcast.sounds.victory", "ui.toast.challenge_complete");
        soundDefeat = cfg.getString("broadcast.sounds.defeat", "entity.villager.no");

        statsPersist = cfg.getBoolean("stats.persist", true);
        statsFile = cfg.getString("stats.file", "stats.yml");
        pointsFile = cfg.getString("storage.points-file", "points.yml");
        itemsFile = cfg.getString("storage.items-file", "items.yml");
    }

    public static BxConfig load(FileConfiguration cfg) {
        return new BxConfig(cfg);
    }

    public String teamName(Side side) {
        return teamName[side.ordinal()];
    }

    public String teamColor(Side side) {
        return teamColor[side.ordinal()];
    }

    public String teamPrefix(Side side) {
        return teamPrefix[side.ordinal()];
    }

    /** 等待区大厅坐标；未配置或世界未加载时返回 {@code null}。 */
    public Location lobbyLocation() {
        if (lobbyWorld == null || lobbyWorld.isBlank()) {
            return null;
        }
        World world = Bukkit.getWorld(lobbyWorld);
        if (world == null) {
            return null;
        }
        return new Location(world, lobbyX, lobbyY, lobbyZ, lobbyYaw, lobbyPitch);
    }

    public boolean hasLobby() {
        return lobbyWorld != null && !lobbyWorld.isBlank();
    }

    public boolean teamOnlyChat() {
        return "TEAM_ONLY".equals(chatMode);
    }
}
