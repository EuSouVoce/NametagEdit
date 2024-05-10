package com.nametagedit.plugin.converter;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.NametagMessages;
import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.nametagedit.plugin.utils.Utils;

import lombok.AllArgsConstructor;

/**
 * This class converts to and from Flatfile and MySQL
 */
@AllArgsConstructor
public class ConverterTask extends BukkitRunnable {

    private final boolean databaseToFile;
    private final CommandSender sender;
    private final NametagEdit plugin;

    @Override
    public void run() {
        final FileConfiguration config = this.plugin.getHandler().getConfig();
        final String connectionString = "jdbc:mysql://" + config.getString("MySQL.Hostname") + ":" + config.getInt("MySQL.Port") + "/"
                + config.getString("MySQL.Database");
        try (Connection connection = DriverManager.getConnection(connectionString, config.getString("MySQL.Username"),
                config.getString("MySQL.Password"))) {
            if (this.databaseToFile) {
                this.convertDatabaseToFile(connection);
            } else {
                this.convertFilesToDatabase(connection);
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        } finally {
            new BukkitRunnable() {
                @Override
                public void run() { ConverterTask.this.plugin.getHandler().reload(); }
            }.runTask(this.plugin);
        }
    }

    private void convertDatabaseToFile(final Connection connection) {
        try {
            final String GROUP_QUERY = "SELECT name, prefix, suffix, permission, priority FROM " + DatabaseConfig.TABLE_GROUPS;
            final String PLAYER_QUERY = "SELECT name, uuid, prefix, suffix, priority FROM " + DatabaseConfig.TABLE_PLAYERS;

            final File groupsFile = new File(this.plugin.getDataFolder(), "groups_CONVERTED.yml");
            final File playersFile = new File(this.plugin.getDataFolder(), "players_CONVERTED.yml");

            final YamlConfiguration groups = Utils.getConfig(groupsFile);
            final YamlConfiguration players = Utils.getConfig(playersFile);

            ResultSet results = connection.prepareStatement(GROUP_QUERY).executeQuery();
            while (results.next()) {
                groups.set("Groups." + results.getString("name") + ".Permission", results.getString("permission"));
                groups.set("Groups." + results.getString("name") + ".Prefix", results.getString("prefix"));
                groups.set("Groups." + results.getString("name") + ".Suffix", results.getString("suffix"));
                groups.set("Groups." + results.getString("name") + ".SortPriority", results.getInt("priority"));
            }

            results = connection.prepareStatement(PLAYER_QUERY).executeQuery();
            while (results.next()) {
                players.set("Players." + results.getString("uuid") + ".Name", results.getString("name"));
                players.set("Players." + results.getString("uuid") + ".Prefix", results.getString("prefix"));
                players.set("Players." + results.getString("uuid") + ".Suffix", results.getString("suffix"));
                players.set("Players." + results.getString("uuid") + ".SortPriority", results.getInt("priority"));
            }

            results.close();
            groups.save(groupsFile);
            players.save(playersFile);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void convertFilesToDatabase(final Connection connection) {
        final File groupsFile = new File(this.plugin.getDataFolder(), "groups.yml");
        final File playersFile = new File(this.plugin.getDataFolder(), "players.yml");

        final YamlConfiguration groups = Utils.getConfig(groupsFile);
        final YamlConfiguration players = Utils.getConfig(playersFile);

        if (players != null && this.checkValid(players, "Players")) {
            // Import the player entries from the file
            try (PreparedStatement playerInsert = connection.prepareStatement("INSERT INTO " + DatabaseConfig.TABLE_PLAYERS
                    + " VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `prefix`=?, `suffix`=?")) {
                for (final String key : players.getConfigurationSection("Players").getKeys(false)) {
                    playerInsert.setString(1, key);
                    playerInsert.setString(2, players.getString("Players." + key + ".Name"));
                    playerInsert.setString(3, Utils.deformat(players.getString("Players." + key + ".Prefix", "")));
                    playerInsert.setString(4, Utils.deformat(players.getString("Players." + key + ".Suffix", "")));
                    playerInsert.setString(5, players.getString("Players." + key + ".SortPriority"));
                    playerInsert.setString(6, Utils.deformat(players.getString("Players." + key + ".Prefix", "")));
                    playerInsert.setString(7, Utils.deformat(players.getString("Players." + key + ".Suffix", "")));
                    playerInsert.addBatch();
                }

                playerInsert.executeBatch();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }

        if (groups != null && this.checkValid(groups, "Groups")) {
            // Import the player entries from the file
            try (PreparedStatement groupInsert = connection.prepareStatement("INSERT INTO " + DatabaseConfig.TABLE_GROUPS
                    + " VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `prefix`=?, `suffix`=?, `permission`=?")) {
                for (final String key : groups.getConfigurationSection("Groups").getKeys(false)) {
                    groupInsert.setString(1, key);
                    groupInsert.setString(2, groups.getString("Groups." + key + ".Permission"));
                    groupInsert.setString(3, Utils.deformat(groups.getString("Groups." + key + ".Prefix", "")));
                    groupInsert.setString(4, Utils.deformat(groups.getString("Groups." + key + ".Suffix", "")));
                    groupInsert.setString(5, groups.getString("Groups." + key + ".SortPriority"));
                    groupInsert.setString(6, Utils.deformat(groups.getString("Groups." + key + ".Prefix", "")));
                    groupInsert.setString(7, Utils.deformat(groups.getString("Groups." + key + ".Suffix", "")));
                    groupInsert.setString(8, groups.getString("Groups." + key + ".Permission"));
                    groupInsert.addBatch();
                }

                groupInsert.executeBatch();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkValid(final FileConfiguration configuration, final String section) {
        if (!configuration.contains(section)) {
            NametagMessages.FILE_MISCONFIGURED.send(this.sender, section + ".yml");
            return false;
        }

        return true;
    }

}