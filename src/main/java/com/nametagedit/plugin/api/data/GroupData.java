package com.nametagedit.plugin.api.data;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * This class represents a group nametag. There are several properties
 * available.
 */
@Data
@AllArgsConstructor
public class GroupData implements INametag {

    private String groupName;
    private String prefix;
    private String suffix;
    private String permission;
    private Permission bukkitPermission;
    private int sortPriority;

    public GroupData() {

    }

    public void setPermission(final String permission) {
        this.permission = permission;
        this.bukkitPermission = new Permission(permission, PermissionDefault.FALSE);
    }

    @Override
    public boolean isPlayerTag() { return false; }

}