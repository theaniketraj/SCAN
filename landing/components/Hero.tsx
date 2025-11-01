import React from "react"
import Link from "next/link"

export default function Hero() {
    return (
        <section className="grid gap-6 sm:gap-8 lg:grid-cols-2 items-center">
            <div>
                <h1 className="text-2xl xs:text-3xl sm:text-4xl lg:text-5xl font-bold leading-tight">SCAN - Secret detection for Gradle builds</h1>
                <p className="mt-3 sm:mt-4 text-base xs:text-lg sm:text-xl leading-relaxed">Catch API keys, tokens & credentials before they reach source control.</p>
                <div className="mt-6 sm:mt-8 flex flex-col xs:flex-row gap-3 sm:gap-4">
                    <Link href="/docs/getting-started" className="rounded-md bg-primary-500 px-6 sm:px-8 py-3 sm:py-4 text-white text-center hover:bg-primary-600 transition-colors font-medium text-sm sm:text-base min-h-[44px] flex items-center justify-center">Install</Link>
                    <Link href="/docs" className="rounded-md border px-6 sm:px-8 py-3 sm:py-4 text-center hover:bg-white/10 transition-colors font-medium text-sm sm:text-base min-h-[44px] flex items-center justify-center">Docs</Link>
                </div>
            </div>
            <div className="overflow-hidden">
                <pre className="bg-black/80 text-white p-3 xs:p-4 sm:p-6 rounded-md text-xs xs:text-sm sm:text-base overflow-x-auto whitespace-pre-wrap break-words">{`plugins {
  id("io.github.theaniketraj.scan") version "2.0.0"
}

./gradlew scanForSecrets`}</pre>
            </div>
        </section>
    )
}