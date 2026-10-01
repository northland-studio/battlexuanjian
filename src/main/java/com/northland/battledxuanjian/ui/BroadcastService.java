package com.northland.battledxuanjian.ui;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.config.BxConfig;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * 宣告服务：聊天栏 / Title / ActionBar / 音效。
 *
 * <p>所有宣告通道都在 {@code config.yml broadcast} 中独立开关（需求 §11）。</p>
 */
public final class BroadcastService {

    private final BattledXuanjianPlugin plugin;

    public BroadcastService(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
    }

    /** 全服聊天宣告（受 {@code broadcast.chat} 控制）。 */
    public void chat(String legacyText) {
        if (!plugin.config().broadcastChat || legacyText == null || legacyText.isEmpty()) {
            return;
        }
        Bukkit.getServer().broadcastMessage(legacyText);
    }

    /** 仅发给某个玩家的聊天消息（不受广播开关影响）。 */
    public void message(Player player, String legacyText) {
        if (player != null && legacyText != null) {
            player.sendMessage(legacyText);
        }
    }

    /** 全服 Title（受 {@code broadcast.title} 控制）。 */
    public void title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!plugin.config().broadcastTitle) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /** 单个玩家 Title（不受广播开关影响，用于倒计时等）。 */
    public void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player != null) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /** 全服 ActionBar（受 {@code broadcast.actionbar} 控制）。 */
    public void actionBar(String text) {
        if (!plugin.config().broadcastActionBar) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(text);
        }
    }

    public void actionBar(Player player, String text) {
        if (player != null) {
            player.sendActionBar(text);
        }
    }

    /** 全服音效（受 {@code broadcast.sound} 控制）。 */
    public void sound(String soundKey, float volume, float pitch) {
        if (!plugin.config().broadcastSound || soundKey == null || soundKey.isBlank()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            playSound(player, soundKey, volume, pitch);
        }
    }

    public void sound(Player player, String soundKey, float volume, float pitch) {
        if (!plugin.config().broadcastSound) {
            return;
        }
        playSound(player, soundKey, volume, pitch);
    }

    private void playSound(Player player, String soundKey, float volume, float pitch) {
        try {
            player.playSound(player.getLocation(), soundKey, SoundCategory.MASTER, volume, pitch);
        } catch (Throwable throwable) {
            // 声音名在跨版本时可能失效：只记录一次，不影响游戏流程
            plugin.getLogger().fine("无法播放音效 " + soundKey + ": " + throwable.getMessage());
        }
    }

    /** 倒计时提醒（Title + ActionBar + 音效 + 聊天，按配置开关）。 */
    public void countdownTick(int seconds) {
        BxConfig cfg = plugin.config();
        String title = plugin.messages().get("countdown-title", "seconds", seconds);
        String subtitle = plugin.messages().get("countdown-subtitle");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (cfg.broadcastTitle) {
                player.sendTitle(title, subtitle, 0, 25, 5);
            }
            if (cfg.broadcastActionBar) {
                player.sendActionBar(plugin.messages().get("countdown-chat", "seconds", seconds));
            }
        }
        sound(cfg.soundCountdown, 0.7f, 1.2f);
    }

    /** 战报类大宣告：聊天 + Title + 音效。 */
    public void bigAnnounce(String chatText, String titleText, String subtitleText, String soundKey) {
        chat(chatText);
        title(titleText, subtitleText, 10, 60, 15);
        sound(soundKey, 1.0f, 1.0f);
    }
}
