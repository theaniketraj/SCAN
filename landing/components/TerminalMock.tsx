import React from "react"

export default function TerminalMock() {
    const output = `❌ AWS Access Key found in Config.kt:15
   AKIAIOSFODNN7EXAMPLE

⚠️ High entropy string in application.yml:8
   Entropy: 4.8 (random-looking password detected)

✅ Test key in TestConfig.kt:5 (whitelisted)
   test_key_12345`
    return (
        <pre className="mt-4 rounded-md bg-[#0b1220] p-4 sm:p-6 text-xs sm:text-sm text-white overflow-x-auto whitespace-pre-wrap">{output}</pre>
    )
}