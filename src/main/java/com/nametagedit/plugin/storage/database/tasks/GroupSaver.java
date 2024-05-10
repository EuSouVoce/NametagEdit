package com.nametagedit.plugin.storage.database.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.api.data.GroupData;
import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.nametagedit.plugin.utils.Utils;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GroupSaver extends BukkitRunnable {

    private final GroupData[] groupData;
    private final HikariDataSource hikari;

    @Override
    public void run() {
        try (Connection connection = this.hikari.getConnection()) {
            final String QUERY = "UPDATE " + DatabaseConfig.TABLE_GROUPS
                    + " SET `prefix`=?, `suffix`=?, `permission`=?, `priority`=? WHERE `name`=?";
            final PreparedStatement update = connection.prepareStatement(QUERY);

            for (final GroupData groupData : this.groupData) {
                update.setString(1, Utils.deformat(groupData.getPrefix()));
                update.setString(2, Utils.deformat(groupData.getSuffix()));
                update.setString(3, groupData.getPermission());
                update.setInt(4, groupData.getSortPriority());
                update.setString(5, groupData.getGroupName());
                update.addBatch();
            }

            update.executeBatch();
            update.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

}