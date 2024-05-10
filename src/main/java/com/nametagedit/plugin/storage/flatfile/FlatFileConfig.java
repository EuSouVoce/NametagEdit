package com.nametagedit.plugin.storage.flatfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.NametagHandler;
import com.nametagedit.plugin.api.data.GroupData;
import com.nametagedit.plugin.api.data.PlayerData;
import com.nametagedit.plugin.storage.AbstractConfig;
import com.nametagedit.plugin.utils.UUIDFetcher;
import com.nametagedit.plugin.utils.Utils;

public class FlatFileConfig implements AbstractConfig {

    private File groupsFile;
    private File playersFile;

    private YamlConfiguration groups;
    private YamlConfiguration players;

    private final NametagEdit plugin;
    private final NametagHandler handler;

    public FlatFileConfig(final NametagEdit plugin, final NametagHandler handler) {
        this.plugin = plugin;
        this.handler = handler;
    }

    @Override
    public void load() {
        this.groupsFile = new File(this.plugin.getDataFolder(), "groups.yml");
        this.groups = Utils.getConfig(this.groupsFile, "groups.yml", this.plugin);
        this.playersFile = new File(this.plugin.getDataFolder(), "players.yml");
        this.players = Utils.getConfig(this.playersFile, "players.yml", this.plugin);
        this.loadGroups();
        this.loadPlayers();

        new BukkitRunnable() {
            @Override
            public void run() { FlatFileConfig.this.handler.applyTags(); }
        }.runTask(this.plugin);
    }

    @Override
    public void reload() {
        this.handler.clearMemoryData();

        new BukkitRunnable() {
            @Override
            public void run() { FlatFileConfig.this.load(); }
        }.runTaskAsynchronously(this.plugin);
    }

    @Override
    public void shutdown() {
        // NOTE: Nothing to do
    }

    @Override
    public void load(final Player player, final boolean loggedIn) {
        this.loadPlayerTag(player);
        this.plugin.getHandler().applyTagToPlayer(player, loggedIn);
    }

    @Override
    public void save(final PlayerData... data) {
        for (final PlayerData playerData : data) {
            final UUID uuid = playerData.getUuid();
            final String name = playerData.getName();
            this.players.set("Players." + uuid + ".Name", name);
            this.players.set("Players." + uuid + ".Prefix", Utils.deformat(playerData.getPrefix()));
            this.players.set("Players." + uuid + ".Suffix", Utils.deformat(playerData.getSuffix()));
            this.players.set("Players." + uuid + ".SortPriority", playerData.getSortPriority());
        }

        this.save(this.players, this.playersFile);
    }

    @Override
    public void save(final GroupData... data) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final GroupData groupData : data) {
                    FlatFileConfig.this.storeGroup(groupData);
                }

                FlatFileConfig.this.save(FlatFileConfig.this.groups, FlatFileConfig.this.groupsFile);
            }
        }.runTaskAsynchronously(this.plugin);
    }

    @Override
    public void savePriority(final boolean playerTag, final String key, final int priority) {
        if (playerTag) {
            final Player target = Bukkit.getPlayerExact(key);
            if (target != null) {
                if (this.players.contains("Players." + target.getUniqueId().toString())) {
                    this.players.set("Players." + target.getUniqueId().toString(), priority);
                    this.save(this.players, this.playersFile);
                }
                return;
            }

            UUIDFetcher.lookupUUID(key, this.plugin, new UUIDFetcher.UUIDLookup() {
                @Override
                public void response(final UUID uuid) {
                    if (FlatFileConfig.this.players.contains("Players." + uuid.toString())) {
                        FlatFileConfig.this.players.set("Players." + uuid, priority);
                        FlatFileConfig.this.save(FlatFileConfig.this.players, FlatFileConfig.this.playersFile);
                    }
                }
            });
        }
    }

    @Override
    public void delete(final GroupData groupData) {
        this.groups.set("Groups." + groupData.getGroupName(), null);
        this.save(this.groups, this.groupsFile);
    }

    @Override
    public void add(final GroupData groupData) {
        // NOTE: Nothing to do
    }

    @Override
    public void clear(final UUID uuid, final String targetName) {
        this.handler.removePlayerData(uuid);
        this.players.set("Players." + uuid.toString(), null);
        this.save(this.players, this.playersFile);
    }

    @Override
    public void orderGroups(final CommandSender commandSender, final List<String> order) {
        this.groups.set("Groups", null);
        for (final String set : order) {
            final GroupData groupData = this.handler.getGroupData(set);
            if (groupData != null) {
                this.storeGroup(groupData);
            }
        }

        for (final GroupData groupData : this.handler.getGroupData()) {
            if (!this.groups.contains("Groups." + groupData.getGroupName())) {
                this.storeGroup(groupData);
            }
        }

        this.save(this.groups, this.groupsFile);
    }

    private void save(final YamlConfiguration config, final File file) {
        try {
            config.save(file);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlayerTag(final Player player) {
        final PlayerData data = PlayerData.fromFile(player.getUniqueId().toString(), this.players);
        if (data != null) {
            data.setName(player.getName());
            this.handler.storePlayerData(player.getUniqueId(), data);
        }
    }

    private void loadPlayers() {
        for (final Player player : Utils.getOnline()) {
            this.loadPlayerTag(player);
        }
    }

    private void loadGroups() {
        final List<GroupData> groupData = new ArrayList<>();
        for (final String groupName : this.groups.getConfigurationSection("Groups").getKeys(false)) {
            final GroupData data = new GroupData();
            data.setGroupName(groupName);
            data.setPermission(this.groups.getString("Groups." + groupName + ".Permission", "nte.default"));
            data.setPrefix(this.groups.getString("Groups." + groupName + ".Prefix", ""));
            data.setSuffix(this.groups.getString("Groups." + groupName + ".Suffix", ""));
            data.setSortPriority(this.groups.getInt("Groups." + groupName + ".SortPriority", -1));
            groupData.add(data);
        }

        this.handler.assignGroupData(groupData);
    }

    private void storeGroup(final GroupData groupData) {
        this.groups.set("Groups." + groupData.getGroupName() + ".Permission", groupData.getPermission());
        this.groups.set("Groups." + groupData.getGroupName() + ".Prefix", Utils.deformat(groupData.getPrefix()));
        this.groups.set("Groups." + groupData.getGroupName() + ".Suffix", Utils.deformat(groupData.getSuffix()));
        this.groups.set("Groups." + groupData.getGroupName() + ".SortPriority", groupData.getSortPriority());
    }

}