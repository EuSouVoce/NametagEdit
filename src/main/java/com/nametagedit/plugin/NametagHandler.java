package com.nametagedit.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.nametagedit.plugin.api.data.GroupData;
import com.nametagedit.plugin.api.data.INametag;
import com.nametagedit.plugin.api.data.PlayerData;
import com.nametagedit.plugin.api.events.NametagEvent;
import com.nametagedit.plugin.api.events.NametagFirstLoadedEvent;
import com.nametagedit.plugin.metrics.Metrics;
import com.nametagedit.plugin.storage.AbstractConfig;
import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.nametagedit.plugin.storage.flatfile.FlatFileConfig;
import com.nametagedit.plugin.utils.Configuration;
import com.nametagedit.plugin.utils.UUIDFetcher;
import com.nametagedit.plugin.utils.Utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPIPlugin;

@Getter
@Setter
@SuppressWarnings("deprecation")
public class NametagHandler implements Listener {

    // Multiple threads access resources. We need to make sure we avoid concurrency
    // issues.
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static boolean DISABLE_PUSH_ALL_TAGS = false;
    private boolean debug;
    @Getter(AccessLevel.NONE)
    private boolean tabListEnabled;
    private boolean longNametagsEnabled;
    private boolean refreshTagOnWorldChange;

    private BukkitTask clearEmptyTeamTask;
    private BukkitTask refreshNametagTask;
    private AbstractConfig abstractConfig;

    private Configuration config;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<GroupData> groupData = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Map<UUID, PlayerData> playerData = new HashMap<>();

    private NametagEdit plugin;
    private NametagManager nametagManager;

    public NametagHandler(final NametagEdit plugin, final NametagManager nametagManager) {
        this.config = this.getCustomConfig(plugin);
        this.plugin = plugin;
        this.nametagManager = nametagManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Apply config properties
        this.applyConfig();

        if (this.config.getBoolean("MySQL.Enabled")) {
            this.abstractConfig = new DatabaseConfig(plugin, this, this.config);
        } else {
            this.abstractConfig = new FlatFileConfig(plugin, this);
        }

        new BukkitRunnable() {
            @Override
            public void run() { NametagHandler.this.abstractConfig.load(); }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * This function loads our custom config with comments, and includes changes
     */
    private Configuration getCustomConfig(final Plugin plugin) {
        final File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveDefaultConfig();

            final Configuration newConfig = new Configuration(file);
            newConfig.reload(true);
            return newConfig;
        } else {
            final Configuration oldConfig = new Configuration(file);
            oldConfig.reload(false);

            file.delete();
            plugin.saveDefaultConfig();

            final Configuration newConfig = new Configuration(file);
            newConfig.reload(true);

            for (final String key : oldConfig.getKeys(false)) {
                if (newConfig.contains(key)) {
                    newConfig.set(key, oldConfig.get(key));
                }
            }

            newConfig.save();
            return newConfig;
        }
    }

    /**
     * Cleans up any nametag data on the server to prevent memory leaks
     */
    @EventHandler
    public void onQuit(final PlayerQuitEvent event) { this.nametagManager.reset(event.getPlayer().getName()); }

    /**
     * Applies tags to a player
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.nametagManager.sendTeams(player);

        new BukkitRunnable() {
            @Override
            public void run() { NametagHandler.this.abstractConfig.load(player, true); }
        }.runTaskLaterAsynchronously(this.plugin, 1);
    }

    /**
     * Some users may have different permissions per world. If this is enabled,
     * their tag will be reloaded on TP.
     */
    @EventHandler
    public void onTeleport(final PlayerChangedWorldEvent event) {
        if (!this.refreshTagOnWorldChange)
            return;

        new BukkitRunnable() {
            @Override
            public void run() { NametagHandler.this.applyTagToPlayer(event.getPlayer(), false); }
        }.runTaskLater(this.plugin, 3);
    }

    private void handleClear(final UUID uuid, final String player) {
        this.removePlayerData(uuid);
        this.nametagManager.reset(player);
        this.abstractConfig.clear(uuid, player);
    }

    public void clearMemoryData() {
        try {
            this.readWriteLock.writeLock().lock();
            this.groupData.clear();
            this.playerData.clear();
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public void removePlayerData(final UUID uuid) {
        try {
            this.readWriteLock.writeLock().lock();
            this.playerData.remove(uuid);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public void storePlayerData(final UUID uuid, final PlayerData data) {
        try {
            this.readWriteLock.writeLock().lock();
            this.playerData.put(uuid, data);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public void assignGroupData(final List<GroupData> groupData) {
        try {
            this.readWriteLock.writeLock().lock();
            this.groupData = groupData;
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public void assignData(final List<GroupData> groupData, final Map<UUID, PlayerData> playerData) {
        try {
            this.readWriteLock.writeLock().lock();
            this.groupData = groupData;
            this.playerData = playerData;
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    // ==========================================
    // Below are methods used by the API/Commands
    // ==========================================
    boolean debug() { return this.debug; }

    void toggleDebug() {
        this.debug = !this.debug;
        this.config.set("Debug", this.debug);
        this.config.save();
    }

    void toggleLongTags() {
        this.longNametagsEnabled = !this.longNametagsEnabled;
        this.config.set("Tablist.LongTags", this.longNametagsEnabled);
        this.config.save();
    }

    // =================================================
    // Below are methods that we have to be careful with
    // as they can be called from different threads
    // =================================================
    public PlayerData getPlayerData(final Player player) { return player == null ? null : this.playerData.get(player.getUniqueId()); }

    void addGroup(final GroupData data) {
        this.abstractConfig.add(data);

        try {
            this.readWriteLock.writeLock().lock();
            this.groupData.add(data);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    void deleteGroup(final GroupData data) {
        this.abstractConfig.delete(data);

        try {
            this.readWriteLock.writeLock().lock();
            this.groupData.remove(data);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public List<GroupData> getGroupData() {
        try {
            this.readWriteLock.writeLock().lock();
            return new ArrayList<>(this.groupData); // Create a copy instead of unmodifiable
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public GroupData getGroupData(final String key) {
        for (final GroupData groupData : this.getGroupData()) {
            if (groupData.getGroupName().equalsIgnoreCase(key)) {
                return groupData;
            }
        }

        return null;
    }

    /**
     * Replaces placeholders when a player tag is created. Maxim and Clip's plugins
     * are searched for, and input is replaced. We use direct imports to avoid any
     * problems! (So don't change that)
     */
    public String formatWithPlaceholders(final Player player, String input, final boolean limitChars) {
        this.plugin.debug("Formatting text..");
        if (input == null)
            return "";
        if (player == null)
            return input;

        // The string can become null again at this point. Add another check.
        if (input != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.plugin.debug("Trying to use PlaceholderAPI for placeholders");
            input = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, input);
        }

        this.plugin.debug("Applying colors..");
        return Utils.format(input, limitChars);
    }

    private BukkitTask createTask(final String path, final BukkitTask existing, final Runnable runnable) {
        if (existing != null) {
            existing.cancel();
        }

        if (this.config.getInt(path, -1) <= 0)
            return null;
        return Bukkit.getScheduler().runTaskTimer(this.plugin, runnable, 0, 20L * this.config.getInt(path));
    }

    public void reload() {
        this.config.reload(true);
        this.applyConfig();
        this.nametagManager.reset();
        this.abstractConfig.reload();
    }

    private void applyConfig() {
        this.debug = this.config.getBoolean("Debug");
        this.tabListEnabled = this.config.getBoolean("Tablist.Enabled");
        this.longNametagsEnabled = this.config.getBoolean("Tablist.LongTags");
        this.refreshTagOnWorldChange = this.config.getBoolean("RefreshTagOnWorldChange");
        NametagHandler.DISABLE_PUSH_ALL_TAGS = this.config.getBoolean("DisablePush");

        if (this.config.getBoolean("MetricsEnabled")) {
            final Metrics m = new Metrics(NametagEdit.getPlugin(NametagEdit.class));
            m.addCustomChart(
                    new Metrics.SimplePie("using_spigot", () -> PlaceholderAPIPlugin.getServerVersion().isSpigot() ? "yes" : "no"));
        }

        this.clearEmptyTeamTask = this.createTask("ClearEmptyTeamsInterval", this.clearEmptyTeamTask,
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "nte teams clear"));

        this.refreshNametagTask = this.createTask("RefreshInterval", this.refreshNametagTask, () -> {
            this.nametagManager.reset();
            this.applyTags();
        });
    }

    public void applyTags() {
        if (!Bukkit.isPrimaryThread()) {
            new BukkitRunnable() {
                @Override
                public void run() { NametagHandler.this.applyTags(); }
            }.runTask(this.plugin);
            return;
        }

        for (final Player online : Utils.getOnline()) {
            if (online != null) {
                this.applyTagToPlayer(online, false);
            }
        }

        this.plugin.debug("Applied tags to all online players.");
    }

    public void applyTagToPlayer(final Player player, final boolean loggedIn) {
        // If on the primary thread, run async
        if (Bukkit.isPrimaryThread()) {
            new BukkitRunnable() {
                @Override
                public void run() { NametagHandler.this.applyTagToPlayer(player, loggedIn); }
            }.runTaskAsynchronously(this.plugin);
            return;
        }

        INametag tempNametag = this.getPlayerData(player);
        if (tempNametag == null) {
            for (final GroupData group : this.getGroupData()) {
                if (player.hasPermission(group.getBukkitPermission())) {
                    tempNametag = group;
                    break;
                }
            }
        }

        if (tempNametag == null)
            return;
        this.plugin.debug("Applying " + (tempNametag.isPlayerTag() ? "PlayerTag" : "GroupTag") + " to " + player.getName());

        final INametag nametag = tempNametag;
        new BukkitRunnable() {
            @Override
            public void run() {
                NametagHandler.this.nametagManager.setNametag(player.getName(),
                        NametagHandler.this.formatWithPlaceholders(player, nametag.getPrefix(), true),
                        NametagHandler.this.formatWithPlaceholders(player, nametag.getSuffix(), true), nametag.getSortPriority());
                // If the TabList is disabled...
                if (!NametagHandler.this.tabListEnabled) {
                    // apply the default white username to the player.
                    player.setPlayerListName(Utils.format("&f" + player.getPlayerListName()));
                } else {
                    if (NametagHandler.this.longNametagsEnabled) {
                        player.setPlayerListName(NametagHandler.this.formatWithPlaceholders(player,
                                nametag.getPrefix() + player.getName() + nametag.getSuffix(), false));
                    } else {
                        player.setPlayerListName(null);
                    }
                }

                if (loggedIn) {
                    Bukkit.getPluginManager().callEvent(new NametagFirstLoadedEvent(player, nametag));
                }
            }
        }.runTask(this.plugin);
    }

    void clear(final CommandSender sender, final String player) {
        final Player target = Bukkit.getPlayerExact(player);
        if (target != null) {
            this.handleClear(target.getUniqueId(), player);
            return;
        }

        UUIDFetcher.lookupUUID(player, this.plugin, uuid -> {
            if (uuid == null) {
                NametagMessages.UUID_LOOKUP_FAILED.send(sender);
            } else {
                this.handleClear(uuid, player);
            }
        });
    }

    void save(final CommandSender sender, final boolean playerTag, final String key, final int priority) {
        if (playerTag) {
            final Player player = Bukkit.getPlayerExact(key);

            final PlayerData data = this.getPlayerData(player);
            if (data == null) {
                this.abstractConfig.savePriority(true, key, priority);
                return;
            }

            data.setSortPriority(priority);
            this.abstractConfig.save(data);
        } else {
            final GroupData groupData = this.getGroupData(key);

            if (groupData == null) {
                sender.sendMessage(ChatColor.RED + "Group " + key + " does not exist!");
                return;
            }

            groupData.setSortPriority(priority);
            this.abstractConfig.save(groupData);
        }
    }

    public void save(final String targetName, final NametagEvent.ChangeType changeType, final String value) {
        this.save(null, targetName, changeType, value);
    }

    // Reduces checks to have this method (ie not saving data twice)
    public void save(final String targetName, final String prefix, final String suffix) {
        final Player player = Bukkit.getPlayerExact(targetName);

        PlayerData data = this.getPlayerData(player);
        if (data == null) {
            data = new PlayerData(targetName, null, "", "", -1);
            if (player != null) {
                this.storePlayerData(player.getUniqueId(), data);
            }
        }

        data.setPrefix(prefix);
        data.setSuffix(suffix);

        if (player != null) {
            this.applyTagToPlayer(player, false);
            data.setUuid(player.getUniqueId());
            this.abstractConfig.save(data);
            return;
        }

        final PlayerData finalData = data;
        UUIDFetcher.lookupUUID(targetName, this.plugin, uuid -> {
            if (uuid != null) {
                this.storePlayerData(uuid, finalData);
                finalData.setUuid(uuid);
                this.abstractConfig.save(finalData);
            }
        });
    }

    void save(final CommandSender sender, final String targetName, final NametagEvent.ChangeType changeType, final String value) {
        final Player player = Bukkit.getPlayerExact(targetName);

        PlayerData data = this.getPlayerData(player);
        if (data == null) {
            data = new PlayerData(targetName, null, "", "", -1);
            if (player != null) {
                this.storePlayerData(player.getUniqueId(), data);
            }
        }

        if (changeType == NametagEvent.ChangeType.PREFIX) {
            data.setPrefix(value);
        } else {
            data.setSuffix(value);
        }

        if (player != null) {
            this.applyTagToPlayer(player, false);
            data.setUuid(player.getUniqueId());
            this.abstractConfig.save(data);
            return;
        }

        final PlayerData finalData = data;
        UUIDFetcher.lookupUUID(targetName, this.plugin, uuid -> {
            if (uuid == null && sender != null) { // null is passed in api
                NametagMessages.UUID_LOOKUP_FAILED.send(sender);
            } else {
                this.storePlayerData(uuid, finalData);
                finalData.setUuid(uuid);
                this.abstractConfig.save(finalData);
            }
        });
    }

}