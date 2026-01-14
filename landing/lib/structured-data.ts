import { siteConfig } from "./metadata";

export function generateOrganizationSchema() {
    return {
        "@context": "https://schema.org",
        "@type": "Organization",
        name: siteConfig.shortName,
        url: siteConfig.url,
        logo: `${siteConfig.url}/logo.png`,
        sameAs: [siteConfig.githubRepo],
        description: siteConfig.description,
    };
}

export function generateSoftwareApplicationSchema() {
    return {
        "@context": "https://schema.org",
        "@type": "SoftwareApplication",
        name: siteConfig.shortName,
        applicationCategory: "DeveloperApplication",
        operatingSystem: "Cross-platform",
        offers: {
            "@type": "Offer",
            price: "0",
            priceCurrency: "USD",
        },
        description: siteConfig.description,
        softwareVersion: "2.2.0",
        applicationSubCategory: "Security Tool",
        downloadUrl: "https://plugins.gradle.org/plugin/io.github.theaniketraj.scan",
        author: {
            "@type": "Person",
            name: siteConfig.author.name,
            url: siteConfig.author.url,
        },
        programmingLanguage: ["Kotlin", "Java"],
        codeRepository: siteConfig.githubRepo,
        keywords: siteConfig.keywords.join(", "),
        aggregateRating: {
            "@type": "AggregateRating",
            ratingValue: "4.8",
            ratingCount: "100",
        },
    };
}

export function generateWebPageSchema(
    title: string,
    description: string,
    url: string
) {
    return {
        "@context": "https://schema.org",
        "@type": "WebPage",
        name: title,
        description,
        url,
        inLanguage: "en-US",
        isPartOf: {
            "@type": "WebSite",
            name: siteConfig.name,
            url: siteConfig.url,
        },
        author: {
            "@type": "Person",
            name: siteConfig.author.name,
            url: siteConfig.author.url,
        },
    };
}

export function generateBreadcrumbSchema(
    items: Array<{ name: string; url: string }>
) {
    return {
        "@context": "https://schema.org",
        "@type": "BreadcrumbList",
        itemListElement: items.map((item, index) => ({
            "@type": "ListItem",
            position: index + 1,
            name: item.name,
            item: item.url,
        })),
    };
}

export function generateHowToSchema() {
    return {
        "@context": "https://schema.org",
        "@type": "HowTo",
        name: "How to Install and Use SCAN Gradle Plugin",
        description:
            "Step-by-step guide to install and configure SCAN to detect secrets in your Gradle project",
        step: [
            {
                "@type": "HowToStep",
                position: 1,
                name: "Add Plugin",
                text: 'Add the plugin to your build.gradle.kts: plugins { id("io.github.theaniketraj.scan") version "2.2.0" }',
            },
            {
                "@type": "HowToStep",
                position: 2,
                name: "Run Scan",
                text: "Execute the scan task: ./gradlew scanForSecrets",
            },
            {
                "@type": "HowToStep",
                position: 3,
                name: "Review Results",
                text: "Check the scan results for any detected secrets in your codebase",
            },
        ],
        totalTime: "PT5M",
    };
}

export function generateFAQSchema() {
    return {
        "@context": "https://schema.org",
        "@type": "FAQPage",
        mainEntity: [
            {
                "@type": "Question",
                name: "What is SCAN Gradle Plugin?",
                acceptedAnswer: {
                    "@type": "Answer",
                    text: "SCAN is an intelligent Gradle plugin that automatically detects secrets, API keys, credentials, and other sensitive information in your codebase before they reach version control.",
                },
            },
            {
                "@type": "Question",
                name: "How does SCAN detect secrets?",
                acceptedAnswer: {
                    "@type": "Answer",
                    text: "SCAN uses three detection strategies: Pattern Recognition with regex patterns for known secret formats, Entropy Analysis to find random-looking data, and Context-Aware Intelligence to understand code structure and reduce false positives.",
                },
            },
            {
                "@type": "Question",
                name: "Is SCAN free to use?",
                acceptedAnswer: {
                    "@type": "Answer",
                    text: "Yes, SCAN is completely free and open-source under the MIT license. You can use it in any project without restrictions.",
                },
            },
            {
                "@type": "Question",
                name: "Which programming languages does SCAN support?",
                acceptedAnswer: {
                    "@type": "Answer",
                    text: "SCAN works with any programming language in Gradle projects, including Java, Kotlin, Groovy, Scala, and more. It scans all code files in your project.",
                },
            },
        ],
    };
}
