import React from "react"

export default function DocsIndex() {
    return (
        <div>
            <h1 className="text-3xl font-bold">Documentation</h1>
            <p className="mt-4">Find quickstart, configuration reference and CI examples.</p>
            <ul className="mt-6 space-y-2">
                <li><a href="/docs/getting-started">Getting Started</a></li>
                <li><a href="/docs/configuration">Configuration</a></li>
                <li><a href="/docs/patterns">Pattern Reference</a></li>
                <li><a href="/docs/ci">CI/CD Examples</a></li>
            </ul>
        </div>
    )
}