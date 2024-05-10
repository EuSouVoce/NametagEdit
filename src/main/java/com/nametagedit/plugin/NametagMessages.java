package com.nametagedit.plugin;

import org.bukkit.command.CommandSender;

import com.nametagedit.plugin.utils.Utils;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum NametagMessages {

    SET_PRIORITY("Set sort priority to %s for %s"), CLEARED_TEAMS("Empty teams: %s. Purge: %s."),
    DEBUG_TOGGLED("NametagEdit debug has been %s"), LONG_TAGS("Long Nametags has been %s"), GROUP_EXISTS("The group %s already exists"),
    GROUP_VALUE_CLEARED("Cleared the %s for the group %s"), GROUP_EXISTS_NOT("The group %s does not exist!"),
    GROUP_VALUE("Changed %s's %s to %s"), USAGE_CONVERT("Usage: /nte convert <file/db> <file/db> <legacy (true/false)>"),
    GROUP_REMOVED("Successfully removed group %s"), MODIFY_OWN_TAG("You can only modify your own tag."),
    NO_PERMISSION("You do not have permission to use this."), RELOADED_DATA("Successfully reloaded plugin data"),
    FILE_DOESNT_EXIST("The file %s does not exist"), UUID_LOOKUP_FAILED("Could not find the uuid for %s"),
    CREATED_GROUP("Created group %s"), NOT_A_NUMBER("Uh-oh! %s does not appear to be a number!"),
    FILE_MISCONFIGURED("The file %s is not properly configured. Please read the configuration guide, otherwise conversion will fail."),
    CONVERSION("Attempting to convert %s from %s to %s. (Legacy: %s)");

    private final String text;

    @Override
    public String toString() { return Utils.color("&8» &a" + this.text); }

    public void send(final CommandSender sender) { sender.sendMessage(this.toString()); }

    public void send(final CommandSender sender, final String replacement) {
        sender.sendMessage(this.toString().replace("%s", Utils.format(replacement)));
    }

    public void send(final CommandSender sender, final Object... object) { sender.sendMessage(String.format(this.toString(), object)); }

}
