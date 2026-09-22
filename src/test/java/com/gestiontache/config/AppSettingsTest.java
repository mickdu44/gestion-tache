package com.gestiontache.config;

import com.gestiontache.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSettingsTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsToTheRepositoryDefaultDirectoryWhenNothingIsConfigured() {
        AppSettings settings = new AppSettings(tempDir.resolve("settings.properties"));

        assertEquals(Optional.empty(), settings.getConfiguredDataDirectory());
        assertEquals(TaskRepository.DEFAULT_DATA_DIR, settings.getDataDirectory());
    }

    @Test
    void persistsAChosenDataDirectoryAcrossInstances() {
        Path settingsFile = tempDir.resolve("settings.properties");
        Path chosen = tempDir.resolve("mes-taches");
        new AppSettings(settingsFile).setDataDirectory(chosen);

        AppSettings reloaded = new AppSettings(settingsFile);

        assertEquals(Optional.of(chosen.toAbsolutePath()), reloaded.getConfiguredDataDirectory());
        assertEquals(chosen.toAbsolutePath(), reloaded.getDataDirectory());
        assertTrue(settingsFile.toFile().exists());
    }
}
