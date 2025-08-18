import React from "react";

export default function DocsLayout({ children }: { children: React.ReactNode }) {
    return (
        <div className="container mx-auto px-6 py-12">
            {children}
        </div>
    );
}