package com.nametagedit.plugin.packets;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.nametagedit.plugin.NametagHandler;
import com.nametagedit.plugin.utils.Utils;

@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
public class PacketWrapper {

    public String error;
    private final int param;
    private final Object packet = PacketAccessor.createPacket();
    private final Object packetParams = PacketAccessor.createPacketParams();

    private static Method CraftChatMessage;
    private static Class<? extends Enum> typeEnumChatFormat;
    private static Enum<?> RESET_COLOR;

    static {
        try {
            if (!PacketAccessor.isLegacyVersion()) {
                if (!PacketAccessor.isParamsVersion()) {
                    PacketWrapper.typeEnumChatFormat = (Class<? extends Enum>) Class
                            .forName("net.minecraft.server." + PacketAccessor.VERSION + ".EnumChatFormat");
                } else {
                    try {
                        PacketWrapper.typeEnumChatFormat = (Class<? extends Enum>) Class.forName("net.minecraft.ChatFormatting");
                    } catch (final ClassNotFoundException e) {
                        PacketWrapper.typeEnumChatFormat = (Class<? extends Enum>) Class.forName("net.minecraft.EnumChatFormat");
                    }
                }
                Class<?> typeCraftChatMessage;
                try {
                    typeCraftChatMessage = Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage");
                } catch (final ClassNotFoundException e) {
                    typeCraftChatMessage = Class.forName("org.bukkit.craftbukkit." + PacketAccessor.VERSION + ".util.CraftChatMessage");
                }
                PacketWrapper.CraftChatMessage = typeCraftChatMessage.getMethod("fromString", String.class);
                PacketWrapper.RESET_COLOR = Enum.valueOf(PacketWrapper.typeEnumChatFormat, "RESET");
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public PacketWrapper(final String name, final int param, final List<String> members) {
        if (param != 3 && param != 4) {
            throw new IllegalArgumentException("Method must be join or leave for player constructor");
        }
        this.param = param;
        this.setupDefaults(name, param);
        this.setupMembers(members);
    }

    public PacketWrapper(final String name, final String prefix, String suffix, final int param, final Collection<?> players,
            final boolean visible) {
        this.param = param;
        this.setupDefaults(name, param);
        if (param == 0 || param == 2) {
            try {
                if (PacketAccessor.isLegacyVersion()) {
                    PacketAccessor.DISPLAY_NAME.set(this.packet, name);
                    PacketAccessor.PREFIX.set(this.packet, prefix);
                    PacketAccessor.SUFFIX.set(this.packet, suffix);
                } else {
                    final String color = ChatColor.getLastColors(prefix);
                    String colorCode = null;
                    Enum<?> colorEnum = null;

                    if (!color.isEmpty()) {
                        colorCode = color.substring(color.length() - 1);
                        String chatColor = ChatColor.getByChar(colorCode).name();

                        if (chatColor.equalsIgnoreCase("MAGIC"))
                            chatColor = "OBFUSCATED";

                        colorEnum = Enum.valueOf(PacketWrapper.typeEnumChatFormat, chatColor);
                    }

                    if (colorCode != null)
                        suffix = ChatColor.getByChar(colorCode) + suffix;

                    if (!PacketAccessor.isParamsVersion()) {
                        PacketAccessor.TEAM_COLOR.set(this.packet, colorEnum == null ? PacketWrapper.RESET_COLOR : colorEnum);
                        PacketAccessor.DISPLAY_NAME.set(this.packet, Array.get(PacketWrapper.CraftChatMessage.invoke(null, name), 0));
                        PacketAccessor.PREFIX.set(this.packet, Array.get(PacketWrapper.CraftChatMessage.invoke(null, prefix), 0));
                        PacketAccessor.SUFFIX.set(this.packet, Array.get(PacketWrapper.CraftChatMessage.invoke(null, suffix), 0));
                    } else {
                        // 1.17+
                        PacketAccessor.TEAM_COLOR.set(this.packetParams, colorEnum == null ? PacketWrapper.RESET_COLOR : colorEnum);
                        PacketAccessor.DISPLAY_NAME.set(this.packetParams, Array.get(PacketWrapper.CraftChatMessage.invoke(null, name), 0));
                        PacketAccessor.PREFIX.set(this.packetParams, Array.get(PacketWrapper.CraftChatMessage.invoke(null, prefix), 0));
                        PacketAccessor.SUFFIX.set(this.packetParams, Array.get(PacketWrapper.CraftChatMessage.invoke(null, suffix), 0));
                    }
                }

                if (!PacketAccessor.isParamsVersion()) {
                    PacketAccessor.PACK_OPTION.set(this.packet, 1);

                    if (PacketAccessor.VISIBILITY != null) {
                        PacketAccessor.VISIBILITY.set(this.packet, visible ? "always" : "never");
                    }
                } else {
                    // 1.17+
                    PacketAccessor.PACK_OPTION.set(this.packetParams, 1);

                    if (PacketAccessor.VISIBILITY != null) {
                        PacketAccessor.VISIBILITY.set(this.packetParams, visible ? "always" : "never");
                    }
                }

                if (param == 0) {
                    ((Collection) PacketAccessor.MEMBERS.get(this.packet)).addAll(players);
                }
            } catch (final Exception e) {
                this.error = e.getMessage();
            }
        }
    }

    private void setupMembers(Collection<?> players) {
        try {
            players = players == null || players.isEmpty() ? new ArrayList<>() : players;
            ((Collection) PacketAccessor.MEMBERS.get(this.packet)).addAll(players);
        } catch (final Exception e) {
            this.error = e.getMessage();
        }
    }

    private void setupDefaults(final String name, final int param) {
        try {
            PacketAccessor.TEAM_NAME.set(this.packet, name);
            PacketAccessor.PARAM_INT.set(this.packet, param);

            if (PacketAccessor.isParamsVersion()) {
                // 1.17+ These null values are not allowed, this initializes them.
                PacketAccessor.MEMBERS.set(this.packet, new ArrayList<>());
                PacketAccessor.PUSH.set(this.packetParams, "");
                PacketAccessor.VISIBILITY.set(this.packetParams, "");
                PacketAccessor.TEAM_COLOR.set(this.packetParams, PacketWrapper.RESET_COLOR);
            }
            if (NametagHandler.DISABLE_PUSH_ALL_TAGS && PacketAccessor.PUSH != null) {
                if (!PacketAccessor.isParamsVersion()) {
                    PacketAccessor.PUSH.set(this.packet, "never");
                } else {
                    // 1.17+
                    PacketAccessor.PUSH.set(this.packetParams, "never");
                }
            }
        } catch (final Exception e) {
            this.error = e.getMessage();
        }
    }

    private void constructPacket() {
        try {
            if (PacketAccessor.isParamsVersion()) {
                // 1.17+

                PacketAccessor.PARAMS.set(this.packet, this.param == 0 ? Optional.ofNullable(this.packetParams) : Optional.empty());
            }
        } catch (final Exception e) {
            this.error = e.getMessage();
        }
    }

    public void send() {
        this.constructPacket();
        PacketAccessor.sendPacket(Utils.getOnline(), this.packet);
    }

    public void send(final Player player) {
        this.constructPacket();
        PacketAccessor.sendPacket(player, this.packet);
    }

}