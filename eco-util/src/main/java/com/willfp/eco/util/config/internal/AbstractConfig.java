package com.willfp.eco.util.config.internal;

import com.willfp.eco.util.StringUtils;
import com.willfp.eco.util.internal.PluginDependent;
import com.willfp.eco.util.plugin.AbstractEcoPlugin;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unchecked")
public abstract class AbstractConfig extends PluginDependent {
    /**
     * The linked {@link YamlConfiguration} where values are physically stored.
     */
    @Getter(AccessLevel.PUBLIC)
    protected final YamlConfiguration config;

    /**
     * The physical config file, as stored on disk.
     */
    @Getter(AccessLevel.PROTECTED)
    private final File configFile;

    /**
     * The full name of the config file (eg config.yml).
     */
    @Getter(AccessLevel.PROTECTED)
    private final String name;

    /**
     * The subdirectory path.
     */
    @Getter(AccessLevel.PROTECTED)
    private final String subDirectoryPath;

    /**
     * The provider of the config.
     */
    @Getter(AccessLevel.PROTECTED)
    private final Class<?> source;

    /**
     * Cached values for faster reading.
     */
    private final Map<String, Object> cache = new HashMap<>();

    /**
     * Abstract config.
     *
     * @param configName       The name of the config
     * @param plugin           The plugin.
     * @param subDirectoryPath The subdirectory path.
     * @param source           The class that owns the resource.
     */
    protected AbstractConfig(@NotNull final String configName,
                             @NotNull final AbstractEcoPlugin plugin,
                             @NotNull final String subDirectoryPath,
                             @NotNull final Class<?> source) {
        super(plugin);
        this.source = source;
        this.subDirectoryPath = subDirectoryPath;
        this.name = configName + ".yml";

        File directory = new File(this.getPlugin().getDataFolder(), subDirectoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        if (!new File(directory, this.name).exists()) {
            createFile();
        }

        this.configFile = new File(directory, this.name);
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createFile() {
        String resourcePath = getResourcePath();
        InputStream in = source.getResourceAsStream(resourcePath);

        File outFile = new File(this.getPlugin().getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(this.getPlugin().getDataFolder(), resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists()) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Get resource path as relative to base directory.
     *
     * @return The resource path.
     */
    protected String getResourcePath() {
        String resourcePath;

        if (subDirectoryPath.isEmpty()) {
            resourcePath = name;
        } else {
            resourcePath = subDirectoryPath + name;
        }

        return "/" + resourcePath;
    }

    /**
     * Get YamlConfiguration as found in jar.
     *
     * @return The YamlConfiguration.
     */
    protected YamlConfiguration getConfigInJar() {
        InputStream newIn = source.getResourceAsStream(getResourcePath());

        if (newIn == null) {
            throw new NullPointerException(this.getName() + " is null?");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(newIn, StandardCharsets.UTF_8));
        YamlConfiguration newConfig = new YamlConfiguration();

        try {
            newConfig.load(reader);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        return newConfig;
    }

    /**
     * Clears cache.
     */
    public final void clearCache() {
        cache.clear();
    }

    /**
     * Get if the config contains a key.
     *
     * @param path The key to check.
     * @return If contained.
     */
    public boolean has(@NotNull final String path) {
        return config.contains(path);
    }

    /**
     * Get configuration section from config.
     *
     * @param path The key to check.
     * @return The configuration section. Throws NPE if not found.
     */
    @NotNull
    public ConfigurationSection getSection(@NotNull final String path) {
        ConfigurationSection section = getSectionOrNull(path);
        if (section == null) {
            throw new NullPointerException("Section cannot be null!");
        } else {
            return section;
        }
    }

    /**
     * Get configuration section from config.
     *
     * @param path The key to check.
     * @return The configuration section, or null if not found.
     */
    @Nullable
    public ConfigurationSection getSectionOrNull(@NotNull final String path) {
        if (cache.containsKey(path)) {
            return (ConfigurationSection) cache.get(path);
        } else {
            cache.put(path, config.getConfigurationSection(path));
            return getSectionOrNull(path);
        }
    }

    /**
     * Get an integer from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or 0 if not found.
     */
    public int getInt(@NotNull final String path) {
        if (cache.containsKey(path)) {
            return (int) cache.get(path);
        } else {
            cache.put(path, config.getInt(path, 0));
            return getInt(path);
        }
    }

    /**
     * Get an integer from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or null if not found.
     */
    @Nullable
    public Integer getIntOrNull(@NotNull final String path) {
        if (has(path)) {
            return getInt(path);
        } else {
            return null;
        }
    }

    /**
     * Get an integer from config with a specified default (not found) value.
     *
     * @param path The key to fetch the value from.
     * @param def  The value to default to if not found.
     * @return The found value, or the default.
     */
    public int getInt(@NotNull final String path,
                      final int def) {
        if (cache.containsKey(path)) {
            return (int) cache.get(path);
        } else {
            cache.put(path, config.getInt(path, def));
            return getInt(path);
        }
    }

    /**
     * Get a list of integers from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or a blank {@link java.util.ArrayList} if not found.
     */
    @NotNull
    public List<Integer> getInts(@NotNull final String path) {
        if (cache.containsKey(path)) {
            return (List<Integer>) cache.get(path);
        } else {
            cache.put(path, config.getIntegerList(path));
            return getInts(path);
        }
    }

    /**
     * Get a list of integers from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or null if not found.
     */
    @Nullable
    public List<Integer> getIntsOrNull(@NotNull final String path) {
        if (has(path)) {
            return getInts(path);
        } else {
            return null;
        }
    }

    /**
     * Get a boolean from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or false if not found.
     */
    public boolean getBool(@NotNull final String path) {
        if (cache.containsKey(path)) {
            return (boolean) cache.get(path);
        } else {
            cache.put(path, config.getBoolean(path));
            return getBool(path);
        }
    }

    /**
     * Get a boolean from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or null if not found.
     */
    @Nullable
    public Boolean getBoolOrNull(@NotNull final String path) {
        if (has(path)) {
            return getBool(path);
        } else {
            return null;
        }
    }

    /**
     * Get a list of booleans from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or a blank {@link java.util.ArrayList} if not found.
     */
    @NotNull
    public List<Boolean> getBools(@NotNull final String path) {
        if (cache.containsKey(path)) {
            return (List<Boolean>) cache.get(path);
        } else {
            cache.put(path, config.getBooleanList(path));
            return getBools(path);
        }
    }

    /**
     * Get a list of booleans from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or null if not found.
     */
    @Nullable
    public List<Boolean> getBoolsOrNull(@NotNull final String path) {
        if (has(path)) {
            return getBools(path);
        } else {
            return null;
        }
    }

    /**
     * Get a string from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or an empty string if not found.
     */
    @NotNull
    public String getString(@NotNull final String path) {
        if (cache.containsKey(path)) {
            return (String) cache.get(path);
        } else {
            cache.put(path, StringUtils.translate(Objects.requireNonNull(config.getString(path, ""))));
            return getString(path);
        }
    }

    /**
     * Get a string from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or null if not found.
     */
    @Nullable
    public String getStringOrNull(@NotNull final String path) {
        if (has(path)) {
            return getString(path);
        } else {
            return null;
        }
    }

    /**
     * Get a list of strings from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or a blank {@link java.util.ArrayList} if not found.
     */
    @NotNull
    public List<String> getStrings(@NotNull final String path) {
        if (cache.containsKey(path)) {
            return (List<String>) cache.get(path);
        } else {
            cache.put(path, config.getStringList(path));
            return getStrings(path);
        }
    }

    /**
     * Get a list of strings from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or null if not found.
     */
    @Nullable
    public List<String> getStringsOrNull(@NotNull final String path) {
        if (has(path)) {
            return getStrings(path);
        } else {
            return null;
        }
    }

    /**
     * Get a decimal from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or 0 if not found.
     */
    public double getDouble(@NotNull final String path) {
        if (cache.containsKey(path)) {
            return (double) cache.get(path);
        } else {
            cache.put(path, config.getDouble(path));
            return getDouble(path);
        }
    }

    /**
     * Get a decimal from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or null if not found.
     */
    @Nullable
    public Double getDoubleOrNull(@NotNull final String path) {
        if (has(path)) {
            return getDouble(path);
        } else {
            return null;
        }
    }

    /**
     * Get a list of decimals from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or a blank {@link java.util.ArrayList} if not found.
     */
    @NotNull
    public List<Double> getDoubles(@NotNull final String path) {
        if (cache.containsKey(path)) {
            return (List<Double>) cache.get(path);
        } else {
            cache.put(path, config.getDoubleList(path));
            return getDoubles(path);
        }
    }

    /**
     * Get a list of decimals from config.
     *
     * @param path The key to fetch the value from.
     * @return The found value, or null if not found.
     */
    @Nullable
    public List<Double> getDoublesOrNull(@NotNull final String path) {
        if (has(path)) {
            return getDoubles(path);
        } else {
            return null;
        }
    }
}