import { ImageResponse } from "next/og";

export const runtime = "edge";
export const alt = "SCAN - Secret Detection for Gradle Builds";
export const size = {
    width: 1200,
    height: 630,
};
export const contentType = "image/png";

export default async function Image() {
    return new ImageResponse(
        (
            <div
                style={{
                    fontSize: 128,
                    background: "linear-gradient(135deg, #1e293b 0%, #0f172a 100%)",
                    width: "100%",
                    height: "100%",
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "white",
                    padding: "40px",
                }}
            >
                <div
                    style={{
                        display: "flex",
                        fontSize: 80,
                        fontWeight: "bold",
                        marginBottom: 20,
                        background:
                            "linear-gradient(90deg, #60a5fa 0%, #3b82f6 100%)",
                        backgroundClip: "text",
                        color: "transparent",
                    }}
                >
                    🔐 SCAN
                </div>
                <div
                    style={{
                        fontSize: 40,
                        fontWeight: 600,
                        textAlign: "center",
                        marginBottom: 20,
                    }}
                >
                    Secret Detection for Gradle Builds
                </div>
                <div
                    style={{
                        fontSize: 28,
                        textAlign: "center",
                        color: "#94a3b8",
                        maxWidth: "80%",
                    }}
                >
                    Catch API keys, tokens and credentials before they reach
                    version control
                </div>
            </div>
        ),
        {
            ...size,
        }
    );
}
