package com.nametagedit.plugin.hooks;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsPlayerUpdateEvent;

import com.nametagedit.plugin.NametagHandler;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HookZPermissions implements Listener {

    private final NametagHandler handler;

    @EventHandler
    public void onZPermissionsRankChangeEvent(final ZPermissionsPlayerUpdateEvent event) {
        this.handler.applyTagToPlayer(event.getPlayer(), false);
    }

}