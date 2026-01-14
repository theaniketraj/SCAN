export default function manifest() {
    return {
        name: "SCAN - Secret Detection for Gradle Builds",
        short_name: "SCAN",
        description:
            "Intelligent Gradle plugin that automatically detects secrets, API keys, and credentials before they reach version control",
        start_url: "/",
        display: "standalone",
        background_color: "#ffffff",
        theme_color: "#1e293b",
        icons: [
            {
                src: "/icon-192.png",
                sizes: "192x192",
                type: "image/png",
            },
            {
                src: "/icon-512.png",
                sizes: "512x512",
                type: "image/png",
            },
        ],
    };
}
