package com.nametagedit.plugin.storage.database.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GroupConfigUpdater extends BukkitRunnable {

    private final String setting;
    private final String value;
    private final HikariDataSource hikari;

    @Override
    public void run() {
        try (Connection connection = this.hikari.getConnection()) {
            final String QUERY = "INSERT INTO " + DatabaseConfig.TABLE_GROUPS + " VALUES(?, ?) ON DUPLICATE KEY UPDATE `value`=?";
            final PreparedStatement update = connection.prepareStatement(QUERY);
            update.setString(1, this.setting);
            update.setString(2, this.value);
            update.setString(3, this.value);
            update.execute();
            update.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

}