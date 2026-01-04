import React from "react";

export default function FeatureCard({
    title,
    desc,
}: {
    title: string;
    desc: string;
}) {
    return (
        <div className="rounded-lg border border-gray-200 dark:border-white/20 p-4 sm:p-6 hover:border-gray-300 dark:hover:border-white/40 transition-colors cursor-pointer focus-within:ring-2 focus-within:ring-primary-500">
            <h3 className="font-semibold text-base sm:text-lg leading-tight">
                {title}
            </h3>
            <p className="mt-2 sm:mt-3 text-sm sm:text-base leading-relaxed text-gray-700 dark:text-gray-300">
                {desc}
            </p>
        </div>
    );
}
