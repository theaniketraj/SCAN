import React from "react"

export default function FeatureCard({ title, desc }: { title: string; desc: string }) {
    return (
        <div className="rounded-lg border p-4">
            <h3 className="font-semibold">{title}</h3>
            <p className="mt-2 text-sm">{desc}</p>
        </div>
    )
}