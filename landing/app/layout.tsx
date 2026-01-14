import "./globals.css";
import React from "react";
import Script from "next/script";

import ThemeProvider from "../components/ThemeProvider";
import Header from "../components/Header";
import Footer from "../components/Footer";
import { baseMetadata } from "../lib/metadata";
import {
    generateOrganizationSchema,
    generateSoftwareApplicationSchema,
} from "../lib/structured-data";

export const metadata = baseMetadata;

export const viewport = {
    width: "device-width",
    initialScale: 1,
    maximumScale: 5,
    userScalable: true,
};

export default function RootLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    const organizationSchema = generateOrganizationSchema();
    const softwareSchema = generateSoftwareApplicationSchema();

    return (
        <html lang="en" className="dark" suppressHydrationWarning>
            <head>
                <link rel="icon" href="/favicon.ico" sizes="any" />
                <link
                    rel="icon"
                    href="/icon.svg"
                    type="image/svg+xml"
                    sizes="any"
                />
                <link rel="apple-touch-icon" href="/apple-touch-icon.png" />
                <link rel="manifest" href="/manifest.json" />
                <meta name="theme-color" content="#1e293b" />
                <Script
                    id="schema-org-organization"
                    type="application/ld+json"
                    dangerouslySetInnerHTML={{
                        __html: JSON.stringify(organizationSchema),
                    }}
                />
                <Script
                    id="schema-org-software"
                    type="application/ld+json"
                    dangerouslySetInnerHTML={{
                        __html: JSON.stringify(softwareSchema),
                    }}
                />
            </head>
            <body className="bg-white text-gray-900 dark:bg-gray-950 dark:text-gray-100 transition-colors duration-300">
                <Script
                    id="theme-no-flash"
                    strategy="beforeInteractive"
                    dangerouslySetInnerHTML={{
                        __html: `(()=>{try{const s=localStorage.getItem('theme');const c=document.documentElement.classList;if(s==='light'){c.remove('dark');}else if(s==='dark'){c.add('dark');}else{c.add('dark');}}catch(e){document.documentElement.classList.add('dark');}})();`,
                    }}
                />
                <ThemeProvider>
                    <Header />
                    <main className="min-h-[60vh]">{children}</main>
                    <Footer />
                </ThemeProvider>
            </body>
        </html>
    );
}
