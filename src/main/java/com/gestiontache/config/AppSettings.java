package com.gestiontache.config;

import com.gestiontache.repository.TaskRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

/**
 * Persists small app-wide preferences in a properties file under the
 * user's home, separate from the day-JSON task files themselves. Currently
 * holds a single preference: which folder those task files are read from
 * and written to (see {@link TaskRepository}), changeable from the app's
 * "Reglages" menu.
 */
public class AppSettings {

    private static final Path DEFAULT_SETTINGS_FILE =
            Paths.get(System.getProperty("user.home"), ".gestion-tache", "settings.properties");
    private static final String DATA_DIR_KEY = "dataDir";

    private final Path settingsFile;

    public AppSettings() {
        this(DEFAULT_SETTINGS_FILE);
    }

    public AppSettings(Path settingsFile) {
        this.settingsFile = settingsFile;
    }

    /** The folder chosen by the user, if any, without falling back to the default. */
    public Optional<Path> getConfiguredDataDirectory() {
        String stored = load().getProperty(DATA_DIR_KEY);
        return stored == null ? Optional.empty() : Optional.of(Paths.get(stored));
    }

    /** The folder tasks are actually read/written from: the chosen one, or the default. */
    public Path getDataDirectory() {
        return getConfiguredDataDirectory().orElse(TaskRepository.DEFAULT_DATA_DIR);
    }

    public void setDataDirectory(Path dataDir) {
        Properties properties = load();
        properties.setProperty(DATA_DIR_KEY, dataDir.toAbsolutePath().toString());
        save(properties);
    }

    private Properties load() {
        Properties properties = new Properties();
        if (Files.exists(settingsFile)) {
            try (InputStream in = Files.newInputStream(settingsFile)) {
                properties.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Impossible de lire les preferences depuis " + settingsFile, e);
            }
        }
        return properties;
    }

    private void save(Properties properties) {
        try {
            Files.createDirectories(settingsFile.getParent());
            try (OutputStream out = Files.newOutputStream(settingsFile)) {
                properties.store(out, "Preferences gestion-tache");
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'enregistrer les preferences dans " + settingsFile, e);
        }
    }
}
