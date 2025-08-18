import React from "react"
import Hero from "../components/Hero"
import FeatureCard from "../components/FeatureCard"
import TerminalMock from "../components/TerminalMock"

export default function HomePage() {
    return (
        <div className="container mx-auto px-4 sm:px-6 py-8 sm:py-12">
            <Hero />
            <section className="mt-12 sm:mt-16 grid gap-6 sm:gap-8 md:grid-cols-2 lg:grid-cols-3">
                <FeatureCard title="Pattern Recognition" desc="Detects AWS, GitHub, DB strings using refined regex patterns." />
                <FeatureCard title="Entropy Analysis" desc="Finds random-looking strings and encoded secrets using entropy thresholds." />
                <FeatureCard title="Context-Aware" desc="Understands code context to reduce false positives." />
            </section>
            <section className="mt-12 sm:mt-16">
                <h2 className="text-xl sm:text-2xl font-semibold">Detection Examples</h2>
                <TerminalMock />
            </section>
        </div>
    )
}