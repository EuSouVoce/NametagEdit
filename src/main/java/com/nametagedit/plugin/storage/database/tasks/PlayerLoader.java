package com.nametagedit.plugin.storage.database.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.NametagHandler;
import com.nametagedit.plugin.api.data.PlayerData;
import com.nametagedit.plugin.storage.database.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PlayerLoader extends BukkitRunnable {

    private final UUID uuid;
    private final Plugin plugin;
    private final NametagHandler handler;
    private final HikariDataSource hikari;
    private final boolean loggedIn;

    @Override
    public void run() {
        String tempPrefix = null;
        String tempSuffix = null;
        int priority = -1;
        boolean found = false;

        try (Connection connection = this.hikari.getConnection()) {
            final String QUERY = "SELECT `prefix`, `suffix`, `priority` FROM " + DatabaseConfig.TABLE_PLAYERS + " WHERE `uuid`=?";

            try (PreparedStatement select = connection.prepareStatement(QUERY)) {
                select.setString(1, this.uuid.toString());

                final ResultSet resultSet = select.executeQuery();
                if (resultSet.next()) {
                    tempPrefix = resultSet.getString("prefix");
                    tempSuffix = resultSet.getString("suffix");
                    priority = resultSet.getInt("priority");
                    found = true;
                }

                resultSet.close();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        } finally {
            final String prefix = tempPrefix == null ? "" : tempPrefix;
            final String suffix = tempSuffix == null ? "" : tempSuffix;
            final boolean finalFound = found;

            final int finalPriority = priority;
            new BukkitRunnable() {
                @Override
                public void run() {
                    final Player player = Bukkit.getPlayer(PlayerLoader.this.uuid);
                    if (player != null) {
                        if (finalFound) {
                            PlayerData data = PlayerLoader.this.handler.getPlayerData(player);
                            if (data == null) {
                                data = new PlayerData(player.getName(), player.getUniqueId(), prefix, suffix, finalPriority);
                                PlayerLoader.this.handler.storePlayerData(player.getUniqueId(), data);
                            } else {
                                data.setPrefix(prefix);
                                data.setSuffix(suffix);
                            }
                        }

                        PlayerLoader.this.handler.applyTagToPlayer(player, PlayerLoader.this.loggedIn);
                    }
                }
            }.runTask(this.plugin);
        }
    }

}