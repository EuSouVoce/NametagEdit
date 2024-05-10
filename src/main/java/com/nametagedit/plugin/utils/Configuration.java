package com.nametagedit.plugin.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Configuration extends YamlConfiguration {

    private final Map<String, List<String>> headers = Maps.newConcurrentMap();
    private final File file;
    private List<String> mainHeader = Lists.newArrayList();
    private boolean loadHeaders;

    public Configuration(final File file) { this.file = file; }

    /**
     * Set the main header displayed at top of config.
     *
     * @param header header
     */
    public void mainHeader(final String... header) { this.mainHeader = Arrays.asList(header); }

    /**
     * Get main header displayed at top of config.
     *
     * @return header
     */
    public List<String> mainHeader() { return this.mainHeader; }

    /**
     * Set option header.
     *
     * @param key    of option (or section)
     * @param header of option (or section)
     */
    public void header(final String key, final String... header) { this.headers.put(key, Arrays.asList(header)); }

    /**
     * Get header of option
     *
     * @param key of option (or section)
     * @return Header
     */
    public List<String> header(final String key) { return this.headers.get(key); }

    public <T> T get(final String key, final Class<T> type) { return type.cast(this.get(key)); }

    /**
     * Reload config from file.
     */
    public void reload() { this.reload(this.headers.isEmpty()); }

    /**
     * Reload config from file.
     *
     * @param loadHeaders Whether or not to load headers.
     */
    public void reload(final boolean loadHeaders) {
        this.loadHeaders = loadHeaders;
        try {
            this.load(this.file);
        } catch (final Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "failed to reload file", e);
        }
    }

    @Override
    public void loadFromString(final String contents) throws InvalidConfigurationException {
        if (!this.loadHeaders) {
            super.loadFromString(contents);
            return;
        }

        final StringBuilder memoryData = new StringBuilder();

        // Parse headers
        final int indentLength = this.options().indent();
        final String pathSeparator = Character.toString(this.options().pathSeparator());
        int currentIndents = 0;
        String key = "";
        List<String> headers = Lists.newArrayList();
        for (final String line : contents.split("\n")) {
            if (line.isEmpty())
                continue; // Skip empty lines
            final int indent = this.getSuccessiveCharCount(line, ' ');
            final String subline = indent > 0 ? line.substring(indent) : line;
            if (subline.startsWith("#")) {
                if (subline.startsWith("#>")) {
                    final String txt = subline.startsWith("#> ") ? subline.substring(3) : subline.substring(2);
                    this.mainHeader.add(txt);
                    continue; // Main header, handled by bukkit
                }

                // Add header to list
                final String txt = subline.startsWith("# ") ? subline.substring(2) : subline.substring(1);
                headers.add(txt);
                continue;
            }

            final int indents = indent / indentLength;
            if (indents <= currentIndents) {
                // Remove last section of key
                final String[] array = key.split(Pattern.quote(pathSeparator));
                final int backspace = currentIndents - indents + 1;
                key = this.join(array, this.options().pathSeparator(), 0, array.length - backspace);
            }

            // Add new section to key
            final String separator = key.length() > 0 ? pathSeparator : "";
            final String lineKey = line.contains(":") ? line.split(Pattern.quote(":"))[0] : line;
            key += separator + lineKey.substring(indent);

            currentIndents = indents;

            memoryData.append(line).append('\n');
            if (!headers.isEmpty()) {
                this.headers.put(key, headers);
                headers = Lists.newArrayList();
            }
        }

        // Parse remaining text
        super.loadFromString(memoryData.toString());

        // Clear bukkit header
        this.options().setHeader(null);
    }

    /**
     * Save config to file
     */
    public void save() {
        if (this.headers.isEmpty() && this.mainHeader.isEmpty()) {
            try {
                super.save(this.file);
            } catch (final IOException e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to save file", e);
            }
            return;
        }

        // Custom save
        final int indentLength = this.options().indent();
        final String pathSeparator = Character.toString(this.options().pathSeparator());
        final String content = this.saveToString();
        @SuppressWarnings("deprecation")
        final StringBuilder fileData = new StringBuilder(this.buildHeader());
        int currentIndents = 0;
        String key = "";
        for (final String h : this.mainHeader) {
            // Append main header to top of file
            fileData.append("#> ").append(h).append('\n');
        }

        for (final String line : content.split("\n")) {
            if (line.isEmpty())
                continue; // Skip empty lines
            final int indent = this.getSuccessiveCharCount(line, ' ');
            final int indents = indent / indentLength;
            final String indentText = indent > 0 ? line.substring(0, indent) : "";
            if (indents <= currentIndents) {
                // Remove last section of key
                final String[] array = key.split(Pattern.quote(pathSeparator));
                final int backspace = currentIndents - indents + 1;
                key = this.join(array, this.options().pathSeparator(), 0, array.length - backspace);
            }

            // Add new section to key
            final String separator = key.length() > 0 ? pathSeparator : "";
            final String lineKey = line.contains(":") ? line.split(Pattern.quote(":"))[0] : line;
            key += separator + lineKey.substring(indent);

            currentIndents = indents;

            final List<String> header = this.headers.get(key);
            final String headerText = header != null ? this.addHeaderTags(header, indentText) : "";
            fileData.append(headerText).append(line).append('\n');
        }

        // Write data to file
        FileWriter writer = null;
        try {
            writer = new FileWriter(this.file);
            writer.write(fileData.toString());
            writer.flush();
        } catch (final IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to save file", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (final IOException ignored) {
                }
            }
        }
    }

    private String addHeaderTags(final List<String> header, final String indent) {
        final StringBuilder builder = new StringBuilder();
        for (final String line : header) {
            builder.append(indent).append("# ").append(line).append('\n');
        }
        return builder.toString();
    }

    private String join(final String[] array, final char joinChar, final int start, final int length) {
        final String[] copy = new String[length - start];
        System.arraycopy(array, start, copy, 0, length - start);
        return Joiner.on(joinChar).join(copy);
    }

    private int getSuccessiveCharCount(final String text, final char key) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == key) {
                count += 1;
            } else {
                break;
            }
        }
        return count;
    }

}