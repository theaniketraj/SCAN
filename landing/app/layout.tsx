
import "./globals.css";
import React from "react";
import Script from "next/script";



import ThemeProvider from "../components/ThemeProvider";
import Header from "../components/Header";
import Footer from "../components/Footer";

export const metadata = {
    title: "SCAN — Secret detection for Gradle builds",
    description: "Catch API keys, tokens and credentials before they reach source control.",
    viewport: "width=device-width, initial-scale=1",
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
    return (
        <html lang="en" className="dark" suppressHydrationWarning>
            <body className="bg-white text-gray-900 dark:bg-gray-950 dark:text-gray-100 transition-colors duration-300">
                <Script id="theme-no-flash" strategy="beforeInteractive"
                    dangerouslySetInnerHTML={{
                        __html: `(()=>{try{const s=localStorage.getItem('theme');const c=document.documentElement.classList;if(s==='light'){c.remove('dark');}else if(s==='dark'){c.add('dark');}else{c.add('dark');}}catch(e){document.documentElement.classList.add('dark');}})();`
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
