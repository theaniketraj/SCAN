import React from "react"

export default function Page() {
    return (
        <div>
            <h1 className="text-3xl font-bold">CI/CD Integration</h1>
            <pre className="mt-4 rounded-md bg-gray-900 p-4 text-white overflow-auto">{`name: CI\non: [push, pull_request]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - name: Set up JDK 17\n        uses: actions/setup-java@v4\n        with:\n          java-version: '17'\n      - name: Gradle Scan\n        run: ./gradlew scan --no-daemon`}</pre>
            <p className="mt-4">Set <code>failOnDetection = true</code> to fail the pipeline when secrets are detected.</p>
        </div>
    )
}