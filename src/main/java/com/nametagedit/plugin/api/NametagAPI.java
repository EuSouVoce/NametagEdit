package com.nametagedit.plugin.api;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nametagedit.plugin.NametagHandler;
import com.nametagedit.plugin.NametagManager;
import com.nametagedit.plugin.api.data.FakeTeam;
import com.nametagedit.plugin.api.data.GroupData;
import com.nametagedit.plugin.api.data.Nametag;
import com.nametagedit.plugin.api.events.NametagEvent;

import lombok.AllArgsConstructor;

/**
 * Implements the INametagAPI interface. There only exists one instance of this
 * class.
 */
@AllArgsConstructor
public final class NametagAPI implements INametagApi {

    private final NametagHandler handler;
    private final NametagManager manager;

    @Override
    public FakeTeam getFakeTeam(final Player player) { return this.manager.getFakeTeam(player.getName()); }

    @Override
    public Nametag getNametag(final Player player) {
        final FakeTeam team = this.manager.getFakeTeam(player.getName());
        final boolean nullTeam = team == null;
        return new Nametag(nullTeam ? "" : team.getPrefix(), nullTeam ? "" : team.getSuffix());
    }

    @Override
    public void clearNametag(final Player player) {
        if (this.shouldFireEvent(player, NametagEvent.ChangeType.CLEAR)) {
            this.manager.reset(player.getName());
        }
    }

    @Override
    public void reloadNametag(final Player player) {
        if (this.shouldFireEvent(player, NametagEvent.ChangeType.RELOAD)) {
            this.handler.applyTagToPlayer(player, false);
        }
    }

    @Override
    public void clearNametag(final String player) { this.manager.reset(player); }

    @Override
    public void setPrefix(final Player player, final String prefix) {
        final FakeTeam fakeTeam = this.manager.getFakeTeam(player.getName());
        this.setNametagAlt(player, prefix, fakeTeam == null ? null : fakeTeam.getSuffix());
    }

    @Override
    public void setSuffix(final Player player, final String suffix) {
        final FakeTeam fakeTeam = this.manager.getFakeTeam(player.getName());
        this.setNametagAlt(player, fakeTeam == null ? null : fakeTeam.getPrefix(), suffix);
    }

    @Override
    public void setPrefix(final String player, final String prefix) {
        final FakeTeam fakeTeam = this.manager.getFakeTeam(player);
        this.manager.setNametag(player, prefix, fakeTeam == null ? null : fakeTeam.getSuffix());
    }

    @Override
    public void setSuffix(final String player, final String suffix) {
        final FakeTeam fakeTeam = this.manager.getFakeTeam(player);
        this.manager.setNametag(player, fakeTeam == null ? null : fakeTeam.getPrefix(), suffix);
    }

    @Override
    public void setNametag(final Player player, final String prefix, final String suffix) { this.setNametagAlt(player, prefix, suffix); }

    @Override
    public void setNametag(final String player, final String prefix, final String suffix) {
        this.manager.setNametag(player, prefix, suffix);
    }

    @Override
    public void hideNametag(final Player player) {
        final FakeTeam fakeTeam = this.manager.getFakeTeam(player.getName());
        this.manager.setNametag(player.getName(), fakeTeam == null ? null : fakeTeam.getPrefix(),
                fakeTeam == null ? null : fakeTeam.getSuffix(), false);
    }

    @Override
    public void hideNametag(final String player) {
        final FakeTeam fakeTeam = this.manager.getFakeTeam(player);
        this.manager.setNametag(player, fakeTeam == null ? null : fakeTeam.getPrefix(), fakeTeam == null ? null : fakeTeam.getSuffix(),
                false);
    }

    @Override
    public void showNametag(final Player player) {
        final FakeTeam fakeTeam = this.manager.getFakeTeam(player.getName());
        this.manager.setNametag(player.getName(), fakeTeam == null ? null : fakeTeam.getPrefix(),
                fakeTeam == null ? null : fakeTeam.getSuffix(), true);
    }

    @Override
    public void showNametag(final String player) {
        final FakeTeam fakeTeam = this.manager.getFakeTeam(player);
        this.manager.setNametag(player, fakeTeam == null ? null : fakeTeam.getPrefix(), fakeTeam == null ? null : fakeTeam.getSuffix(),
                true);
    }

    @Override
    public List<GroupData> getGroupData() { return this.handler.getGroupData(); }

    @Override
    public void saveGroupData(final GroupData... groupData) { this.handler.getAbstractConfig().save(groupData); }

    @Override
    public void applyTags() { this.handler.applyTags(); }

    @Override
    public void applyTagToPlayer(final Player player, final boolean loggedIn) { this.handler.applyTagToPlayer(player, loggedIn); }

    @Override
    public void updatePlayerPrefix(final String target, final String prefix) {
        this.handler.save(target, NametagEvent.ChangeType.PREFIX, prefix);
    }

    @Override
    public void updatePlayerSuffix(final String target, final String suffix) {
        this.handler.save(target, NametagEvent.ChangeType.SUFFIX, suffix);
    }

    @Override
    public void updatePlayerNametag(final String target, final String prefix, final String suffix) {
        this.handler.save(target, prefix, suffix);
    }

    /**
     * Private helper function to reduce redundancy
     */
    private boolean shouldFireEvent(final Player player, final NametagEvent.ChangeType type) {
        final NametagEvent event = new NametagEvent(player.getName(), "", this.getNametag(player), type);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    /**
     * Private helper function to reduce redundancy
     */
    private void setNametagAlt(final Player player, final String prefix, final String suffix) {
        final Nametag nametag = new Nametag(this.handler.formatWithPlaceholders(player, prefix, true),
                this.handler.formatWithPlaceholders(player, suffix, true));

        final NametagEvent event = new NametagEvent(player.getName(), prefix, nametag, NametagEvent.ChangeType.UNKNOWN);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        this.manager.setNametag(player.getName(), nametag.getPrefix(), nametag.getSuffix());
    }

}