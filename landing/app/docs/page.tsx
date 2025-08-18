import React from "react"
import Link from "next/link"

export default function DocsIndex() {
    return (
        <div>
            <h1 className="text-3xl font-bold">Documentation</h1>
            <p className="mt-4">Find quickstart, configuration reference and CI examples.</p>
            <ul className="mt-6 space-y-2">
                <li><Link href="/docs/getting-started">Getting Started</Link></li>
                <li><Link href="/docs/configuration">Configuration</Link></li>
                <li><Link href="/docs/patterns">Pattern Reference</Link></li>
                <li><Link href="/docs/ci">CI/CD Examples</Link></li>
            </ul>
        </div>
    )
}