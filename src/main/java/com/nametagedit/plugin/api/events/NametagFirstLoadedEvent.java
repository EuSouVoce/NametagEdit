package com.nametagedit.plugin.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nametagedit.plugin.api.data.INametag;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class represents an Event that is fired when a player joins the server
 * and receives their nametag.
 */
@Getter
@AllArgsConstructor
public class NametagFirstLoadedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final INametag nametag;

    public static HandlerList getHandlerList() { return NametagFirstLoadedEvent.HANDLERS; }

    @Override
    public HandlerList getHandlers() { return NametagFirstLoadedEvent.HANDLERS; }

}