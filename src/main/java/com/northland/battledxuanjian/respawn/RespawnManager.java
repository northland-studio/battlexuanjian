package com.northland.battledxuanjian.respawn;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.config.BxConfig;
import com.northland.battledxuanjian.game.Side;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * 重生管理（需求 §8）。
 *
 * <p>死亡后延迟 {@code respawn.delay} 秒重生；重生点优先使用阵营出发点，
 * 若出发点距离死亡点不足 {@code respawn.min-distance} 格，则在死亡点周围寻找安全落点。</p>
 */
public final class RespawnManager {

    private final BattledXuanjianPlugin plugin;
    private final Map<UUID, Location> deathLocations = new HashMap<>();
    private final Random random = new Random();

    public RespawnManager(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    /** 死亡处理：不掉落装备、记录死亡点、延迟重生。 */
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!plugin.game().isPlaying(player)) {
            return;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        deathLocations.put(player.getUniqueId(), player.getLocation().clone());

        plugin.stats().recordDeath(player);
        Player killer = player.getKiller();
        if (killer != null && killer.getUniqueId() != player.getUniqueId()) {
            plugin.stats().recordKill(killer);
        }

        BxConfig cfg = plugin.config();
        long delayTicks = Math.max(1L, cfg.respawnDelaySeconds * 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (plugin.game().phase() != com.northland.battledxuanjian.game.GamePhase.RUNNING) {
                return;
            }
            if (player.isDead()) {
                player.spigot().respawn();
            }
        }, delayTicks);
    }

    /** 重生处理：决定落点、扣除攻方兵力、重生后补发装备。 */
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!plugin.game().isPlaying(player)) {
            return;
        }
        Side side = plugin.game().sideOf(player);
        if (side == null) {
            return;
        }
        Location death = deathLocations.remove(player.getUniqueId());
        Location target = chooseLocation(side, death);
        if (target != null) {
            event.setRespawnLocation(target);
        }

        if (side == Side.M) {
            boolean consumed = plugin.game().pool().consume();
            if (consumed) {
                int remaining = plugin.game().pool().remaining();
                plugin.broadcast().actionBar(player, plugin.messages().get("reinforcements-left", "count", remaining));
            }
            if (plugin.game().pool().depleted()) {
                plugin.game().endMatch(Side.C, "reinforcements");
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.game().prepareRespawned(player, side);
            }
        }, 1L);
    }

    private Location chooseLocation(Side side, Location death) {
        BxConfig cfg = plugin.config();
        Location spawn = plugin.game().spawnLocation(side);
        if (death == null || death.getWorld() == null) {
            return spawn;
        }
        if (spawn == null || spawn.getWorld() == null) {
            return findSafeLocation(death, cfg.respawnMinDistance);
        }
        if (!spawn.getWorld().equals(death.getWorld()) || spawn.distance(death) >= cfg.respawnMinDistance) {
            return spawn;
        }
        Location safe = findSafeLocation(death, cfg.respawnMinDistance);
        return safe != null ? safe : spawn;
    }

    /** 在死亡点周围随机搜索安全落点。 */
    private Location findSafeLocation(Location death, double minDistance) {
        World world = death.getWorld();
        if (world == null) {
            return null;
        }
        BxConfig cfg = plugin.config();
        for (int attempt = 0; attempt < cfg.safeSearchAttempts; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = minDistance + random.nextDouble() * 16.0;
            int x = (int) Math.floor(death.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(death.getZ() + Math.sin(angle) * distance);
            if (!world.isChunkGenerated(x >> 4, z >> 4)) {
                continue;
            }
            Block ground = world.getHighestBlockAt(x, z);
            Location candidate = ground.getLocation().add(0.5, 1.0, 0.5);
            if (isSafe(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block below = feet.getRelative(0, -1, 0);
        Material groundType = below.getType();
        if (!groundType.isSolid() || groundType == Material.MAGMA_BLOCK) {
            return false;
        }
        if (groundType == Material.LAVA || groundType == Material.FIRE || groundType == Material.CACTUS
                || groundType == Material.CAMPFIRE || groundType == Material.SOUL_CAMPFIRE) {
            return false;
        }
        if (feet.isLiquid() || head.isLiquid()) {
            return false;
        }
        return feet.isPassable() && head.isPassable();
    }

    /** 清空死亡记录（对局重置时调用）。 */
    public void clear() {
        deathLocations.clear();
    }
}
