package com.nametagedit.plugin.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.nametagedit.plugin.NametagHandler;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;

public class HookLuckPerms implements Listener {

    private final NametagHandler handler;

    public HookLuckPerms(final NametagHandler handler) {
        this.handler = handler;
        final EventBus eventBus = Bukkit.getServicesManager().load(LuckPerms.class).getEventBus();
        eventBus.subscribe(handler.getPlugin(), UserDataRecalculateEvent.class, this::onUserDataRecalculateEvent);
    }

    private void onUserDataRecalculateEvent(final UserDataRecalculateEvent event) {
        final User user = event.getUser();
        final Player player = Bukkit.getPlayer(user.getUniqueId());
        if (player != null) {
            this.handler.applyTagToPlayer(player, false);
        }
    }

}
