package com.example.queueplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class QueuePlugin extends JavaPlugin {

    private Queue<UUID> queue;
    private int maxQueueSize;
    private String messagePrefix;
    private boolean notifyJoin;
    private boolean notifyLeave;
    private long joinCooldown;
    private Map<UUID, Long> cooldowns;
    private Map<UUID, Integer> priorities;

    @Override
    public void onEnable() {
        queue = new LinkedList<>();
        cooldowns = new HashMap<>();
        priorities = new HashMap<>();
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        maxQueueSize = config.getInt("maxQueueSize", 10);
        messagePrefix = config.getString("messagePrefix", "&6[Queue] &r");
        notifyJoin = config.getBoolean("notifyJoin", true);
        notifyLeave = config.getBoolean("notifyLeave", true);
        joinCooldown = config.getLong("joinCooldown", 60) * 1000;

        loadQueue();

        getCommand("joinqueue").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                long currentTime = System.currentTimeMillis();
                if (cooldowns.containsKey(player.getUniqueId()) && (currentTime - cooldowns.get(player.getUniqueId())) < joinCooldown) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&cYou must wait before joining the queue again."));
                    return true;
                }
                if (!queue.contains(player.getUniqueId())) {
                    if (queue.size() < maxQueueSize) {
                        queue.add(player.getUniqueId());
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&aYou have joined the queue."));
                        if (notifyJoin) {
                            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&e" + player.getName() + " has joined the queue."));
                        }
                        cooldowns.put(player.getUniqueId(), currentTime);
                        notifyPositionChange();
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&cThe queue is full."));
                    }
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&cYou are already in the queue."));
                }
            }
            return true;
        });

        getCommand("leavequeue").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (queue.remove(player.getUniqueId())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&aYou have left the queue."));
                    if (notifyLeave) {
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&e" + player.getName() + " has left the queue."));
                    }
                    notifyPositionChange();
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&cYou are not in the queue."));
                }
            }
            return true;
        });

        getCommand("nextinqueue").setExecutor((sender, command, label, args) -> {
            if (!queue.isEmpty()) {
                UUID nextPlayerUUID = queue.poll();
                if (nextPlayerUUID != null) {
                    Player nextPlayer = Bukkit.getPlayer(nextPlayerUUID);
                    if (nextPlayer != null) {
                        nextPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&aIt's your turn!"));
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&e" + nextPlayer.getName() + " is now being served."));
                    }
                }
                notifyPositionChange();
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&cThe queue is empty."));
            }
            return true;
        });

        getCommand("viewqueue").setExecutor((sender, command, label, args) -> {
            if (!queue.isEmpty()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&aCurrent queue:"));
                int position = 1;
                for (UUID playerUUID : queue) {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&e" + position + ". " + player.getName()));
                    }
                    position++;
                }
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&cThe queue is empty."));
            }
            return true;
        });

        getCommand("clearqueue").setExecutor((sender, command, label, args) -> {
            queue.clear();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&aThe queue has been cleared."));
            return true;
        });

        getServer().getPluginManager().registerEvents(new QueueListener(this), this);

        getLogger().log(Level.INFO, "QueuePlugin has been enabled.");
    }

    @Override
    public void onDisable() {
        saveQueue();
        queue.clear();
        getLogger().log(Level.INFO, "QueuePlugin has been disabled.");
    }

    public void notifyPositionChange() {
        int position = 1;
        for (UUID playerUUID : queue) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagePrefix + "&aYour new position in the queue is: &e" + position));
            }
            position++;
        }
    }

    private void saveQueue() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(getDataFolder(), "queue.dat")))) {
            oos.writeObject(new LinkedList<>(queue));
            getLogger().log(Level.INFO, "Queue saved successfully.");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to save the queue.", e);
        }
    }

    private void loadQueue() {
        File file = new File(getDataFolder(), "queue.dat");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                queue = (Queue<UUID>) ois.readObject();
                getLogger().log(Level.INFO, "Queue loaded successfully.");
            } catch (IOException | ClassNotFoundException e) {
                getLogger().log(Level.SEVERE, "Failed to load the queue.", e);
                queue = new LinkedList<>();
            }
        }
    }
}
