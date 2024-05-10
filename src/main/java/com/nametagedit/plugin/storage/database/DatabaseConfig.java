package com.nametagedit.plugin.storage.database;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.NametagHandler;
import com.nametagedit.plugin.api.data.GroupData;
import com.nametagedit.plugin.api.data.PlayerData;
import com.nametagedit.plugin.storage.AbstractConfig;
import com.nametagedit.plugin.storage.database.tasks.DataDownloader;
import com.nametagedit.plugin.storage.database.tasks.DatabaseUpdater;
import com.nametagedit.plugin.storage.database.tasks.GroupAdd;
import com.nametagedit.plugin.storage.database.tasks.GroupConfigUpdater;
import com.nametagedit.plugin.storage.database.tasks.GroupDeleter;
import com.nametagedit.plugin.storage.database.tasks.GroupPriority;
import com.nametagedit.plugin.storage.database.tasks.GroupSaver;
import com.nametagedit.plugin.storage.database.tasks.PlayerDeleter;
import com.nametagedit.plugin.storage.database.tasks.PlayerLoader;
import com.nametagedit.plugin.storage.database.tasks.PlayerPriority;
import com.nametagedit.plugin.storage.database.tasks.PlayerSaver;
import com.nametagedit.plugin.utils.Configuration;
import com.nametagedit.plugin.utils.UUIDFetcher;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseConfig implements AbstractConfig {

    private final NametagEdit plugin;
    private final NametagHandler handler;
    private HikariDataSource hikari;

    // These are used if the user wants to customize the
    // schema structure. Perhaps more cosmetic.
    public static String TABLE_GROUPS;
    public static String TABLE_PLAYERS;
    public static String TABLE_CONFIG;

    public DatabaseConfig(final NametagEdit plugin, final NametagHandler handler, final Configuration config) {
        this.plugin = plugin;
        this.handler = handler;
        DatabaseConfig.TABLE_GROUPS = "`" + config.getString("MySQL.GroupsTable", "nte_groups") + "`";
        DatabaseConfig.TABLE_PLAYERS = "`" + config.getString("MySQL.PlayersTable", "nte_players") + "`";
        DatabaseConfig.TABLE_CONFIG = "`" + config.getString("MySQL.ConfigTable", "nte_config") + "`";
    }

    @Override
    public void load() {
        final FileConfiguration config = this.handler.getConfig();
        this.shutdown();
        this.hikari = new HikariDataSource();
        this.hikari.setMaximumPoolSize(config.getInt("MinimumPoolSize", 10));
        this.hikari.setPoolName("NametagEdit Pool");

        String port = "3306";

        if (config.isSet("MySQL.Port")) {
            port = config.getString("MySQL.Port");
        }

        this.hikari
                .setJdbcUrl("jdbc:mysql://" + config.getString("MySQL.Hostname") + ":" + port + "/" + config.getString("MySQL.Database"));
        this.hikari.addDataSourceProperty("useSSL", false);
        this.hikari.addDataSourceProperty("requireSSL", false);
        this.hikari.addDataSourceProperty("verifyServerCertificate", false);
        this.hikari.addDataSourceProperty("user", config.getString("MySQL.Username"));
        this.hikari.addDataSourceProperty("password", config.getString("MySQL.Password"));

        this.hikari.setUsername(config.getString("MySQL.Username"));
        this.hikari.setPassword(config.getString("MySQL.Password"));

        new DatabaseUpdater(this.handler, this.hikari, this.plugin).runTaskAsynchronously(this.plugin);
    }

    @Override
    public void reload() { new DataDownloader(this.handler, this.hikari).runTaskAsynchronously(this.plugin); }

    @Override
    public void shutdown() {
        if (this.hikari != null) {
            this.hikari.close();
        }
    }

    @Override
    public void load(final Player player, final boolean loggedIn) {
        new PlayerLoader(player.getUniqueId(), this.plugin, this.handler, this.hikari, loggedIn).runTaskAsynchronously(this.plugin);
    }

    @Override
    public void save(final PlayerData... playerData) { new PlayerSaver(playerData, this.hikari).runTaskAsynchronously(this.plugin); }

    @Override
    public void save(final GroupData... groupData) { new GroupSaver(groupData, this.hikari).runTaskAsynchronously(this.plugin); }

    @Override
    public void savePriority(final boolean playerTag, final String key, final int priority) {
        if (playerTag) {
            UUIDFetcher.lookupUUID(key, this.plugin, uuid -> {
                if (uuid != null) {
                    new PlayerPriority(uuid, priority, this.hikari).runTaskAsynchronously(this.plugin);
                } else {
                    this.plugin.getLogger().severe("An error has occurred while looking for UUID.");
                }
            });
        } else {
            new GroupPriority(key, priority, this.hikari).runTaskAsynchronously(this.plugin);
        }
    }

    @Override
    public void delete(final GroupData groupData) {
        new GroupDeleter(groupData.getGroupName(), this.hikari).runTaskAsynchronously(this.plugin);
    }

    @Override
    public void add(final GroupData groupData) { new GroupAdd(groupData, this.hikari).runTaskAsynchronously(this.plugin); }

    @Override
    public void clear(final UUID uuid, final String targetName) { new PlayerDeleter(uuid, this.hikari).runTaskAsynchronously(this.plugin); }

    @Override
    public void orderGroups(final CommandSender commandSender, final List<String> order) {
        String formatted = Arrays.toString(order.toArray());
        formatted = formatted.substring(1, formatted.length() - 1).replace(",", "");
        new GroupConfigUpdater("order", formatted, this.hikari).runTaskAsynchronously(this.handler.getPlugin());
    }

}
