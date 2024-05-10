package com.nametagedit.plugin.hooks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import com.nametagedit.plugin.NametagEdit;

import lombok.AllArgsConstructor;
import me.libraryaddict.disguise.events.DisguiseEvent;

@AllArgsConstructor
public class HookLibsDisguise implements Listener {

    private final NametagEdit plugin;

    @EventHandler
    public void onDisguiseEvent(final DisguiseEvent event) {
        if (event.getEntity() instanceof Player) {
            this.plugin.getHandler().getNametagManager().reset(event.getEntity().getName());
            new BukkitRunnable() {
                @Override
                public void run() { HookLibsDisguise.this.plugin.getHandler().applyTagToPlayer((Player) event.getEntity(), false); }
            }.runTaskLater(this.plugin, 3);
        }
    }

}