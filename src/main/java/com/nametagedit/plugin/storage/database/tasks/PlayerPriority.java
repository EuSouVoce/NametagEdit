package com.nametagedit.plugin.storage.database.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PlayerPriority extends BukkitRunnable {

    private final UUID player;
    private final int priority;
    private final HikariDataSource hikari;

    @Override
    public void run() {
        try (Connection connection = this.hikari.getConnection()) {
            final PreparedStatement preparedStatement = connection
                    .prepareStatement("UPDATE " + DatabaseConfig.TABLE_PLAYERS + " SET `priority`=? WHERE `uuid`=?");
            preparedStatement.setInt(1, this.priority);
            preparedStatement.setString(2, this.player.toString());
            preparedStatement.execute();
            preparedStatement.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

}