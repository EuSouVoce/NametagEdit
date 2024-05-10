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
public class PlayerDeleter extends BukkitRunnable {

    private final UUID uuid;
    private final HikariDataSource hikari;

    @Override
    public void run() {
        try (Connection connection = this.hikari.getConnection()) {
            final String QUERY = "DELETE FROM " + DatabaseConfig.TABLE_PLAYERS + " WHERE `uuid`=?";
            final PreparedStatement delete = connection.prepareStatement(QUERY);
            delete.setString(1, this.uuid.toString());
            delete.execute();
            delete.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

}