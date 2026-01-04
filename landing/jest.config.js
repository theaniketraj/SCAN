module.exports = {
    testEnvironment: "jsdom",
    testMatch: [
        "**/__tests__/**/*.[jt]s?(x)",
        "**/?(*.)+(spec|test).[jt]s?(x)",
    ],
    testPathIgnorePatterns: ["/node_modules/", "/e2e/", "/.next/", "/out/"],
    moduleNameMapper: {
        "^@/(.*)$": "<rootDir>/$1",
    },
};
