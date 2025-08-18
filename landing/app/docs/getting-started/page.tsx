import React from "react"

export default function Page() {
    return (
        <div>
            <h1 className="text-3xl font-bold">Getting Started</h1>
            <p className="mt-4">Install the plugin.</p>
            <pre className="mt-4 rounded-md bg-gray-900 p-4 text-white overflow-auto">{`plugins {\n  id("io.github.theaniketraj.scan") version "1.0.0"\n}`}</pre>
            <p className="mt-6">Run a scan.</p>
            <pre className="mt-2 rounded-md bg-gray-900 p-4 text-white overflow-auto">{`./gradlew scanForSecrets`}</pre>
            <p className="mt-6">Basic configuration.</p>
            <pre className="mt-2 rounded-md bg-gray-900 p-4 text-white overflow-auto">{`scan {\n  failOnDetection = true\n  reportFormat = "console"\n}`}</pre>
        </div>
    )
}