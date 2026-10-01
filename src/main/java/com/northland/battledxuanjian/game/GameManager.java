package com.northland.battledxuanjian.game;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.capture.CapturePoint;
import com.northland.battledxuanjian.config.BxConfig;
import com.northland.battledxuanjian.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

/**
 * 战局总控（需求 §5、§8、§12）：
 * 等待 → 倒计时 → 阵营分配 → 战斗 → 胜负判定 → 重置。
 */
public final class GameManager {

    private final BattledXuanjianPlugin plugin;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Map<UUID, Side> preferences = new HashMap<>();
    private final Map<UUID, Side> sides = new HashMap<>();
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Random random = new Random();

    private ReinforcementPool pool = new ReinforcementPool(0);
    private GamePhase phase = GamePhase.IDLE;
    private int countdown;
    private int timeRemaining = -1;
    private Side winner;
    private String endReason;
    private BukkitTask countdownTask;
    private BukkitTask updateTask;
    private BukkitTask clockTask;
    private BukkitTask pendingEndTask;

    public GameManager(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- 状态查询

    public GamePhase phase() {
        return phase;
    }

    public Side winner() {
        return winner;
    }

    public String endReason() {
        return endReason;
    }

    public ReinforcementPool pool() {
        return pool;
    }

    public int timeRemaining() {
        return timeRemaining;
    }

    public int countdown() {
        return countdown;
    }

    public Set<UUID> participants() {
        return participants;
    }

    public Side sideOf(Player player) {
        return player == null ? null : sideOf(player.getUniqueId());
    }

    public Side sideOf(UUID playerId) {
        return sides.get(playerId);
    }

    public boolean isParticipant(Player player) {
        return player != null && participants.contains(player.getUniqueId());
    }

    /** 是否处于进行中的战斗中（拥有阵营）。 */
    public boolean isPlaying(Player player) {
        return phase == GamePhase.RUNNING && player != null && sides.containsKey(player.getUniqueId());
    }

    public int countOf(Side side) {
        int count = 0;
        for (Side value : sides.values()) {
            if (value == side) {
                count++;
            }
        }
        return count;
    }

    public Location spawnLocation(Side side) {
        Location spawn = plugin.points().spawn(side);
        if (spawn != null && spawn.getWorld() != null) {
            return spawn;
        }
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0).getSpawnLocation();
    }

    // ---------------------------------------------------------------- 玩家进出

    public void join(Player player) {
        BxConfig cfg = plugin.config();
        UUID id = player.getUniqueId();
        if (!participants.add(id)) {
            plugin.messages().send(player, "already-joined");
            return;
        }
        returnLocations.putIfAbsent(id, player.getLocation().clone());

        if (phase == GamePhase.RUNNING) {
            if (!cfg.allowMidJoin) {
                participants.remove(id);
                returnLocations.remove(id);
                plugin.messages().send(player, "mid-join-denied");
                return;
            }
            Side side = cfg.autoBalance
                    ? TeamBalancer.pickSmallerSide(countOf(Side.C), countOf(Side.M), random)
                    : (random.nextBoolean() ? Side.M : Side.C);
            sides.put(id, side);
            plugin.teams().applyPlayer(player, side);
            plugin.tab().refresh();
            preparePlayer(player, side);
            plugin.messages().send(player, "team-assigned", "side", side.id(), "color", cfg.teamColor(side));
            updateSidebar();
            return;
        }

        if (phase == GamePhase.IDLE) {
            phase = GamePhase.WAITING;
        }
        Location lobby = cfg.lobbyLocation();
        if (lobby != null) {
            player.teleport(lobby);
        }
        player.setGameMode(GameMode.SURVIVAL);
        plugin.messages().send(player, "joined");
    }

    public void leave(Player player) {
        UUID id = player.getUniqueId();
        if (!participants.remove(id)) {
            plugin.messages().send(player, "not-joined");
            return;
        }
        sides.remove(id);
        preferences.remove(id);
        plugin.teams().removePlayer(player);
        plugin.tab().refresh();
        plugin.kits().clearInventory(player);
        clearEffects(player);
        player.setGameMode(GameMode.SURVIVAL);
        Location back = returnLocations.remove(id);
        if (back != null) {
            player.teleport(back);
        }
        plugin.messages().send(player, "left");
        if (participants.isEmpty() && phase == GamePhase.WAITING) {
            phase = GamePhase.IDLE;
        }
        updateSidebar();
    }

    /** 预选阵营（仅开局前有效）。 */
    public void preselect(Player player, Side side) {
        if (phase == GamePhase.RUNNING || phase == GamePhase.COUNTDOWN) {
            plugin.messages().send(player, "game-running");
            return;
        }
        if (!participants.contains(player.getUniqueId())) {
            join(player);
        }
        preferences.put(player.getUniqueId(), side);
        plugin.messages().send(player, "team-joined", "side", side.id(), "color", plugin.config().teamColor(side));
    }

    /** 玩家离开服务器：比赛中保留阵营以便重连回归。 */
    public void handleQuit(Player player) {
        if (phase != GamePhase.RUNNING && phase != GamePhase.COUNTDOWN && phase != GamePhase.ENDING) {
            participants.remove(player.getUniqueId());
            preferences.remove(player.getUniqueId());
            if (participants.isEmpty() && phase == GamePhase.WAITING) {
                phase = GamePhase.IDLE;
            }
        }
    }

    /** 玩家重连回归战局。 */
    public void handleRejoin(Player player) {
        UUID id = player.getUniqueId();
        if (phase == GamePhase.RUNNING && sides.containsKey(id)) {
            Side side = sides.get(id);
            plugin.teams().applyPlayer(player, side);
            preparePlayer(player, side);
            plugin.messages().send(player, "team-assigned", "side", side.id(), "color", plugin.config().teamColor(side));
            return;
        }
        if (participants.contains(id)) {
            Location lobby = plugin.config().lobbyLocation();
            if (lobby != null) {
                player.teleport(lobby);
            }
            plugin.messages().send(player, "join-hint");
        }
    }

    // ---------------------------------------------------------------- 开局

    public void start(CommandSender sender) {
        BxConfig cfg = plugin.config();
        if (phase == GamePhase.COUNTDOWN || phase == GamePhase.RUNNING) {
            reply(sender, "game-running");
            return;
        }
        if (plugin.points().size() == 0) {
            reply(sender, "no-points");
            return;
        }
        if (participants.size() < cfg.minPlayers) {
            reply(sender, "not-enough-players", "min", cfg.minPlayers, "current", participants.size());
            return;
        }
        cancelCountdown();
        countdown = cfg.countdownSeconds;
        if (countdown <= 0) {
            beginMatch();
            return;
        }
        phase = GamePhase.COUNTDOWN;
        plugin.sidebar().show();
        updateSidebar();
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickCountdown, 0L, 20L);
    }

    private void tickCountdown() {
        BxConfig cfg = plugin.config();
        if (phase != GamePhase.COUNTDOWN) {
            cancelCountdown();
            return;
        }
        if (participants.size() < cfg.minPlayers) {
            cancelCountdown();
            phase = participants.isEmpty() ? GamePhase.IDLE : GamePhase.WAITING;
            plugin.broadcast().chat(plugin.messages().get("countdown-cancelled"));
            plugin.sidebar().hide();
            return;
        }
        if (countdown <= 0) {
            beginMatch();
            return;
        }
        plugin.broadcast().countdownTick(countdown);
        countdown--;
    }

    private void beginMatch() {
        cancelCountdown();
        BxConfig cfg = plugin.config();
        pool = new ReinforcementPool(cfg.reinforcementsM);
        plugin.stats().beginMatch();

        TeamBalancer.Result result = TeamBalancer.balance(new ArrayList<>(participants), preferences, random);
        sides.clear();
        sides.putAll(result.sides());
        plugin.teams().reset();
        for (UUID id : new ArrayList<>(participants)) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                continue;
            }
            Side side = sides.get(id);
            if (side == null) {
                continue;
            }
            plugin.teams().applyPlayer(player, side);
            preparePlayer(player, side);
            plugin.messages().send(player, "team-assigned", "side", side.id(), "color", cfg.teamColor(side));
        }
        plugin.points().reset(Side.C);
        plugin.bossBars().syncPointBars(plugin.points().points());
        plugin.tab().refresh();

        timeRemaining = cfg.timeLimitSeconds > 0 ? cfg.timeLimitSeconds : -1;
        phase = GamePhase.RUNNING;
        plugin.sidebar().show();

        String text = plugin.messages().get("game-started", "side", Side.M.id(), "color", cfg.teamColor(Side.M));
        plugin.broadcast().chat(text);
        plugin.broadcast().title(text, plugin.messages().get("countdown-subtitle"), 10, 40, 10);
        plugin.broadcast().sound(cfg.soundStart, 1.0f, 1.0f);
        updateSidebar();

        int interval = cfg.captureIntervalTicks;
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickMatch, interval, interval);
        clockTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickClock, 20L, 20L);
    }

    /** 每 {@code capture.update-interval-ticks} 刻执行：推进据点与刷新 UI。 */
    private void tickMatch() {
        if (phase != GamePhase.RUNNING) {
            return;
        }
        plugin.points().tick();
        if (phase != GamePhase.RUNNING) {
            return;
        }
        updateSidebar();
    }

    /** 每秒执行：倒计时时间限制。 */
    private void tickClock() {
        if (phase != GamePhase.RUNNING) {
            return;
        }
        if (timeRemaining > 0) {
            timeRemaining--;
            if (timeRemaining <= 0) {
                endMatch(Side.C, "time-up");
                return;
            }
        }
        updateSidebar();
    }

    // ---------------------------------------------------------------- 结算

    public void endMatch(Side winnerSide, String reason) {
        if (phase != GamePhase.RUNNING && phase != GamePhase.COUNTDOWN) {
            return;
        }
        phase = GamePhase.ENDING;
        cancelTasks();
        this.winner = winnerSide;
        this.endReason = reason;

        BxConfig cfg = plugin.config();
        String text = plugin.messages().get("victory",
                "side", winnerSide.id(),
                "color", cfg.teamColor(winnerSide),
                "reason", plugin.messages().get("reason-" + reason));
        plugin.broadcast().chat(text);
        plugin.broadcast().title(text, "", 10, 60, 15);
        plugin.broadcast().chat(plugin.messages().get("reason-" + reason, "side", winnerSide.id()));

        if ("reinforcements".equals(reason)) {
            plugin.broadcast().chat(plugin.messages().get("reinforcements-depleted"));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (sideOf(player) == Side.M) {
                    player.setGameMode(GameMode.SPECTATOR);
                    plugin.messages().send(player, "team-spectate");
                }
            }
        } else if ("time-up".equals(reason)) {
            plugin.broadcast().chat(plugin.messages().get("time-up"));
        } else if ("all-points".equals(reason)) {
            plugin.broadcast().chat(plugin.messages().get("all-points-captured"));
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Side side = sideOf(player);
            plugin.broadcast().sound(player, side == winnerSide ? cfg.soundVictory : cfg.soundDefeat, 1.0f, 1.0f);
        }

        plugin.stats().endMatch();
        plugin.broadcast().chat(plugin.messages().get("match-summary-header"));
        for (String line : plugin.stats().summaryLines()) {
            plugin.broadcast().chat(line);
        }

        pendingEndTask = Bukkit.getScheduler().runTaskLater(plugin, this::reset, 100L);
    }

    /** 强制结束（{@code /bx stop}）：不判定胜负，直接重置。 */
    public void forceStop(CommandSender sender) {
        if (phase == GamePhase.IDLE || phase == GamePhase.WAITING) {
            reply(sender, "game-idle");
            return;
        }
        reply(sender, "game-stopped");
        plugin.broadcast().chat(plugin.messages().get("game-stopped"));
        phase = GamePhase.ENDING;
        cancelTasks();
        reset();
    }

    /** 重置全部状态，准备连续开局（需求 §12.2）。 */
    public void reset() {
        cancelTasks();
        BxConfig cfg = plugin.config();
        for (UUID id : new ArrayList<>(sides.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                continue;
            }
            plugin.kits().clearInventory(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setFireTicks(0);
            clearEffects(player);
            plugin.teams().removePlayer(player);
            Location back = returnLocations.get(id);
            if (back == null) {
                back = cfg.lobbyLocation();
            }
            if (back != null) {
                player.teleport(back);
            }
        }
        sides.clear();
        preferences.clear();
        winner = null;
        endReason = null;
        timeRemaining = -1;
        pool = new ReinforcementPool(cfg.reinforcementsM);
        plugin.points().reset(Side.C);
        plugin.bossBars().clearAll();
        plugin.sidebar().hide();
        plugin.respawn().clear();
        plugin.tab().refresh();
        phase = participants.isEmpty() ? GamePhase.IDLE : GamePhase.WAITING;

        if (cfg.autoNextRound && participants.size() >= cfg.minPlayers) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (phase == GamePhase.WAITING) {
                    start(null);
                }
            }, 100L);
        }
    }

    /** 服务器关闭时调用。 */
    public void shutdown() {
        cancelTasks();
        if (pendingEndTask != null) {
            pendingEndTask.cancel();
            pendingEndTask = null;
        }
    }

    private void cancelTasks() {
        cancelCountdown();
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (clockTask != null) {
            clockTask.cancel();
            clockTask = null;
        }
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    // ---------------------------------------------------------------- 玩家准备

    /** 传送、清状态、发装备。 */
    public void preparePlayer(Player player, Side side) {
        Location spawn = spawnLocation(side);
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.setGameMode(GameMode.SURVIVAL);
        resetVitals(player);
        plugin.kits().give(player, side);
    }

    /** 重生后的准备（重生事件已完成落点设置）。 */
    public void prepareRespawned(Player player, Side side) {
        player.setGameMode(GameMode.SURVIVAL);
        resetVitals(player);
        plugin.kits().give(player, side);
        plugin.messages().send(player, "team-assigned",
                "side", side.id(), "color", plugin.config().teamColor(side));
    }

    private void resetVitals(Player player) {
        try {
            player.setHealth(20.0);
        } catch (IllegalArgumentException ignored) {
            // 最大生命值被修改时忽略
        }
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        if (plugin.config().clearEffectsOnRespawn) {
            clearEffects(player);
        }
    }

    private void clearEffects(Player player) {
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
    }

    // ---------------------------------------------------------------- UI

    public void updateSidebar() {
        plugin.sidebar().update(sidebarLines());
    }

    /** 构建侧边栏内容（需求 §11）。 */
    public List<String> sidebarLines() {
        BxConfig cfg = plugin.config();
        List<String> lines = new ArrayList<>();
        lines.add(Text.render(cfg.lineTime, "time", timeRemaining < 0 ? "∞" : Text.clock(timeRemaining)));
        lines.add(Text.render(cfg.lineReinforcements, "count", pool.display()));
        for (CapturePoint point : plugin.points().points()) {
            if (point.owner() != null) {
                lines.add(Text.render(cfg.linePointOwned,
                        "point", point.name(),
                        "side", point.owner().id(),
                        "color", cfg.teamColor(point.owner())));
            } else if (point.contestant() != null) {
                lines.add(Text.render(cfg.linePointContesting,
                        "point", point.name(),
                        "color", cfg.teamColor(point.contestant()),
                        "progress", point.progressPercent()));
            } else {
                lines.add(Text.render(cfg.linePointNeutral, "point", point.name()));
            }
        }
        lines.add(cfg.lineFooter);
        return lines;
    }

    /** {@code /bx status} 输出。 */
    public List<String> statusLines() {
        BxConfig cfg = plugin.config();
        List<String> lines = new ArrayList<>();
        lines.add(plugin.messages().get("status-header"));
        lines.add(plugin.messages().get("status-phase", "phase", phase.display()));
        lines.add(plugin.messages().get("status-players",
                "count", participants.size(), "c", countOf(Side.C), "m", countOf(Side.M)));
        lines.add(plugin.messages().get("status-reinforcements", "count", pool.display()));
        lines.add(plugin.messages().get("status-time", "time", timeRemaining < 0 ? "∞" : Text.clock(timeRemaining)));
        lines.add(plugin.messages().get("status-points", "points", plugin.points().names().toString()));
        return lines;
    }

    private void reply(CommandSender sender, String key, Object... pairs) {
        if (sender != null) {
            plugin.messages().send(sender, key, pairs);
        } else {
            plugin.getLogger().info(plugin.messages().get(key, pairs));
        }
    }
}
