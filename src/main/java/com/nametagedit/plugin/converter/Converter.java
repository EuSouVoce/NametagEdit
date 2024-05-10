package com.nametagedit.plugin.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.NametagMessages;
import com.nametagedit.plugin.utils.Utils;

/**
 * Converts from v2.4 of NametagEdit to use the new storage method introduced in
 * v3.0.
 */
public class Converter {

    private List<String> getLines(final File file) {
        final List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (final IOException e) {
            return new ArrayList<>();
        }
    }

    private List<String> getLines(final CommandSender commandSender, final Plugin plugin, final String oldFileName) {
        final File oldFile = new File(plugin.getDataFolder(), oldFileName);
        if (!oldFile.exists()) {
            NametagMessages.FILE_DOESNT_EXIST.send(commandSender, oldFileName);
            return new ArrayList<>();
        }

        return this.getLines(oldFile);
    }

    public void legacyConversion(final CommandSender sender, final Plugin plugin) {
        try {
            this.handleFile(plugin, sender, "groups");
            this.handleFile(plugin, sender, "players");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void handleFile(final Plugin plugin, final CommandSender sender, final String fileType) throws IOException {
        final boolean GROUP = fileType.equals("groups");
        final File nametagConfigFile = new File(plugin.getDataFolder(), fileType + ".yml");
        final YamlConfiguration nametagConfig = Utils.getConfig(nametagConfigFile);
        for (final String line : this.getLines(sender, plugin, fileType + ".txt")) {
            if (!line.contains("="))
                continue; // If the special token is missing, skip. Malformed line.
            if (GROUP) {
                this.handleGroup(nametagConfig, line);
            } else {
                this.handlePlayer(nametagConfig, line);
            }
        }

        nametagConfig.save(nametagConfigFile);
    }

    private void handleGroup(final YamlConfiguration config, final String line) {
        final String[] lineContents = line.replace("=", "").split(" ");
        final String[] permissionSplit = lineContents[0].split("\\.");
        final String group = WordUtils.capitalizeFully(permissionSplit[permissionSplit.length - 1]);
        final String permission = lineContents[0];
        final String type = lineContents[1];
        String value = line.substring(line.indexOf("\"") + 1);
        value = value.substring(0, value.indexOf("\""));
        config.set("Groups." + group + ".Permission", permission);
        config.set("Groups." + group + ".SortPriority", -1);
        if (type.equals("prefix")) {
            config.set("Groups." + group + ".Prefix", value);
        } else {
            config.set("Groups." + group + ".Suffix", value);
        }
    }

    private void handlePlayer(final YamlConfiguration config, final String line) {
        final String[] initialSplit = line.split("=");
        final String prefix = initialSplit[1].trim().split("\"")[1];
        final String[] whiteSpaces = initialSplit[0].trim().split(" ");
        final String playerName = whiteSpaces[0];
        final String type = whiteSpaces[1];
        OfflinePlayer offlinePlayer = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(offlinePlayerFromBukkitCache -> playerName.equals(offlinePlayerFromBukkitCache.getName())).findFirst().orElse(null);
        Boolean nullExcp = false;
        if (offlinePlayer == null) {
            nullExcp = true;
            offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        }

        final String uuid = offlinePlayer.getUniqueId().toString();
        config.set("Players." + uuid + ".Name", playerName);
        config.set("Players." + uuid + "." + type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase(), prefix);
        config.set("Players." + uuid + ".SortPriority", -1);
        if (nullExcp)
            NametagEdit.getInstance().getLogger().info("The player " + playerName
                    + " is not present at the usercache of bukkit. Using deprecated 'Bukkit.getOfflinePlayer(playerName)' function to proceed");

    }

}