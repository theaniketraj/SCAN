import React from "react"
import Link from "next/link"

export default function Hero() {
    return (
        <section className="grid gap-8 lg:grid-cols-2 items-center">
            <div>
                <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold leading-tight">SCAN — Secret detection for Gradle builds</h1>
                <p className="mt-4 text-lg sm:text-xl">Catch API keys, tokens, and credentials before they reach source control.</p>
                <div className="mt-6 flex flex-col sm:flex-row gap-4">
                    <Link href="/docs/getting-started" className="rounded-md bg-primary-500 px-6 py-3 text-white text-center hover:bg-primary-600 transition-colors">Install</Link>
                    <Link href="/docs" className="rounded-md border px-6 py-3 text-center hover:bg-white/10 transition-colors">Docs</Link>
                </div>
            </div>
            <div>
                <pre className="bg-black/80 text-white p-4 rounded-md text-sm sm:text-base overflow-x-auto">{`plugins {
  id("io.github.theaniketraj.scan") version "2.0.0"
}

./gradlew scanForSecrets`}</pre>
            </div>
        </section>
    )
}