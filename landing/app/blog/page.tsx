import React from "react";
import Link from "next/link";

export default function BlogIndex() {
    return (
        <div className="container mx-auto px-6 py-8">
            <h1 className="text-3xl font-bold">Blog</h1>
            <article className="mt-6">
                <Link href="/blog/posts/2025-08-16-introducing-scan">
                    Introducing SCAN — an intelligent Gradle plugin
                </Link>
            </article>
        </div>
    );
}
