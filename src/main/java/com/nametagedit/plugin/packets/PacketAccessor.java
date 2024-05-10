package com.nametagedit.plugin.packets;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

class PacketAccessor {

    protected static String VERSION;

    static {
        try {
            PacketAccessor.VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (final Exception ignored) {
        }
        if (Bukkit.getBukkitVersion().contains("1.20.6")) {
            PacketAccessor.VERSION = "v1_20_R6";
        }
    }
    protected static final int MINOR_VERSION = Integer.parseInt(PacketAccessor.VERSION.split("_")[1]);

    private static final List<String> legacyVersions = Arrays.asList("v1_7_R1", "v1_7_R2", "v1_7_R3", "v1_7_R4", "v1_8_R1", "v1_8_R2",
            "v1_8_R3", "v1_9_R1", "v1_9_R2", "v1_10_R1", "v1_11_R1", "v1_12_R1");
    private static boolean CAULDRON_SERVER = false;
    private static boolean LEGACY_SERVER = false;

    private static Object UNSAFE;
    private static Method ALLOCATE_INSTANCE;

    static Field MEMBERS;
    static Field PREFIX;
    static Field SUFFIX;
    static Field TEAM_NAME;
    static Field PARAM_INT;
    static Field PACK_OPTION;
    static Field DISPLAY_NAME;
    static Field TEAM_COLOR;
    static Field PUSH;
    static Field VISIBILITY;
    // 1.17+
    static Field PARAMS;

    private static Method getHandle;
    private static Method sendPacket;
    private static Field playerConnection;

    private static Class<?> packetClass;
    // 1.17+
    private static Class<?> packetParamsClass;

    static {
        try {
            Class.forName("cpw.mods.fml.common.Mod");
            PacketAccessor.CAULDRON_SERVER = true;
        } catch (final ClassNotFoundException ignored) {
            // This is not a cauldron server
        }

        try {
            final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            final Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            PacketAccessor.UNSAFE = theUnsafeField.get(null);
            PacketAccessor.ALLOCATE_INSTANCE = PacketAccessor.UNSAFE.getClass().getMethod("allocateInstance", Class.class);

            if (PacketAccessor.legacyVersions.contains(PacketAccessor.VERSION))
                PacketAccessor.LEGACY_SERVER = true;

            final Class<?> typeCraftPlayer = Class.forName("org.bukkit.craftbukkit." + PacketAccessor.VERSION + ".entity.CraftPlayer");
            PacketAccessor.getHandle = typeCraftPlayer.getMethod("getHandle");

            if (PacketAccessor.CAULDRON_SERVER) {
                PacketAccessor.packetClass = Class.forName("net.minecraft.server.v1_7_R4.PacketPlayOutScoreboardTeam");
                final Class<?> typeNMSPlayer = Class.forName("net.minecraft.server.v1_7_R4.EntityPlayer");
                final Class<?> typePlayerConnection = Class.forName("net.minecraft.server.v1_7_R4.PlayerConnection");
                PacketAccessor.playerConnection = typeNMSPlayer.getField("field_71135_a");
                PacketAccessor.sendPacket = typePlayerConnection.getMethod("func_147359_a",
                        Class.forName("net.minecraft.server.v1_7_R4.Packet"));
            } else if (!PacketAccessor.isParamsVersion()) {
                PacketAccessor.packetClass = Class
                        .forName("net.minecraft.server." + PacketAccessor.VERSION + ".PacketPlayOutScoreboardTeam");
                final Class<?> typeNMSPlayer = Class.forName("net.minecraft.server." + PacketAccessor.VERSION + ".EntityPlayer");
                final Class<?> typePlayerConnection = Class.forName("net.minecraft.server." + PacketAccessor.VERSION + ".PlayerConnection");
                PacketAccessor.playerConnection = typeNMSPlayer.getField("playerConnection");
                PacketAccessor.sendPacket = typePlayerConnection.getMethod("sendPacket",
                        Class.forName("net.minecraft.server." + PacketAccessor.VERSION + ".Packet"));
            } else if (PacketAccessor.MINOR_VERSION > 20 && PacketAccessor.VERSION == "v1_20_R6") {
                PacketAccessor.packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket");
                PacketAccessor.packetParamsClass = Class
                        .forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket$Parameters");
                final Class<?> typeNMSPlayer = Class.forName("net.minecraft.server.level.ServerPlayer");
                final Class<?> typePlayerConnection = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
                PacketAccessor.playerConnection = typeNMSPlayer.getField("connection");
                final Class<?>[] sendPacketParameters = new Class[] { Class.forName("net.minecraft.network.protocol.Packet") };
                // 1.20.2+ priority to sending
                PacketAccessor.sendPacket = Stream
                        .concat(Arrays.stream(typePlayerConnection.getSuperclass().getMethods()),
                                Arrays.stream(typePlayerConnection.getMethods()))
                        .filter(method -> Arrays.equals(method.getParameterTypes(), sendPacketParameters)).findFirst()
                        .orElseThrow(NoSuchMethodException::new);
            } else {
                // 1.17+
                PacketAccessor.packetClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam");
                PacketAccessor.packetParamsClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam$b");
                final Class<?> typeNMSPlayer = Class.forName("net.minecraft.server.level.EntityPlayer");
                final Class<?> typePlayerConnection = Class.forName("net.minecraft.server.network.PlayerConnection");
                if (PacketAccessor.MINOR_VERSION >= 20) {
                    // 1.20
                    PacketAccessor.playerConnection = typeNMSPlayer.getField("c");
                } else {
                    // 1.17-1.19
                    PacketAccessor.playerConnection = typeNMSPlayer.getField("b");
                }
                final Class<?>[] sendPacketParameters = new Class[] { Class.forName("net.minecraft.network.protocol.Packet") };
                PacketAccessor.sendPacket = Stream.concat(Arrays.stream(typePlayerConnection.getSuperclass().getMethods()), // 1.20.2+
                                                                                                                            // priority to
                                                                                                                            // packet
                        // sending
                        Arrays.stream(typePlayerConnection.getMethods()))
                        .filter(method -> Arrays.equals(method.getParameterTypes(), sendPacketParameters)).findFirst()
                        .orElseThrow(NoSuchMethodException::new);
            }

            PacketData currentVersion = null;
            if (PacketAccessor.VERSION == "v1_20_R6") {
                currentVersion = PacketData.v1_20_6;
            } else
                for (final PacketData packetData : PacketData.values()) {
                    if (PacketAccessor.VERSION.contains(packetData.name())) {
                        currentVersion = packetData;
                    }
                }

            if (PacketAccessor.CAULDRON_SERVER) {
                currentVersion = PacketData.cauldron;
            }

            if (currentVersion != null) {
                if (!PacketAccessor.isParamsVersion()) {
                    PacketAccessor.PREFIX = PacketAccessor.getNMS(currentVersion.getPrefix());
                    PacketAccessor.SUFFIX = PacketAccessor.getNMS(currentVersion.getSuffix());
                    PacketAccessor.MEMBERS = PacketAccessor.getNMS(currentVersion.getMembers());
                    PacketAccessor.TEAM_NAME = PacketAccessor.getNMS(currentVersion.getTeamName());
                    PacketAccessor.PARAM_INT = PacketAccessor.getNMS(currentVersion.getParamInt());
                    PacketAccessor.PACK_OPTION = PacketAccessor.getNMS(currentVersion.getPackOption());
                    PacketAccessor.DISPLAY_NAME = PacketAccessor.getNMS(currentVersion.getDisplayName());

                    if (!PacketAccessor.isLegacyVersion()) {
                        PacketAccessor.TEAM_COLOR = PacketAccessor.getNMS(currentVersion.getColor());
                    }

                    if (PacketAccessor.isPushVersion()) {
                        PacketAccessor.PUSH = PacketAccessor.getNMS(currentVersion.getPush());
                    }

                    if (PacketAccessor.isVisibilityVersion()) {
                        PacketAccessor.VISIBILITY = PacketAccessor.getNMS(currentVersion.getVisibility());
                    }
                } else {
                    // 1.17+
                    PacketAccessor.PARAM_INT = PacketAccessor.getNMS(currentVersion.getParamInt());
                    PacketAccessor.TEAM_NAME = PacketAccessor.getNMS(currentVersion.getTeamName());
                    PacketAccessor.MEMBERS = PacketAccessor.getNMS(currentVersion.getMembers());
                    PacketAccessor.PARAMS = PacketAccessor.getNMS(currentVersion.getParams());

                    PacketAccessor.PREFIX = PacketAccessor.getParamNMS(currentVersion.getPrefix());
                    PacketAccessor.SUFFIX = PacketAccessor.getParamNMS(currentVersion.getSuffix());
                    PacketAccessor.PACK_OPTION = PacketAccessor.getParamNMS(currentVersion.getPackOption());
                    PacketAccessor.DISPLAY_NAME = PacketAccessor.getParamNMS(currentVersion.getDisplayName());
                    PacketAccessor.TEAM_COLOR = PacketAccessor.getParamNMS(currentVersion.getColor());
                    PacketAccessor.PUSH = PacketAccessor.getParamNMS(currentVersion.getPush());
                    PacketAccessor.VISIBILITY = PacketAccessor.getParamNMS(currentVersion.getVisibility());
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isLegacyVersion() { return PacketAccessor.LEGACY_SERVER; }

    public static boolean isParamsVersion() { return PacketAccessor.MINOR_VERSION >= 17; }

    private static boolean isPushVersion() { return PacketAccessor.MINOR_VERSION >= 9; }

    private static boolean isVisibilityVersion() { return PacketAccessor.MINOR_VERSION >= 8; }

    private static Field getNMS(final String path) throws Exception {
        final Field field = PacketAccessor.packetClass.getDeclaredField(path);
        field.setAccessible(true);
        return field;
    }

    // 1.17+
    private static Field getParamNMS(final String path) throws Exception {
        final Field field = PacketAccessor.packetParamsClass.getDeclaredField(path);
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings("deprecation")
    static Object createPacket() {
        try {
            if (!PacketAccessor.isParamsVersion()) {
                return PacketAccessor.packetClass.newInstance();
            } else {
                return PacketAccessor.ALLOCATE_INSTANCE.invoke(PacketAccessor.UNSAFE, PacketAccessor.packetClass);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static Object createPacketParams() {
        try {
            if (!PacketAccessor.isParamsVersion()) {
                return null;
            } else {
                return PacketAccessor.ALLOCATE_INSTANCE.invoke(PacketAccessor.UNSAFE, PacketAccessor.packetParamsClass);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static void sendPacket(final Collection<? extends Player> players, final Object packet) {
        for (final Player player : players) {
            PacketAccessor.sendPacket(player, packet);
        }
    }

    static void sendPacket(final Player player, final Object packet) {
        try {
            final Object nmsPlayer = PacketAccessor.getHandle.invoke(player);
            final Object connection = PacketAccessor.playerConnection.get(nmsPlayer);
            PacketAccessor.sendPacket.invoke(connection, packet);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
