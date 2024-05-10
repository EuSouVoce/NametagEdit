package com.nametagedit.plugin.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.nametagedit.plugin.NametagHandler;

import lombok.AllArgsConstructor;
import me.glaremasters.guilds.api.events.base.GuildEvent;

@AllArgsConstructor
public class HookGuilds implements Listener {

    private final NametagHandler handler;

    @EventHandler
    public void onGuildEvent(final GuildEvent event) {
        final Player player = Bukkit.getPlayerExact(event.getPlayer().getName());
        if (player != null) {
            this.handler.applyTagToPlayer(player, false);
        }
    }

}