"use client";
import React from "react";

export default function CodeBlock({ children }: { children: React.ReactNode }) {
    return (
        <pre className="rounded-md bg-gray-900 p-4 text-white overflow-auto">
            {children}
        </pre>
    );
}
