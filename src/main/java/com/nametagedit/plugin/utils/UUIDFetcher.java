package com.nametagedit.plugin.utils;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.ImmutableList;

/**
 * This class is responsible for retrieving UUIDs from Names
 *
 * @author evilmidget38
 */
public class UUIDFetcher implements Callable<Map<String, UUID>> {

    private static final double PROFILES_PER_REQUEST = 100;
    private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private final JSONParser jsonParser = new JSONParser();
    private final List<String> names;
    private final boolean rateLimiting;

    private UUIDFetcher(final List<String> names, final boolean rateLimiting) {
        this.names = ImmutableList.copyOf(names);
        this.rateLimiting = rateLimiting;
    }

    private UUIDFetcher(final List<String> names) { this(names, true); }

    public static void lookupUUID(final String name, final Plugin plugin, final UUIDLookup uuidLookup) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID response = null;
                try {
                    response = UUIDFetcher.getUUIDOf(name);
                } catch (final Exception e) {
                    // Swallow
                }

                final UUID finalResponse = response;
                new BukkitRunnable() {
                    @Override
                    public void run() { uuidLookup.response(finalResponse); }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private static void writeBody(final HttpURLConnection connection, final String body) throws Exception {
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body.getBytes());
            stream.flush();
        }
    }

    private static HttpURLConnection createConnection() throws Exception {
        final URL url = URI.create(UUIDFetcher.PROFILE_URL).toURL();
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

    private static UUID getUUID(final String id) {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20)
                + "-" + id.substring(20, 32));
    }

    private static UUID getUUIDOf(final String name) throws Exception {
        return new UUIDFetcher(Collections.singletonList(name)).call().get(name);
    }

    @Override
    public Map<String, UUID> call() throws Exception {
        final Map<String, UUID> uuidMap = new HashMap<>();
        final int requests = (int) Math.ceil(this.names.size() / UUIDFetcher.PROFILES_PER_REQUEST);
        for (int i = 0; i < requests; i++) {
            final HttpURLConnection connection = UUIDFetcher.createConnection();
            final String body = JSONArray.toJSONString(this.names.subList(i * 100, Math.min((i + 1) * 100, this.names.size())));
            UUIDFetcher.writeBody(connection, body);
            final JSONArray array = (JSONArray) this.jsonParser.parse(new InputStreamReader(connection.getInputStream()));

            for (final Object profile : array) {
                final JSONObject jsonProfile = (JSONObject) profile;
                final String id = (String) jsonProfile.get("id");
                final String name = (String) jsonProfile.get("name");
                final UUID uuid = UUIDFetcher.getUUID(id);
                uuidMap.put(name, uuid);
            }

            if (this.rateLimiting && i != requests - 1) {
                Thread.sleep(100L);
            }
        }
        return uuidMap;
    }

    public interface UUIDLookup { void response(UUID uuid); }

}