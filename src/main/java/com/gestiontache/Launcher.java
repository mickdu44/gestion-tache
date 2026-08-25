package com.gestiontache;

/**
 * Separate entry point (not extending Application) so the shaded jar can be
 * run with a plain "java -jar" without the JavaFX runtime-components check
 * complaining about a missing module-path.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
