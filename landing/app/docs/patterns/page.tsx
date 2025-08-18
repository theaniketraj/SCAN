import React from "react"

export default function Page() {
    return (
        <div>
            <h1 className="text-3xl font-bold">Pattern Reference</h1>
            <p className="mt-4">Patterns for cloud providers, VCS tokens, database URLs, and common API keys. Custom regex patterns can be added.</p>
            <p className="mt-4">Entropy uses Shannon entropy over candidate strings. A typical threshold is 3.5 to 5.0.</p>
        </div>
    )
}