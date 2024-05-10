package com.nametagedit.plugin.hooks;

import org.anjocaido.groupmanager.events.GMUserEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.nametagedit.plugin.NametagHandler;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HookGroupManager implements Listener {

    private final NametagHandler handler;

    @EventHandler
    public void onGMUserEvent(final GMUserEvent event) {
        final Player player = event.getUser().getBukkitPlayer();
        if (player != null) {
            this.handler.applyTagToPlayer(player, false);
        }
    }

}