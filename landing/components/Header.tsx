import Link from "next/link";
import DarkModeToggle from "./DarkModeToggle";

export default function Header() {
    return (
        <header className="bg-white/5 border-b border-white/10">
            <div className="container mx-auto flex items-center justify-between px-6 py-4">
                <Link href="/" className="text-lg font-semibold">SCAN</Link>
                <nav className="flex items-center gap-4">
                    <Link href="/docs">Docs</Link>
                    <a href="https://github.com/theaniketraj/SCAN" target="_blank" rel="noreferrer">GitHub</a>
                    <a href="/docs/getting-started" className="rounded-md bg-primary-500 px-3 py-2 text-white">Install</a>
                    <DarkModeToggle />
                </nav>
            </div>
        </header>
    )
}