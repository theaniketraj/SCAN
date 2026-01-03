package com.example.config;

/**
 * Configuration class with embedded secrets (INTENTIONAL FOR TESTING)
 * This demonstrates what the SCAN plugin should detect
 */
public class DatabaseConfig {
    
    // AWS Access Key - Should be detected
    private static final String AWS_ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String AWS_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    
    // Database credentials - Should be detected
    private String dbUsername = "admin";
    private String dbPassword = "SuperSecretP@ssw0rd123!";
    private String connectionString = "jdbc:mysql://localhost:3306/mydb?user=root&password=MyP@ssw0rd123";
    
    // GitHub Personal Access Token - Should be detected
    public static final String GITHUB_TOKEN = "ghp_1234567890abcdefghijklmnopqrstuvwx";
    
    // API Keys - Should be detected
    private String stripeApiKey = "sk_live_1234567890abcdefghijklmnopqrstuvwxyzABCDEF";
    private String googleApiKey = "AIzaSyD1234567890abcdefghijklmnopqrstu";
    
    // Azure credentials - Should be detected
    private String azureClientId = "12345678-1234-1234-1234-123456789abc";
    private String azureClientSecret = "AbCd~EfGh.IjKl-MnOp_QrStUvWxYzAbCdEfGhIjKl";
    
    // JWT Token - Should be detected as high entropy
    private String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    
    // Slack webhook - Should be detected
    private String slackWebhook = "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX";
    
    // Private Key - Should be detected
    private static final String RSA_PRIVATE_KEY = """
        -----BEGIN RSA PRIVATE KEY-----
        MIIEpAIBAAKCAQEAy8Dbv8prpJ/0kKhlGeJYozo2t60EG8L0561g13R29LvMR5hy
        vGZlGJpmn65+A4xHXInJYiPuKzrKUnApeLZ+vw1HocOAZtWK0z3r26uA8kQYOKX9
        Qt/DbCdvsF9wF8gRK0ptx9M6R13NvBxvVQApfc9jB9nTzphOgM4JiEYvlV8FLhg9
        yZovMYd6Wwf3aoXK891VQxTr/kQYoq1Yp+68i6T4nNq7NWC+UNVjQHxNQMQMzU6l
        WCX8zyg3yH88OAQkUXIXKfQ+NkvYQ1cxaMoVPpY72+eVthKzpMeyHkBn7ciumk5q
        gLTEJAfWZpe4f4eFZj/Rc8Y8Jj2IS5kVPjUywQIDAQABAoIBADhg1u1Mv1hAAlX8
        omz1Gn2f4AAW2aos2cM5UDCNw1SYmj+9SRIkaxjRsE/C4o9sw1oxrg1/z6kajV0e
        N/z7Thx1xPZPLPbp3xG1pRLXv2l8fNGxB5Kp6zyP2ydcK2ZC1xO7aYO7bT9YHD4Z
        -----END RSA PRIVATE KEY-----
        """;
    
    // Legitimate constant (should NOT be detected)
    public static final String APP_NAME = "MyApplication";
    public static final int MAX_CONNECTIONS = 100;
    public static final String DEFAULT_LOCALE = "en_US";
}
