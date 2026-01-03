package com.example;

/**
 * Main application class - This should be clean of secrets
 */
public class Application {
    
    public static void main(String[] args) {
        System.out.println("Starting application...");
        System.out.println("App Version: 1.0.0");
        System.out.println("Max Memory: " + Runtime.getRuntime().maxMemory());
    }
    
    public String getAppName() {
        return "MyApplication";
    }
    
    public int getVersion() {
        return 1;
    }
}
