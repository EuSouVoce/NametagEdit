package com.nametagedit.plugin.storage.database.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.api.data.GroupData;
import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GroupAdd extends BukkitRunnable {

    private final GroupData groupData;
    private final HikariDataSource hikari;

    @Override
    public void run() {
        try (Connection connection = this.hikari.getConnection()) {
            final String QUERY = "INSERT INTO " + DatabaseConfig.TABLE_GROUPS + " VALUES(?, ?, ?, ?, ?)";
            final PreparedStatement insert = connection.prepareStatement(QUERY);
            insert.setString(1, this.groupData.getGroupName());
            insert.setString(2, this.groupData.getPermission());
            insert.setString(3, this.groupData.getPrefix());
            insert.setString(4, this.groupData.getSuffix());
            insert.setInt(5, this.groupData.getSortPriority());
            insert.execute();
            insert.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

}