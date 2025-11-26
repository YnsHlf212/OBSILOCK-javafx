package com.coffrefort.client;

import javafx.application.Application;

/**
 * Separate launcher to avoid the "JavaFX runtime components are missing" error
 * when running directly from some IDEs or with newer JDKs.
 *
 * Run this class (com.coffrefort.client.Launcher) instead of App in your IDE.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
