import React from "react"

export default function Footer() {
    return (
        <footer className="border-t border-white/10 mt-12 sm:mt-16">
            <div className="container mx-auto px-4 sm:px-6 py-8 sm:py-12 text-xs sm:text-sm">
                {/* Desktop layout */}
                <div className="hidden md:flex justify-between items-center gap-4">
                    <div>© {new Date().getFullYear()} SCAN</div>
                    <div className="text-center flex-grow">
                        <span className="text-gray-400">⁛ Sensitive Code Analyzer for Nerds ⁛</span>
                    </div>
                    <div className="space-x-4 flex-shrink-0">
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/LICENSE" target="_blank" className="hover:text-primary-400 dark:hover:text-white transition-colors">MIT License</a>
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/SECURITY.md" target="_blank" className="hover:text-primary-400 dark:hover:text-white transition-colors">Privacy</a>
                    </div>
                </div>

                {/* Mobile layout */}
                <div className="md:hidden space-y-3 sm:space-y-4 text-center">
                    <div className="text-gray-400 text-xs">⁛ Sensitive Code Analyzer for Nerds ⁛</div>
                    <div>© {new Date().getFullYear()} SCAN</div>
                    <div className="flex gap-2 sm:gap-4 justify-center flex-wrap">
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/LICENSE" target="_blank" className="hover:text-primary-400 dark:hover:text-white transition-colors min-h-[44px] flex items-center">License</a>
                        <a href="https://github.com/theaniketraj/SCAN/blob/main/SECURITY.md" target="_blank" className="hover:text-primary-400 dark:hover:text-white transition-colors min-h-[44px] flex items-center">Privacy</a>
                    </div>
                </div>
            </div>
        </footer>
    )
}