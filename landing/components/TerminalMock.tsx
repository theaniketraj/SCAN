import React from "react"

export default function TerminalMock() {
    const output = `❌ AWS Access Key found in Config.kt:15\n   AKIAIOSFODNN7EXAMPLE\n\n⚠️ High entropy string in application.yml:8\n   Entropy: 4.8 (random-looking password detected)\n\n✅ Test key in TestConfig.kt:5 (whitelisted)\n   test_key_12345`
    return (
        <pre className="mt-4 rounded-md bg-[#0b1220] p-6 text-sm text-white overflow-auto">{output}</pre>
    )
}