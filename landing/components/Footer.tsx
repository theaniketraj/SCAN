import React from "react"

export default function Footer() {
    return (
        <footer className="border-t border-white/10 mt-16">
            <div className="container mx-auto px-6 py-8 text-sm">
                {/* Desktop layout */}
                <div className="hidden md:flex justify-between items-center">
                    <div>© {new Date().getFullYear()} SCAN</div>
                    <div className="text-center">
                        <span className="text-gray-400">⁛ Sensitive Code Analyzer for Nerds ⁛</span>
                    </div>
                    <div className="space-x-4">
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/LICENSE" target="_blank" className="hover:text-primary-400 dark:hover:text-white transition-colors">MIT License</a>
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/SECURITY.md" target="_blank" className="hover:text-primary-400 dark:hover:text-white transition-colors">Privacy</a>
                    </div>
                </div>

                {/* Mobile layout */}
                <div className="md:hidden space-y-4 text-center">
                    <div className="text-gray-400">⁛ Sensitive Code Analyzer for Nerds ⁛</div>
                    <div>© {new Date().getFullYear()} SCAN</div>
                    <div className="space-x-4">
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/LICENSE" target="_blank" className="hover:text-primary-400 dark:hover:text-white transition-colors">MIT License</a>
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/SECURITY.md" target="_blank" className="hover:text-primary-400 dark:hover:text-white transition-colors">Privacy</a>
                    </div>
                </div>
            </div>
        </footer>
    )
}