"use client";
import { useTheme } from "./ThemeProvider";

export default function DarkModeToggle() {
    const { isDark, toggleTheme } = useTheme();
    return (
        <button
            aria-label="Toggle dark mode"
            className="px-2 py-1 rounded border"
            onClick={toggleTheme}
        >
            {isDark ? "🌙" : "☀️"}
        </button>
    );
}
