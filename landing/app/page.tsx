import React from "react";
import Script from "next/script";
import Hero from "../components/Hero";
import FeatureCard from "../components/FeatureCard";
import TerminalMock from "../components/TerminalMock";
import { generatePageMetadata } from "../lib/metadata";
import {
    generateHowToSchema,
    generateFAQSchema,
} from "../lib/structured-data";

export const metadata = generatePageMetadata({
    title: "SCAN - Intelligent Secret Detection for Gradle Builds",
    description:
        "Prevent security leaks with SCAN Gradle Plugin. Automatically detect API keys, tokens, passwords, and sensitive data before they reach version control. Free, open-source, and easy to integrate.",
    path: "/",
    keywords: [
        "prevent security leaks",
        "gradle security plugin",
        "detect API keys",
        "prevent credential leaks",
        "security automation",
        "open source security",
    ],
});

export default function HomePage() {
    const howToSchema = generateHowToSchema();
    const faqSchema = generateFAQSchema();

    return (
        <>
            <Script
                id="schema-howto"
                type="application/ld+json"
                dangerouslySetInnerHTML={{
                    __html: JSON.stringify(howToSchema),
                }}
            />
            <Script
                id="schema-faq"
                type="application/ld+json"
                dangerouslySetInnerHTML={{
                    __html: JSON.stringify(faqSchema),
                }}
            />
            <div className="container mx-auto px-4 sm:px-6 py-6 sm:py-8 lg:py-12">
                <Hero />
                <section className="mt-8 sm:mt-12 lg:mt-16 grid gap-4 sm:gap-6 md:grid-cols-2 lg:grid-cols-3">
                    <FeatureCard
                        title="Pattern Recognition"
                        desc="Detects AWS, GitHub, DB strings using refined regex patterns."
                    />
                    <FeatureCard
                        title="Entropy Analysis"
                        desc="Finds random-looking strings and encoded secrets using entropy thresholds."
                    />
                    <FeatureCard
                        title="Context-Aware"
                        desc="Understands code context to reduce false positives."
                    />
                </section>
                <section className="mt-8 sm:mt-12 lg:mt-16">
                    <h2 className="text-xl sm:text-2xl lg:text-3xl font-semibold">
                        Detection Examples
                    </h2>
                    <TerminalMock />
                </section>
            </div>
        </>
    );
}
