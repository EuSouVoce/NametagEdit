package com.nametagedit.plugin.api.data;

import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents a player nametag. There are several properties
 * available.
 */
@Getter
@Setter
@AllArgsConstructor
public class PlayerData implements INametag {

    private String name;
    private UUID uuid;
    private String prefix;
    private String suffix;
    private int sortPriority;

    public PlayerData() {

    }

    public static PlayerData fromFile(final String key, final YamlConfiguration file) {
        if (!file.contains("Players." + key))
            return null;
        final PlayerData data = new PlayerData();
        data.setUuid(UUID.fromString(key));
        data.setName(file.getString("Players." + key + ".Name"));
        data.setPrefix(file.getString("Players." + key + ".Prefix", ""));
        data.setSuffix(file.getString("Players." + key + ".Suffix", ""));
        data.setSortPriority(file.getInt("Players." + key + ".SortPriority", -1));
        return data;
    }

    @Override
    public boolean isPlayerTag() { return true; }

}