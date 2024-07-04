package com.example.queueplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QueueListener implements Listener {

    private final QueuePlugin plugin;

    public QueueListener(QueuePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getQueue().remove(event.getPlayer().getUniqueId())) {
            plugin.notifyPositionChange();
        }
    }
}
