import React from "react"
import Link from "next/link"

export default function Hero() {
    return (
        <section className="grid gap-6 md:grid-cols-2 items-center">
            <div>
                <h1 className="text-4xl font-bold">SCAN — Secret detection for Gradle builds</h1>
                <p className="mt-4 text-lg">Catch API keys, tokens, and credentials before they reach source control.</p>
                <div className="mt-6 flex gap-4">
                    <Link href="/docs/getting-started" className="rounded-md bg-primary-500 px-4 py-2 text-white">Install</Link>
                    <Link href="/docs" className="rounded-md border px-4 py-2">Docs</Link>
                </div>
            </div>
            <div>
                <pre className="bg-black/80 text-white p-4 rounded-md">{`plugins {\n  id("io.github.theaniketraj.scan") version "1.0.0"\n}\n\n./gradlew scanForSecrets`}</pre>
            </div>
        </section>
    )
}