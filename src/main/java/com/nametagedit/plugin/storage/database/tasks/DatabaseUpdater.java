package com.nametagedit.plugin.storage.database.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.NametagHandler;
import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DatabaseUpdater extends BukkitRunnable {

    private final NametagHandler handler;
    private final HikariDataSource hikari;
    private final NametagEdit plugin;

    private static final int CURRENT_DATABASE_VERSION = 5;

    @Override
    public void run() {
        try (Connection connection = this.hikari.getConnection()) {
            int currentVersion = this.getCurrentDatabaseVersion(connection);

            this.createTablesIfNotExists(connection);

            while (currentVersion < DatabaseUpdater.CURRENT_DATABASE_VERSION) {
                switch (currentVersion) {
                case 1:
                    this.handleUpdate1(connection);
                    break;
                case 2:
                    this.handleUpdate2(connection);
                    break;
                case 3:
                    this.handleUpdate3(connection);
                    break;
                case 4:
                    this.handleUpdate4(connection);
                    break;
                }

                currentVersion++;
            }

            this.setCurrentDatabaseVersion(connection, DatabaseUpdater.CURRENT_DATABASE_VERSION);
        } catch (final SQLException e) {
            e.printStackTrace();
        } finally {
            new DataDownloader(this.handler, this.hikari).runTaskAsynchronously(this.plugin);
        }
    }

    private void createTablesIfNotExists(final Connection connection) {
        this.execute(connection, "CREATE TABLE IF NOT EXISTS " + DatabaseConfig.TABLE_CONFIG
                + " (`setting` varchar(16) NOT NULL, `value` varchar(200) NOT NULL, PRIMARY KEY (`setting`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
        this.execute(connection, "CREATE TABLE IF NOT EXISTS " + DatabaseConfig.TABLE_GROUPS
                + " (`name` varchar(64) NOT NULL, `permission` varchar(64) DEFAULT NULL, `prefix` varchar(256) NOT NULL, `suffix` varchar(256) NOT NULL, `priority` int(11) NOT NULL, PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
        this.execute(connection, "CREATE TABLE IF NOT EXISTS " + DatabaseConfig.TABLE_PLAYERS
                + " (`uuid` varchar(64) NOT NULL, `name` varchar(16) NOT NULL, `prefix` varchar(256) NOT NULL, `suffix` varchar(256) NOT NULL, `priority` int(11) NOT NULL, PRIMARY KEY (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
    }

    private void handleUpdate1(final Connection connection) {
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_PLAYERS + " ADD `priority` INT NOT NULL");
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_GROUPS + " ADD `priority` INT NOT NULL");
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_GROUPS + " MODIFY `permission` VARCHAR(64)");
    }

    private void handleUpdate2(final Connection connection) {
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_GROUPS
                + " CHANGE `prefix` `prefix` VARCHAR(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;");
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_GROUPS
                + " CHANGE `suffix` `suffix` VARCHAR(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;");
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_PLAYERS
                + " CHANGE `prefix` `prefix` VARCHAR(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;");
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_PLAYERS
                + " CHANGE `suffix` `suffix` VARCHAR(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;");
    }

    private void handleUpdate3(final Connection connection) {
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_GROUPS + " CONVERT TO CHARACTER SET utf8;");
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_PLAYERS + " CONVERT TO CHARACTER SET utf8;");

        // TODO: Queries for Issue #230.
    }

    private void handleUpdate4(final Connection connection) {
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_GROUPS + " CHANGE `prefix` `prefix` VARCHAR(256);");
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_GROUPS + " CHANGE `suffix` `suffix` VARCHAR(256);");
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_PLAYERS + " CHANGE `prefix` `prefix` VARCHAR(256);");
        this.execute(connection, "ALTER TABLE " + DatabaseConfig.TABLE_PLAYERS + " CHANGE `suffix` `suffix` VARCHAR(256);");
    }

    private void setCurrentDatabaseVersion(final Connection connection, final int currentVersion) {
        try (PreparedStatement select = connection.prepareStatement(
                "INSERT INTO " + DatabaseConfig.TABLE_CONFIG + " VALUES('db_version', ?) ON DUPLICATE KEY UPDATE `value`=?")) {
            select.setInt(1, currentVersion);
            select.setInt(2, currentVersion);
            select.execute();
        } catch (final SQLException e) {
            this.handleError(e);
        }
    }

    private int getCurrentDatabaseVersion(final Connection connection) {
        try (PreparedStatement select = connection
                .prepareStatement("SELECT `value` FROM " + DatabaseConfig.TABLE_CONFIG + " WHERE `setting`='db_version'")) {
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("value");
                }
            }
        } catch (final SQLException e) {
            this.handleError(e);
        }

        return 1;
    }

    private void execute(final Connection connection, final String query) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.execute();
        } catch (final SQLException e) {
            this.handleError(e);
        }
    }

    private void handleError(final SQLException e) {
        if (this.handler.isDebug()) {
            e.printStackTrace();
        } else {
            this.plugin.getLogger().severe("NametagEdit Query Failed - Reason: " + e.getMessage());
            this.plugin.getLogger().severe(
                    "If this is not a connection error, please enable debug with /nte debug and post the error on our GitHub Issue Tracker.");
        }
    }

}
