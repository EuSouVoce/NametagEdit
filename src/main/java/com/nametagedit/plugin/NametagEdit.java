package com.nametagedit.plugin;

import com.nametagedit.plugin.api.INametagApi;
import com.nametagedit.plugin.api.NametagAPI;
import com.nametagedit.plugin.hooks.*;
import com.nametagedit.plugin.invisibility.InvisibilityTask;
import com.nametagedit.plugin.packets.PacketWrapper;
import com.nametagedit.plugin.packets.VersionChecker;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

/**
 * TODO: - Better uniform message format + more messages - Code cleanup - Add
 * language support
 */
@Getter
public class NametagEdit extends JavaPlugin {

    private static NametagEdit instance;

    private static INametagApi api;

    private NametagHandler handler;
    private NametagManager manager;
    private VersionChecker.BukkitVersion version;

    public static INametagApi getApi() { return NametagEdit.api; }

    @Override
    public void onEnable() {
        this.testCompat();
        if (!this.isEnabled())
            return;

        NametagEdit.instance = this;

        this.version = VersionChecker.getBukkitVersion();

        this.getLogger().info("Successfully loaded using bukkit version: " + this.version.name());

        this.manager = new NametagManager(this);
        this.handler = new NametagHandler(this, this.manager);

        final PluginManager pluginManager = Bukkit.getPluginManager();

        if (this.checkShouldRegister("zPermissions")) {
            pluginManager.registerEvents(new HookZPermissions(this.handler), this);
        } else if (this.checkShouldRegister("PermissionsEx")) {
            pluginManager.registerEvents(new HookPermissionsEX(this.handler), this);
        } else if (this.checkShouldRegister("GroupManager")) {
            pluginManager.registerEvents(new HookGroupManager(this.handler), this);
        } else if (this.checkShouldRegister("LuckPerms")) {
            pluginManager.registerEvents(new HookLuckPerms(this.handler), this);
        }

        if (pluginManager.getPlugin("LibsDisguises") != null) {
            pluginManager.registerEvents(new HookLibsDisguise(this), this);
        }
        if (pluginManager.getPlugin("Guilds") != null) {
            pluginManager.registerEvents(new HookGuilds(this.handler), this);
        }

        this.getCommand("ne").setExecutor(new NametagCommand(this.handler));

        if (NametagEdit.api == null) {
            NametagEdit.api = new NametagAPI(this.handler, this.manager);
        }

        if (this.version.name().startsWith("v1_8_"))
            new InvisibilityTask().runTaskTimerAsynchronously(this, 100L, 20L);
    }

    public static NametagEdit getInstance() { return NametagEdit.instance; }

    @Override
    public void onDisable() {
        this.manager.reset();
        this.handler.getAbstractConfig().shutdown();
    }

    void debug(final String message) {
        if (this.handler != null && this.handler.debug()) {
            this.getLogger().info("[DEBUG] " + message);
        }
    }

    private boolean checkShouldRegister(final String plugin) {
        if (Bukkit.getPluginManager().getPlugin(plugin) == null)
            return false;
        this.getLogger().info("Found " + plugin + "! Hooking in.");
        return true;
    }

    private void testCompat() {
        final PacketWrapper wrapper = new PacketWrapper("TEST", "&f", "", 0, new ArrayList<>(), true);
        wrapper.send();
        if (wrapper.error == null)
            return;
        Bukkit.getPluginManager().disablePlugin(this);
        this.getLogger().severe("\n------------------------------------------------------\n" + "[WARNING] NametagEdit v"
                + this.getPluginMeta().getVersion() + " Failed to load! [WARNING]"
                + "\n------------------------------------------------------" + "\nThis might be an issue with reflection. REPORT this:\n> "
                + wrapper.error + "\nThe plugin will now self destruct.\n------------------------------------------------------");
    }

}