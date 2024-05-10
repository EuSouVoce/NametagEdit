package com.nametagedit.plugin.storage.database.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GroupDeleter extends BukkitRunnable {

    private final String groupName;
    private final HikariDataSource hikari;

    @Override
    public void run() {
        try (Connection connection = this.hikari.getConnection()) {
            final String QUERY = "DELETE FROM " + DatabaseConfig.TABLE_GROUPS + " WHERE `name`=?";
            final PreparedStatement delete = connection.prepareStatement(QUERY);
            delete.setString(1, this.groupName);
            delete.execute();
            delete.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

}