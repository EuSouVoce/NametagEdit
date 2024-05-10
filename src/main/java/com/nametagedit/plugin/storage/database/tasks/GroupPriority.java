package com.nametagedit.plugin.storage.database.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GroupPriority extends BukkitRunnable {

    private final String group;
    private final int priority;
    private final HikariDataSource hikari;

    @Override
    public void run() {
        try (Connection connection = this.hikari.getConnection()) {
            final PreparedStatement preparedStatement = connection
                    .prepareStatement("UPDATE " + DatabaseConfig.TABLE_GROUPS + " SET `priority`=? WHERE `name`=?");
            preparedStatement.setInt(1, this.priority);
            preparedStatement.setString(2, this.group);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

}