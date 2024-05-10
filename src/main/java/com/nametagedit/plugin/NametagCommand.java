package com.nametagedit.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scoreboard.Team;

import com.nametagedit.plugin.api.data.GroupData;
import com.nametagedit.plugin.api.events.NametagEvent;
import com.nametagedit.plugin.converter.Converter;
import com.nametagedit.plugin.converter.ConverterTask;
import com.nametagedit.plugin.utils.Utils;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NametagCommand implements TabExecutor {

    private final NametagHandler handler;

    private List<String> getSuggestions(String argument, final String... array) {
        argument = argument.toLowerCase();
        final List<String> suggestions = new ArrayList<>();
        for (final String suggestion : array) {
            if (suggestion.toLowerCase().startsWith(argument)) {
                suggestions.add(suggestion);
            }
        }
        return suggestions;
    }

    /**
     * Handles auto completions
     */
    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 1) {
            return this.getSuggestions(args[0], "debug", "reload", "convert", "player", "group");
        } else if (args.length == 2 || args.length == 3) {
            if (args[0].equalsIgnoreCase("player")) {
                if (args.length == 2) {
                    final List<String> suggestions = new ArrayList<>();
                    for (final Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            suggestions.add(player.getName());
                        }
                    }
                    return suggestions;
                } else {
                    return this.getSuggestions(args[2], "clear", "prefix", "suffix", "priority");
                }
            } else if (args[0].equalsIgnoreCase("group")) {
                if (args.length == 2) {
                    final List<String> data = new ArrayList<>(this.handler.getGroupData().size() + 4);
                    data.add("list");
                    data.add("add");
                    data.add("remove");
                    data.add("order");
                    for (final GroupData groupData : this.handler.getGroupData()) {
                        data.add(groupData.getGroupName());
                    }

                    return this.getSuggestions(args[1], data.toArray(new String[0]));
                } else {
                    return this.getSuggestions(args[2], "clear", "prefix", "suffix", "permission", "priority");
                }
            }
        }

        return new ArrayList<>();
    }

    /**
     * Base command for NametagEdit. See the Wiki for usage and examples.
     */
    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (this.isNotPermissed(sender, "nametagedit.use"))
            return false;
        if (args.length < 1) {
            this.sendUsage(sender);
        } else {
            switch (args[0].toLowerCase()) {
            case "reload":
                this.cmdReload(sender);
                break;
            case "convert":
                this.cmdConvert(sender, args);
                break;
            case "debug":
                this.handler.toggleDebug();
                NametagMessages.DEBUG_TOGGLED.send(sender, this.handler.debug() ? "&aENABLED" : "&cDISABLED");
                break;
            case "player":
                this.cmdPlayer(sender, args);
                break;
            case "group":
                this.cmdGroups(sender, args);
                break;
            case "longtags":
                this.handler.toggleLongTags();
                NametagMessages.LONG_TAGS.send(sender, this.handler.isLongNametagsEnabled() ? "&aENABLED" : "&cDISABLED");
                break;
            case "teams":
                int emptyTeams = 0;
                final boolean unregister = args.length > 1 && args[1].equalsIgnoreCase("clear");
                for (final Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
                    if (team.getEntries().isEmpty()) {
                        if (unregister) {
                            team.unregister();
                        }
                        emptyTeams++;
                    }
                }

                NametagMessages.CLEARED_TEAMS.send(sender, emptyTeams, unregister);
                break;
            case "priority":
                this.cmdPriority(sender, args);
                break;
            default:
                this.sendUsage(sender);
                break;
            }
        }

        return false;
    }

    private boolean isPermissed(final CommandSender sender, final String permission) { return !this.isNotPermissed(sender, permission); }

    private boolean isNotPermissed(final CommandSender sender, final String permission) {
        if (!sender.hasPermission(permission)) {
            NametagMessages.NO_PERMISSION.send(sender);
            return true;
        }

        return false;
    }

    private void sendUsagePlayer(final CommandSender sender) {
        sender.sendMessage(Utils.format("\n&8» &a&lNametagEdit Player Help &8«"));
        sender.sendMessage(Utils.format("\n\n&8Type a command to get started:"));
        sender.sendMessage(Utils.format("&8» &a/nte player <Player> clear"));
        sender.sendMessage(Utils.format("&8» &a/nte player <Player> prefix <Prefix>"));
        sender.sendMessage(Utils.format("&8» &a/nte player <Player> suffix <Suffix>"));
        sender.sendMessage(Utils.format("&8» &a/nte player <Player> priority <#>"));
    }

    private void sendUsageGroup(final CommandSender sender) {
        sender.sendMessage(Utils.format("\n&8» &a&lNametagEdit Player Help &8«"));
        sender.sendMessage(Utils.format("\n\n&8Type a command to get started:"));
        sender.sendMessage(Utils.format("&8» &a/nte group list"));
        sender.sendMessage(Utils.format("&8» &a/nte group add <Group>"));
        sender.sendMessage(Utils.format("&8» &a/nte group remove <Group>"));
        sender.sendMessage(Utils.format("&8» &a/nte group order <Owner Admin Mod Etc...>"));
        sender.sendMessage(Utils.format("&8» &a/nte group <Group> clear <prefix/suffix>"));
        sender.sendMessage(Utils.format("&8» &a/nte group <Group> prefix <Prefix>"));
        sender.sendMessage(Utils.format("&8» &a/nte group <Group> suffix <Suffix>"));
        sender.sendMessage(Utils.format("&8» &a/nte group <Group> permission <Permission>"));
        sender.sendMessage(Utils.format("&8» &a/nte group <Group> priority <#>"));
    }

    private void sendUsage(final CommandSender sender) {
        sender.sendMessage(Utils.format("\n&8» &a&lNametagEdit Plugin Help &8«"));
        sender.sendMessage(Utils.format("     by Cory and sgtcaze"));
        sender.sendMessage(Utils.format("\n\n&8Type a command to get started:"));
        sender.sendMessage(Utils.format("&8» &a/nte debug"));
        sender.sendMessage(Utils.format("&8» &a/nte reload"));
        sender.sendMessage(Utils.format("&8» &a/nte convert"));
        sender.sendMessage(Utils.format("&8» &a/nte player"));
        sender.sendMessage(Utils.format("&8» &a/nte group"));
        sender.sendMessage(Utils.format("&8» &a/nte priority"));
        sender.sendMessage(Utils.format("&8» &a/nte longtags"));
    }

    /**
     * Handles /nte reload
     */
    private void cmdReload(final CommandSender sender) {
        if (this.isPermissed(sender, "nametagedit.reload")) {
            this.handler.reload();
            NametagMessages.RELOADED_DATA.send(sender);
        }
    }

    /**
     * Handles /nte convert
     */
    private void cmdConvert(final CommandSender sender, final String[] args) {
        if (this.isNotPermissed(sender, "nametagedit.convert"))
            return;
        if (args.length != 4) {
            NametagMessages.USAGE_CONVERT.send(sender);
        } else {
            final boolean sourceIsFile = args[1].equalsIgnoreCase("file");
            final boolean destinationIsSQL = args[2].equalsIgnoreCase("db");
            final boolean legacy = args[3].equalsIgnoreCase("true");
            NametagMessages.CONVERSION.send(sender, "groups & players", sourceIsFile ? "file" : "mysql",
                    destinationIsSQL ? "mysql" : "file", legacy);

            if (sourceIsFile && !destinationIsSQL && legacy) {
                new Converter().legacyConversion(sender, this.handler.getPlugin());
            } else if ((destinationIsSQL && sourceIsFile) || (!sourceIsFile && !destinationIsSQL)) {
                new ConverterTask(!destinationIsSQL, sender, this.handler.getPlugin()).runTaskAsynchronously(this.handler.getPlugin());
            }
        }
    }

    /**
     * Handles /nte priority
     */
    private void cmdPriority(final CommandSender sender, final String[] args) {
        if (this.isNotPermissed(sender, "nametagedit.priority"))
            return;
        // if (args.length == 0) {
        // sender.sendMessage(Utils.format("&a&lNametagEdit &7Sort Priority"));
        // sender.sendMessage(Utils.format("&7This feature allows you to position
        // Nametags in tab."));
        // sender.sendMessage(Utils.format("&a/nte priority view &7view advanced
        // info"));
        // }
        // List<GroupData> copyOfGroups = new ArrayList<>(groupData);
        // Collections.sort(copyOfGroups, new Comparator<GroupData>() {
        // @Override
        // public int compare(GroupData group1, GroupData group2) {
        // return group1.getSortPriority() - group2.getSortPriority();
        // }
        // });
        //
        // int adjustedSortPriority = 1;
        //
        // for (GroupData groupData : copyOfGroups) {
        // groupData.setSortPriority(groupData.getSortPriority() < 1 ? -1 :
        // adjustedSortPriority++);
        // }
        //
        // abstractConfig.save(groupData.toArray(new GroupData[groupData.size()]));
    }

    /**
     * Handles /nte player
     */
    private void cmdPlayer(final CommandSender sender, final String[] args) {
        if (args.length == 3) {
            if (!args[2].equalsIgnoreCase("clear")) {
                this.sendUsagePlayer(sender);
                return;
            }

            if (this.isNotPermissed(sender, "nametagedit.clear.self"))
                return;

            final String targetName = args[1];

            if (!sender.hasPermission("nametagedit.clear.others") && !targetName.equalsIgnoreCase(sender.getName())) {
                NametagMessages.MODIFY_OWN_TAG.send(sender);
                return;
            }

            this.handler.clear(sender, targetName);
            this.handler.applyTagToPlayer(Bukkit.getPlayerExact(targetName), false);
        } else if (args.length >= 4) {
            switch (args[2].toLowerCase()) {
            case "prefix":
            case "suffix":
                if (this.isNotPermissed(sender, "nametagedit.edit.self"))
                    return;

                final String targetName = args[1];

                if (!sender.hasPermission("nametagedit.edit.others") && !targetName.equalsIgnoreCase(sender.getName())) {
                    NametagMessages.MODIFY_OWN_TAG.send(sender);
                    return;
                }

                final NametagEvent.ChangeType changeType = args[2].equalsIgnoreCase("prefix") ? NametagEvent.ChangeType.PREFIX
                        : NametagEvent.ChangeType.SUFFIX;
                this.handler.save(sender, targetName, changeType, Utils.format(args, 3, args.length));
                break;
            case "priority":
                if (this.isNotPermissed(sender, "nametagedit.edit.self"))
                    return;

                final String priorityName = args[1];

                if (!sender.hasPermission("nametagedit.edit.others") && !priorityName.equalsIgnoreCase(sender.getName())) {
                    NametagMessages.MODIFY_OWN_TAG.send(sender);
                    break;
                }

                this.setupPriority(sender, true, priorityName, args[3]);
                break;
            default:
                this.sendUsagePlayer(sender);
            }
        } else {
            this.sendUsagePlayer(sender);
        }
    }

    /**
     * Modifies groups
     */
    private void cmdGroups(final CommandSender sender, final String[] args) {
        if (this.isNotPermissed(sender, "nametagedit.groups"))
            return;
        if (args.length < 2) {
            this.sendUsageGroup(sender);
        } else {
            if (args[1].equalsIgnoreCase("list")) {
                sender.sendMessage(Utils.format("&f&lLoaded Groups"));
                for (final GroupData groupData : this.handler.getGroupData()) {
                    sender.sendMessage(
                            Utils.format("&6Group: &f" + groupData.getGroupName() + " &6Permission: &f" + groupData.getPermission()
                                    + " &6Formatted: " + groupData.getPrefix() + sender.getName() + groupData.getSuffix()));
                }
            } else if (args[1].equalsIgnoreCase("order")) {
                if (args.length <= 2) {
                    this.sendUsageGroup(sender);
                    return;
                }

                final List<String> order = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
                this.handler.getAbstractConfig().orderGroups(sender, order);

                String formatted = Arrays.toString(order.toArray());
                formatted = formatted.substring(1, formatted.length() - 1).replace(",", "");
                sender.sendMessage(Utils.format("&c&lNametagEdit Group Order:"));
                sender.sendMessage(formatted);
                sender.sendMessage(Utils.format("&cType /ne reload for these changes to take effect"));
            } else if (args[1].equalsIgnoreCase("remove")) {
                if (args.length == 3) {
                    final String group = args[2];

                    GroupData toDelete = null;
                    for (final GroupData groupData : this.handler.getGroupData()) {
                        if (groupData.getGroupName().equalsIgnoreCase(group)) {
                            toDelete = groupData;
                            break;

                        }
                    }

                    if (toDelete != null) {
                        this.handler.deleteGroup(toDelete);
                        NametagMessages.GROUP_REMOVED.send(sender, group);
                    }
                }
            } else if (args[1].equalsIgnoreCase("add")) {
                if (args.length == 3) {
                    final String group = args[2];

                    for (final GroupData groupData : this.handler.getGroupData()) {
                        if (groupData.getGroupName().equalsIgnoreCase(group)) {
                            NametagMessages.GROUP_EXISTS.send(sender, group);
                            return;
                        }
                    }

                    this.handler.addGroup(new GroupData(group, "", "", "", new Permission("my.perm", PermissionDefault.FALSE), -1));
                    NametagMessages.CREATED_GROUP.send(sender, group);
                }
            } else {
                if (args.length >= 4) {
                    final String group = args[1];
                    GroupData groupData = null;

                    for (final GroupData groups : this.handler.getGroupData()) {
                        if (groups.getGroupName().equalsIgnoreCase(group)) {
                            groupData = groups;
                            break;
                        }
                    }

                    if (groupData == null) {
                        NametagMessages.GROUP_EXISTS_NOT.send(sender, group);
                        return;
                    }

                    if (args[2].equalsIgnoreCase("permission")) {
                        groupData.setPermission(args[3]);
                        this.handler.getAbstractConfig().save(groupData);
                        NametagMessages.GROUP_VALUE.send(sender, group, "permission", args[3]);
                    } else if (args[2].equalsIgnoreCase("prefix")) {
                        final String value = Utils.format(args, 3, args.length).replace("\"", "");
                        groupData.setPrefix(Utils.format(value));
                        this.handler.applyTags();
                        this.handler.getAbstractConfig().save(groupData);
                        NametagMessages.GROUP_VALUE.send(sender, group, "prefix", Utils.format(value));
                    } else if (args[2].equalsIgnoreCase("suffix")) {
                        final String value = Utils.format(args, 3, args.length).replace("\"", "");
                        groupData.setSuffix(Utils.format(value));
                        this.handler.applyTags();
                        this.handler.getAbstractConfig().save(groupData);
                        NametagMessages.GROUP_VALUE.send(sender, group, "suffix", Utils.format(value));
                    } else if (args[2].equalsIgnoreCase("clear")) {
                        final boolean prefix = args[3].equalsIgnoreCase("prefix");
                        if (prefix) {
                            groupData.setPrefix("&f");
                        } else {
                            groupData.setSuffix("&f");
                        }
                        this.handler.applyTags();
                        this.handler.getAbstractConfig().save(groupData);
                        NametagMessages.GROUP_VALUE_CLEARED.send(sender, prefix ? "prefix" : "suffix", group);
                    } else if (args[2].equalsIgnoreCase("priority")) {
                        this.setupPriority(sender, false, group, args[3]);
                    }
                } else {
                    this.sendUsageGroup(sender);
                }
            }
        }
    }

    private void setupPriority(final CommandSender sender, final boolean player, final String key, final String number) {
        int priority;

        try {
            priority = Integer.parseInt(number);
        } catch (final NumberFormatException e) {
            NametagMessages.NOT_A_NUMBER.send(sender, number);
            return;
        }

        this.handler.save(sender, player, key, priority);
        NametagMessages.SET_PRIORITY.send(sender, priority, key);
    }

}