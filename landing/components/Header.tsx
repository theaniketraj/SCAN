import Link from "next/link";
import DarkModeToggle from "./DarkModeToggle";

export default function Header() {
    return (
        <header className="bg-white/5 border-b border-white/10 sticky top-0 z-50">
            <div className="container mx-auto flex items-center justify-between px-4 sm:px-6 py-4">
                <Link href="/" className="text-base sm:text-lg font-semibold flex-shrink-0">SCAN</Link>
                
                {/* Desktop Navigation */}
                <nav className="hidden md:flex items-center gap-6 lg:gap-8">
                    <Link href="/docs" className="hover:text-primary-400 dark:hover:text-white transition-colors text-sm">Docs</Link>
                    <a href="https://plugins.gradle.org/plugin/io.github.theaniketraj.scan" target="_blank" rel="noreferrer" className="flex items-center hover:text-primary-400 dark:hover:text-white transition-colors text-sm" title="View on Gradle Plugin Portal">Gradle</a>
                    <a href="https://github.com/theaniketraj/SCAN" target="_blank" rel="noreferrer" className="flex items-center hover:text-primary-400 dark:hover:text-white transition-colors" title="View on GitHub">
                        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
                        </svg>
                    </a>
                    <Link href="/docs/getting-started" className="rounded-md bg-primary-500 px-4 py-2.5 text-white hover:bg-primary-600 transition-colors text-sm font-medium">Install</Link>
                    <DarkModeToggle />
                </nav>

                {/* Mobile Navigation */}
                <nav className="md:hidden flex items-center gap-3">
                    <Link href="/docs" className="text-xs sm:text-sm hover:text-primary-400 dark:hover:text-white transition-colors">Docs</Link>
                    <a href="https://github.com/theaniketraj/SCAN" target="_blank" rel="noreferrer" className="flex items-center hover:text-primary-400 dark:hover:text-white transition-colors p-1" title="View on GitHub">
                        <svg className="w-5 h-5 sm:w-5 sm:h-5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
                        </svg>
                    </a>
                    <Link href="/docs/getting-started" className="rounded-md bg-primary-500 px-3 py-2 text-white text-xs sm:text-sm hover:bg-primary-600 transition-colors font-medium">Install</Link>
                    <DarkModeToggle />
                </nav>
            </div>
        </header>
    )
}