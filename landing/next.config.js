/** @type {import('next').NextConfig} */
const isCI = process.env.GITHUB_ACTIONS === "true";
const [owner, repo] = process.env.GITHUB_REPOSITORY
    ? process.env.GITHUB_REPOSITORY.split("/")
    : [null, ""];
const isUserOrOrgSite = repo
    ? repo.toLowerCase().endsWith(".github.io")
    : false;
const envBasePath = process.env.BASE_PATH
    ? process.env.BASE_PATH
    : isCI && !isUserOrOrgSite && repo
      ? `/${repo}`
      : "";
const basePath = envBasePath;

const nextConfig = {
    reactStrictMode: true,
    // Enable static export for GitHub Pages
    output: "export",
    trailingSlash: true,
    // Apply basePath only when provided (e.g., project pages)
    basePath: basePath || undefined,
    assetPrefix: basePath ? `${basePath}/` : undefined,
    images: { unoptimized: true },
};
module.exports = nextConfig;
