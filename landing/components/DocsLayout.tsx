"use client"

import React, { useState, useEffect } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"

interface Section {
    id: string
    title: string
}

interface DocsLayoutProps {
    children: React.ReactNode
    sections: Section[]
    title: string
}

const docPages = [
    { href: "/docs/getting-started", title: "Getting Started", icon: "⚡" },
    { href: "/docs/user-guide", title: "User Guide", icon: "📖" },
    { href: "/docs/configuration", title: "Configuration", icon: "⚙️" },
    { href: "/docs/patterns", title: "Pattern Reference", icon: "🔍" },
    { href: "/docs/basic-usage", title: "Basic Usage", icon: "🚀" },
    { href: "/docs/ci", title: "CI/CD Integration", icon: "🔧" }
]

export default function DocsLayout({ children, sections, title }: DocsLayoutProps) {
    const pathname = usePathname()
    const [activeSection, setActiveSection] = useState("")
    const [sidebarOpen, setSidebarOpen] = useState(false)

    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        setActiveSection(entry.target.id)
                    }
                })
            },
            { rootMargin: "-20% 0px -80% 0px" }
        )

        sections.forEach(({ id }) => {
            const element = document.getElementById(id)
            if (element) observer.observe(element)
        })

        return () => observer.disconnect()
    }, [sections])

    const scrollToSection = (id: string) => {
        const element = document.getElementById(id)
        if (element) {
            element.scrollIntoView({ behavior: "smooth", block: "start" })
        }
    }

    return (
        <div className="min-h-screen bg-white dark:bg-gray-900">
            {/* Mobile menu button */}
            <div className="lg:hidden fixed top-20 left-4 z-50">
                <button
                    onClick={() => setSidebarOpen(!sidebarOpen)}
                    className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-2 shadow-lg"
                >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                    </svg>
                </button>
            </div>

            <div className="flex">
                {/* Left Sidebar - Documentation Navigation */}
                <aside className={`
                    fixed lg:sticky top-0 left-0 z-40 w-64 h-screen pt-20 
                    bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-700
                    transform lg:transform-none transition-transform duration-300 ease-in-out
                    ${sidebarOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"}
                `}>
                    <div className="h-full px-3 pb-4 overflow-y-auto">
                        <div className="space-y-2 font-medium">
                            <div className="px-3 py-2 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                                Documentation
                            </div>
                            {docPages.map((page) => (
                                <Link
                                    key={page.href}
                                    href={page.href}
                                    className={`
                                        flex items-center p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors
                                        ${pathname === page.href 
                                            ? "bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 border-r-2 border-primary-500" 
                                            : "text-gray-700 dark:text-gray-300"
                                        }
                                    `}
                                >
                                    <span className="mr-3 text-lg">{page.icon}</span>
                                    <span className="flex-1">{page.title}</span>
                                </Link>
                            ))}
                        </div>
                    </div>
                </aside>

                {/* Main Content */}
                <main className="flex-1 lg:ml-0">
                    <div className="flex">
                        {/* Content Area */}
                        <div className="flex-1 min-w-0">
                            <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8 max-w-4xl">
                                {children}
                            </div>
                        </div>

                        {/* Right Sidebar - Page Navigation */}
                        <aside className="hidden xl:block w-64 flex-shrink-0">
                            <div className="sticky top-20 h-screen overflow-y-auto pt-8 pr-8">
                                <div className="space-y-2">
                                    <div className="px-3 py-2 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                                        On this page
                                    </div>
                                    <nav className="space-y-1">
                                        {sections.map((section) => (
                                            <button
                                                key={section.id}
                                                onClick={() => scrollToSection(section.id)}
                                                className={`
                                                    block w-full text-left px-3 py-2 text-sm rounded-lg transition-colors
                                                    ${activeSection === section.id
                                                        ? "bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 border-r-2 border-primary-500"
                                                        : "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-800"
                                                    }
                                                `}
                                            >
                                                {section.title}
                                            </button>
                                        ))}
                                    </nav>
                                </div>
                            </div>
                        </aside>
                    </div>
                </main>
            </div>

            {/* Mobile sidebar overlay */}
            {sidebarOpen && (
                <div
                    className="fixed inset-0 z-30 bg-black bg-opacity-50 lg:hidden"
                    onClick={() => setSidebarOpen(false)}
                />
            )}
        </div>
    )
}
