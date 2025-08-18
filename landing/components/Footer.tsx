import React from "react"

export default function Footer() {
    return (
        <footer className="border-t border-white/10 mt-16">
            <div className="container mx-auto px-6 py-8 text-sm">
                <div className="flex justify-between">
                    <div>© {new Date().getFullYear()} SCAN — MIT License</div>
                    <div className="space-x-4">
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/LICENSE" target="_blank">License</a>
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/SECURITY.md" target="_blank">Privacy</a>
                    </div>
                </div>
            </div>
        </footer>
    )
}