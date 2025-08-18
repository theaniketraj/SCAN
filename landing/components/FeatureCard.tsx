import React from "react"

export default function FeatureCard({ title, desc }: { title: string; desc: string }) {
    return (
        <div className="rounded-lg border border-white/20 p-4 sm:p-6 hover:border-white/40 transition-colors">
            <h3 className="font-semibold text-base sm:text-lg">{title}</h3>
            <p className="mt-2 text-sm sm:text-base leading-relaxed">{desc}</p>
        </div>
    )
}