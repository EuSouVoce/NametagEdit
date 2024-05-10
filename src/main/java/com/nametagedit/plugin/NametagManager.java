package com.nametagedit.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.nametagedit.plugin.api.data.FakeTeam;
import com.nametagedit.plugin.packets.PacketWrapper;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NametagManager {

    private final Map<String, FakeTeam> TEAMS = new ConcurrentHashMap<>();
    private final Map<String, FakeTeam> CACHED_FAKE_TEAMS = new ConcurrentHashMap<>();
    private final NametagEdit plugin;

    /**
     * Gets the current team given a prefix and suffix If there is no team similar
     * to this, then a new team is created.
     */
    private FakeTeam getFakeTeam(final String prefix, final String suffix, final boolean visible) {
        return this.TEAMS.values().stream().filter(fakeTeam -> fakeTeam.isSimilar(prefix, suffix, visible)).findFirst().orElse(null);
    }

    /**
     * Adds a player to a FakeTeam. If they are already on this team, we do NOT
     * change that.
     */
    @SuppressWarnings("deprecation")
    private void addPlayerToTeam(final String player, final String prefix, final String suffix, final int sortPriority,
            final boolean playerTag, final boolean visible) {
        final FakeTeam previous = this.getFakeTeam(player);

        if (previous != null && previous.isSimilar(prefix, suffix, visible)) {
            this.plugin.debug(player + " already belongs to a similar team (" + previous.getName() + ")");
            return;
        }

        this.reset(player);

        FakeTeam joining = this.getFakeTeam(prefix, suffix, visible);
        if (joining != null) {
            joining.addMember(player);
            this.plugin.debug("Using existing team for " + player);
        } else {
            joining = new FakeTeam(prefix, suffix, sortPriority, playerTag);
            joining.setVisible(visible);
            joining.addMember(player);
            this.TEAMS.put(joining.getName(), joining);
            this.addTeamPackets(joining);
            this.plugin.debug("Created FakeTeam " + joining.getName() + ". Size: " + this.TEAMS.size());
        }

        final Player adding = Bukkit.getPlayerExact(player);
        if (adding != null) {
            this.addPlayerToTeamPackets(joining, adding.getName());
            this.cache(adding.getName(), joining);
        } else {

            OfflinePlayer offlinePlayer = Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(offlinePlayerFromBukkitCache -> player.equals(offlinePlayerFromBukkitCache.getName())).findFirst().orElse(null);
            if (offlinePlayer == null) {
                offlinePlayer = Bukkit.getOfflinePlayer(player);
                this.plugin.debug("The player " + player
                        + " is not present at the usercache of bukkit. Using deprecated 'Bukkit.getOfflinePlayer(playerName)' function to proceed");

            }

            this.addPlayerToTeamPackets(joining, offlinePlayer.getName());
            this.cache(offlinePlayer.getName(), joining);
        }

        this.plugin.debug(player + " has been added to team " + joining.getName());
    }

    public FakeTeam reset(final String player) { return this.reset(player, this.decache(player)); }

    @SuppressWarnings("deprecation")
    private FakeTeam reset(final String player, final FakeTeam fakeTeam) {
        if (fakeTeam != null && fakeTeam.getMembers().remove(player)) {
            boolean delete;
            final Player removing = Bukkit.getPlayerExact(player);
            if (removing != null) {
                delete = this.removePlayerFromTeamPackets(fakeTeam, removing.getName());
            } else {

                OfflinePlayer toRemoveOffline = Arrays.stream(Bukkit.getOfflinePlayers())
                        .filter(offlinePlayerFromBukkitCache -> player.equals(offlinePlayerFromBukkitCache.getName())).findFirst()
                        .orElse(null);
                if (toRemoveOffline == null) {
                    toRemoveOffline = Bukkit.getOfflinePlayer(player);
                    this.plugin.debug("The player " + player
                            + " is not present at the usercache of bukkit. Using deprecated 'Bukkit.getOfflinePlayer(playerName)' function to proceed");

                }

                delete = this.removePlayerFromTeamPackets(fakeTeam, toRemoveOffline.getName());
            }

            this.plugin.debug(player + " was removed from " + fakeTeam.getName());
            if (delete) {
                this.removeTeamPackets(fakeTeam);
                this.TEAMS.remove(fakeTeam.getName());
                this.plugin.debug("FakeTeam " + fakeTeam.getName() + " has been deleted. Size: " + this.TEAMS.size());
            }
        }

        return fakeTeam;
    }

    // ==============================================================
    // Below are public methods to modify the cache
    // ==============================================================
    private FakeTeam decache(final String player) { return this.CACHED_FAKE_TEAMS.remove(player); }

    public FakeTeam getFakeTeam(final String player) { return this.CACHED_FAKE_TEAMS.get(player); }

    private void cache(final String player, final FakeTeam fakeTeam) { this.CACHED_FAKE_TEAMS.put(player, fakeTeam); }

    // ==============================================================
    // Below are public methods to modify certain data
    // ==============================================================
    public void setNametag(final String player, final String prefix, final String suffix) { this.setNametag(player, prefix, suffix, -1); }

    public void setNametag(final String player, final String prefix, final String suffix, final boolean visible) {
        this.setNametag(player, prefix, suffix, -1, false, visible);
    }

    void setNametag(final String player, final String prefix, final String suffix, final int sortPriority) {
        this.setNametag(player, prefix, suffix, sortPriority, false, true);
    }

    void setNametag(final String player, final String prefix, final String suffix, final int sortPriority, final boolean playerTag,
            final boolean visible) {
        this.addPlayerToTeam(player, prefix != null ? prefix : "", suffix != null ? suffix : "", sortPriority, playerTag, visible);
    }

    void sendTeams(final Player player) {
        for (final FakeTeam fakeTeam : this.TEAMS.values()) {
            new PacketWrapper(fakeTeam.getName(), fakeTeam.getPrefix(), fakeTeam.getSuffix(), 0, fakeTeam.getMembers(),
                    fakeTeam.isVisible()).send(player);
        }
    }

    void reset() {
        for (final FakeTeam fakeTeam : this.TEAMS.values()) {
            this.removePlayerFromTeamPackets(fakeTeam, fakeTeam.getMembers());
            this.removeTeamPackets(fakeTeam);
        }
        this.CACHED_FAKE_TEAMS.clear();
        this.TEAMS.clear();
    }

    // ==============================================================
    // Below are private methods to construct a new Scoreboard packet
    // ==============================================================
    private void removeTeamPackets(final FakeTeam fakeTeam) {
        new PacketWrapper(fakeTeam.getName(), fakeTeam.getPrefix(), fakeTeam.getSuffix(), 1, new ArrayList<>(), fakeTeam.isVisible())
                .send();
    }

    private boolean removePlayerFromTeamPackets(final FakeTeam fakeTeam, final String... players) {
        return this.removePlayerFromTeamPackets(fakeTeam, Arrays.asList(players));
    }

    private boolean removePlayerFromTeamPackets(final FakeTeam fakeTeam, final List<String> players) {
        new PacketWrapper(fakeTeam.getName(), 4, players).send();
        fakeTeam.getMembers().removeAll(players);
        return fakeTeam.getMembers().isEmpty();
    }

    private void addTeamPackets(final FakeTeam fakeTeam) {
        new PacketWrapper(fakeTeam.getName(), fakeTeam.getPrefix(), fakeTeam.getSuffix(), 0, fakeTeam.getMembers(), fakeTeam.isVisible())
                .send();
    }

    private void addPlayerToTeamPackets(final FakeTeam fakeTeam, final String player) {
        new PacketWrapper(fakeTeam.getName(), 3, Collections.singletonList(player)).send();
    }

}