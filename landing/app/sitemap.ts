import { MetadataRoute } from "next";

export default function sitemap(): MetadataRoute.Sitemap {
    const baseUrl = "https://theaniketraj.github.io/scan";

    const routes = [
        "",
        "/docs",
        "/docs/getting-started",
        "/docs/user-guide",
        "/docs/configuration",
        "/docs/patterns",
        "/docs/basic-usage",
        "/docs/ci",
        "/docs/github-integration",
        "/docs/contributing",
        "/blog",
    ];

    return routes.map((route) => ({
        url: `${baseUrl}${route}`,
        lastModified: new Date(),
        changeFrequency: route === "" ? "weekly" : "monthly",
        priority: route === "" ? 1 : route.startsWith("/docs") ? 0.8 : 0.6,
    }));
}
