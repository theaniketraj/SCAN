import React from "react"

export default function Page() {
    return (
        <div>
            <h1 className="text-3xl font-bold">Configuration Reference</h1>
            <ul className="mt-4 list-disc pl-6 space-y-2">
                <li><code>failOnDetection</code> Boolean</li>
                <li><code>reportFormat</code> console | json | html</li>
                <li><code>entropyThreshold</code> Number</li>
                <li><code>whitelist</code> List</li>
            </ul>
            <pre className="mt-6 rounded-md bg-gray-900 p-4 text-white overflow-auto">{`scan {\n  failOnDetection = true\n  reportFormat = "json"\n  entropyThreshold = 4.5\n  whitelist = ["test_key_12345"]\n}`}</pre>
        </div>
    )
}