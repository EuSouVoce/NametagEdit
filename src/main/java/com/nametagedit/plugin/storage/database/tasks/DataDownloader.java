package com.nametagedit.plugin.storage.database.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.NametagHandler;
import com.nametagedit.plugin.api.data.GroupData;
import com.nametagedit.plugin.api.data.PlayerData;
import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.nametagedit.plugin.utils.Utils;
import com.zaxxer.hikari.HikariDataSource;

public class DataDownloader extends BukkitRunnable {

    private final List<UUID> players = new ArrayList<>();
    private final NametagHandler handler;
    private final HikariDataSource hikari;

    public DataDownloader(final NametagHandler handler, final HikariDataSource hikari) {
        this.handler = handler;
        this.hikari = hikari;
        for (final Player player : Utils.getOnline()) {
            this.players.add(player.getUniqueId());
        }
    }

    @Override
    public void run() {
        final HashMap<String, String> settings = new HashMap<>();
        List<GroupData> groupDataUnordered = new ArrayList<>();
        final Map<UUID, PlayerData> playerData = new HashMap<>();

        try (Connection connection = this.hikari.getConnection()) {
            ResultSet results = connection
                    .prepareStatement("SELECT `name`, `prefix`, `suffix`, `permission`, `priority` FROM " + DatabaseConfig.TABLE_GROUPS)
                    .executeQuery();

            while (results.next()) {
                groupDataUnordered.add(new GroupData(results.getString("name"), results.getString("prefix"), results.getString("suffix"),
                        results.getString("permission"), new Permission(results.getString("permission"), PermissionDefault.FALSE),
                        results.getInt("priority")));
            }

            final PreparedStatement select = connection.prepareStatement(
                    "SELECT `uuid`, `prefix`, `suffix`, `priority` FROM " + DatabaseConfig.TABLE_PLAYERS + " WHERE uuid=?");
            for (final UUID uuid : this.players) {
                select.setString(1, uuid.toString());
                results = select.executeQuery();
                if (results.next()) {
                    playerData.put(uuid, new PlayerData("", uuid, Utils.format(results.getString("prefix"), true),
                            Utils.format(results.getString("suffix"), true), results.getInt("priority")));
                }
            }

            results = connection.prepareStatement("SELECT `setting`,`value` FROM " + DatabaseConfig.TABLE_CONFIG).executeQuery();
            while (results.next()) {
                settings.put(results.getString("setting"), results.getString("value"));
            }

            select.close();
            results.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        } finally {
            // First order the groups before performing assignments
            final String orderSetting = settings.get("order");
            if (orderSetting != null) {
                final String[] order = orderSetting.split(" ");
                // This will be the order we will use
                final List<GroupData> current = new ArrayList<>();
                // Determine order for current loaded groups
                for (final String group : order) {
                    final Iterator<GroupData> itr = groupDataUnordered.iterator();
                    while (itr.hasNext()) {
                        final GroupData groupData = itr.next();
                        if (groupData.getGroupName().equalsIgnoreCase(group)) {
                            current.add(groupData);
                            itr.remove();
                            break;
                        }
                    }
                }

                current.addAll(groupDataUnordered); // Add remaining entries (bad order, wasn't specified)
                groupDataUnordered = current; // Reassign the new group order
            }

            this.handler.assignData(groupDataUnordered, playerData); // Safely perform assignments
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (final Player player : Utils.getOnline()) {
                        final PlayerData data = playerData.get(player.getUniqueId());
                        if (data != null) {
                            data.setName(player.getName());
                        }
                    }

                    DataDownloader.this.handler.applyTags();
                }
            }.runTask(this.handler.getPlugin());
        }
    }

}