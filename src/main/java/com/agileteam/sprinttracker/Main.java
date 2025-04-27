package com.agileteam.sprinttracker;

import com.agileteam.sprinttracker.api.ApiServer;
import com.agileteam.sprinttracker.manager.SprintManager;
import com.agileteam.sprinttracker.storage.DataStorage;
import com.agileteam.sprinttracker.ui.UIManager;

import java.io.IOException;

/**
 * Main entry point for the Agile Team Sprint Tracker application.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("Starting Agile Team Sprint Tracker...");
        
        try {
            // Initialize data storage
            System.out.println("Initializing data storage...");
            DataStorage dataStorage = new DataStorage();
            
            // Initialize sprint manager
            SprintManager sprintManager;
            
            try {
                // Try to load existing data
                System.out.println("Attempting to load existing data...");
                sprintManager = dataStorage.loadData();
                System.out.println("Data loaded successfully.");
            } catch (IOException e) {
                // If loading fails or no data exists, create a new sprint manager
                System.out.println("No existing data found or error loading data: " + e.getMessage());
                System.out.println("Starting with a clean slate.");
                sprintManager = new SprintManager();
            }

            // Check if running in API mode or CLI mode
            boolean apiMode = args.length > 0 && args[0].equalsIgnoreCase("--api");
            System.out.println("Running in " + (apiMode ? "API" : "CLI") + " mode");
            
            if (apiMode) {
                // Start API server
                System.out.println("Creating API server...");
                ApiServer apiServer = new ApiServer(sprintManager, dataStorage);
                try {
                    System.out.println("Starting API server...");
                    apiServer.start();
                    System.out.println("API Server started. Press Ctrl+C to stop.");
                    
                    // Keep the main thread alive
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            System.out.println("Shutting down API Server...");
                            apiServer.stop();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }));
                    
                    // Wait indefinitely
                    System.out.println("Main thread waiting...");
                    Thread.currentThread().join();
                } catch (Exception e) {
                    System.err.println("Failed to start API server: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Start CLI UI
                System.out.println("Starting CLI UI...");
                UIManager uiManager = new UIManager(sprintManager, dataStorage);
                uiManager.showMainMenu();
            }
            
            System.out.println("Thank you for using Agile Team Sprint Tracker!");
        } catch (Exception e) {
            System.err.println("Unexpected error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 