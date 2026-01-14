import { MetadataRoute } from "next";

export default function robots(): MetadataRoute.Robots {
    const baseUrl = "https://theaniketraj.github.io/scan";

    return {
        rules: [
            {
                userAgent: "*",
                allow: "/",
                disallow: ["/api/", "/_next/", "/test-results/"],
            },
        ],
        sitemap: `${baseUrl}/sitemap.xml`,
    };
}
