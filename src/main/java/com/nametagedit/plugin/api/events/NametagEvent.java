package com.nametagedit.plugin.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nametagedit.plugin.api.data.Nametag;

import lombok.Getter;
import lombok.Setter;

/**
 * This class represents an Event that is fired when a nametag is changed.
 */
public class NametagEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;

    @Getter
    @Setter
    @Deprecated
    private String value;

    @Getter
    @Setter
    private Nametag nametag;

    @Getter
    @Setter
    private String player;

    @Getter
    @Setter
    private ChangeType changeType;

    @Getter
    @Setter
    private ChangeReason changeReason;

    @Getter
    @Setter
    private StorageType storageType;

    public NametagEvent(final String player, final String value) { this(player, value, ChangeType.UNKNOWN); }

    public NametagEvent(final String player, final String value, final Nametag nametag, final ChangeType type) {
        this(player, value, type);
        this.nametag = nametag;
    }

    public NametagEvent(final String player, final String value, final ChangeType changeType) {
        this(player, value, changeType, StorageType.MEMORY, ChangeReason.UNKNOWN);
    }

    public NametagEvent(final String player, final String value, final ChangeType changeType, final ChangeReason changeReason) {
        this(player, value, changeType, StorageType.MEMORY, changeReason);
    }

    public NametagEvent(final String player, final String value, final ChangeType changeType, final StorageType storageType,
            final ChangeReason changeReason) {
        this.player = player;
        this.value = value;
        this.changeType = changeType;
        this.storageType = storageType;
        this.changeReason = changeReason;
    }

    public static HandlerList getHandlerList() { return NametagEvent.HANDLERS; }

    @Override
    public HandlerList getHandlers() { return NametagEvent.HANDLERS; }

    @Override
    public boolean isCancelled() { return this.cancelled; }

    @Override
    public void setCancelled(final boolean cancelled) { this.cancelled = cancelled; }

    public enum ChangeReason {
        API, PLUGIN, UNKNOWN
    }

    public enum ChangeType {
        PREFIX, SUFFIX, GROUP, CLEAR, PREFIX_AND_SUFFIX, RELOAD, UNKNOWN
    }

    public enum StorageType {
        MEMORY, PERSISTENT
    }

}