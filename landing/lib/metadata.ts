import { Metadata } from "next";

export const siteConfig = {
    name: "SCAN - Secret Detection for Gradle Builds",
    shortName: "SCAN",
    description:
        "Intelligent Gradle plugin that automatically detects secrets, API keys, credentials, and sensitive information in your codebase before they reach version control.",
    url: "https://theaniketraj.github.io/scan",
    ogImage: "https://theaniketraj.github.io/scan/og-image.png",
    author: {
        name: "Aniket Raj",
        url: "https://github.com/theaniketraj",
    },
    keywords: [
        "gradle plugin",
        "secret detection",
        "security scanning",
        "API key detection",
        "credential scanning",
        "sensitive data detection",
        "code security",
        "gradle security",
        "secret scanner",
        "leak prevention",
        "security tool",
        "devops security",
        "CI/CD security",
        "build security",
        "gradle build plugin",
        "secret management",
        "security automation",
        "code analysis",
        "static analysis",
        "security patterns",
    ],
    githubRepo: "https://github.com/theaniketraj/scan",
    twitter: "@theaniketraj",
};

export const baseMetadata: Metadata = {
    metadataBase: new URL(siteConfig.url),
    title: {
        default: siteConfig.name,
        template: `%s | ${siteConfig.shortName}`,
    },
    description: siteConfig.description,
    keywords: siteConfig.keywords,
    authors: [
        {
            name: siteConfig.author.name,
            url: siteConfig.author.url,
        },
    ],
    creator: siteConfig.author.name,
    publisher: siteConfig.author.name,
    applicationName: siteConfig.shortName,
    openGraph: {
        type: "website",
        locale: "en_US",
        url: siteConfig.url,
        title: siteConfig.name,
        description: siteConfig.description,
        siteName: siteConfig.shortName,
        images: [
            {
                url: siteConfig.ogImage,
                width: 1200,
                height: 630,
                alt: siteConfig.name,
            },
        ],
    },
    twitter: {
        card: "summary_large_image",
        title: siteConfig.name,
        description: siteConfig.description,
        images: [siteConfig.ogImage],
        creator: siteConfig.twitter,
    },
    robots: {
        index: true,
        follow: true,
        googleBot: {
            index: true,
            follow: true,
            "max-video-preview": -1,
            "max-image-preview": "large",
            "max-snippet": -1,
        },
    },
    alternates: {
        canonical: siteConfig.url,
    },
    category: "technology",
};

export function generatePageMetadata({
    title,
    description,
    path = "",
    keywords = [],
    noIndex = false,
}: {
    title: string;
    description: string;
    path?: string;
    keywords?: string[];
    noIndex?: boolean;
}): Metadata {
    const url = `${siteConfig.url}${path}`;
    const allKeywords = [...siteConfig.keywords, ...keywords];

    return {
        title,
        description,
        keywords: allKeywords,
        openGraph: {
            title,
            description,
            url,
            type: "website",
            siteName: siteConfig.shortName,
            images: [
                {
                    url: siteConfig.ogImage,
                    width: 1200,
                    height: 630,
                    alt: title,
                },
            ],
        },
        twitter: {
            card: "summary_large_image",
            title,
            description,
            images: [siteConfig.ogImage],
            creator: siteConfig.twitter,
        },
        alternates: {
            canonical: url,
        },
        robots: noIndex
            ? {
                  index: false,
                  follow: false,
              }
            : undefined,
    };
}
