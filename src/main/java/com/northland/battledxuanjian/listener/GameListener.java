package com.northland.battledxuanjian.listener;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.game.GamePhase;
import com.northland.battledxuanjian.game.Side;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * 战局相关事件监听（需求 §5、§8、§9、§10）。
 */
public final class GameListener implements Listener {

    private final BattledXuanjianPlugin plugin;

    public GameListener(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.game().handleRejoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.game().handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        plugin.respawn().onDeath(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.respawn().onRespawn(event);
    }

    /** 友军伤害关闭 + 伤害统计（需求 §9、§10）。 */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        Side victimSide = plugin.game().sideOf(victim);
        Side attackerSide = plugin.game().sideOf(attacker);
        if (victimSide != null && victimSide == attackerSide && !plugin.config().friendlyFire) {
            event.setCancelled(true);
            return;
        }
        if (victimSide != null && attackerSide != null && plugin.game().phase() != GamePhase.RUNNING) {
            // 等待区/倒计时期间关闭 PvP
            event.setCancelled(true);
            return;
        }
        plugin.stats().recordDamage(attacker, event.getFinalDamage());
    }

    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /** 禁止丢弃本阵营装备包物品（需求 §9）。 */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.config().preventDrop || !plugin.game().isPlaying(event.getPlayer())) {
            return;
        }
        if (plugin.kits().kitSide(event.getItemDrop().getItemStack()) != null) {
            event.setCancelled(true);
        }
    }

    /** 禁止拾取敌方装备（需求 §9）。 */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.config().preventEnemyPickup || !(event.getEntity() instanceof Player player)) {
            return;
        }
        Side side = plugin.game().sideOf(player);
        if (side == null) {
            return;
        }
        Side itemSide = plugin.kits().kitSide(event.getItem().getItemStack());
        if (itemSide != null && itemSide != side) {
            event.setCancelled(true);
        }
    }

    /** 原版 {@code /team}、{@code /scoreboard} 指令修改后的自动同步（需求 §6、§13）。 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.config().syncVanillaTeamCommand) {
            return;
        }
        String message = event.getMessage().toLowerCase(Locale.ROOT).trim();
        if (message.equals("/team") || message.startsWith("/team ")
                || message.startsWith("/scoreboard ") || message.equals("/scoreboard")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.teams().refreshAll(), 1L);
        }
    }
}
