package com.scan.integration

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.util.Base64

/**
 * GitHub Code Scanning API integration for uploading SARIF reports.
 *
 * This class handles authentication and communication with GitHub's Code Scanning API
 * to upload security scan results as code scanning alerts.
 *
 * @see https://docs.github.com/en/rest/code-scanning
 */
class GitHubCodeScanning(
    private val githubToken: String,
    private val owner: String,
    private val repo: String,
    private val ref: String = "refs/heads/main",
    private val apiUrl: String = "https://api.github.com"
) {

    companion object {
        private const val API_VERSION = "2022-11-28"
        private const val UPLOAD_ENDPOINT = "/repos/{owner}/{repo}/code-scanning/sarifs"
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB limit for GitHub API

        /**
         * Creates a GitHubCodeScanning instance from environment variables.
         *
         * Required environment variables:
         * - GITHUB_TOKEN: Personal access token or GitHub Actions token
         * - GITHUB_REPOSITORY: Repository in format "owner/repo"
         * - GITHUB_REF: Git reference (branch/tag), defaults to "refs/heads/main"
         */
        fun fromEnvironment(): GitHubCodeScanning? {
            val token = System.getenv("GITHUB_TOKEN") ?: return null
            val repository = System.getenv("GITHUB_REPOSITORY") ?: return null
            val ref = System.getenv("GITHUB_REF") ?: "refs/heads/main"

            val parts = repository.split("/")
            if (parts.size != 2) return null

            return GitHubCodeScanning(
                githubToken = token,
                owner = parts[0],
                repo = parts[1],
                ref = ref
            )
        }
    }

    /**
     * Uploads a SARIF file to GitHub Code Scanning.
     *
     * @param sarifFile The SARIF file to upload
     * @param commitSha The SHA of the commit being analyzed
     * @return GitHubUploadResult containing status and details
     */
    fun uploadSarif(sarifFile: File, commitSha: String): GitHubUploadResult {
        if (!sarifFile.exists()) {
            return GitHubUploadResult(
                success = false,
                message = "SARIF file not found: ${sarifFile.absolutePath}"
            )
        }

        if (sarifFile.length() > MAX_FILE_SIZE) {
            return GitHubUploadResult(
                success = false,
                message = "SARIF file exceeds maximum size of ${MAX_FILE_SIZE / (1024 * 1024)}MB"
            )
        }

        try {
            // Read and encode SARIF file
            val sarifContent = Files.readAllBytes(sarifFile.toPath())
            val encodedSarif = Base64.getEncoder().encodeToString(sarifContent)

            // Create upload payload
            val payload = createUploadPayload(encodedSarif, commitSha)

            // Upload to GitHub
            val response = uploadToGitHub(payload)

            return response
        } catch (e: Exception) {
            return GitHubUploadResult(
                success = false,
                message = "Failed to upload SARIF: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Creates the JSON payload for GitHub API.
     */
    private fun createUploadPayload(encodedSarif: String, commitSha: String): String {
        return buildString {
            append("{\n")
            append("  \"commit_sha\": \"$commitSha\",\n")
            append("  \"ref\": \"$ref\",\n")
            append("  \"sarif\": \"$encodedSarif\",\n")
            append("  \"tool_name\": \"SCAN\"\n")
            append("}")
        }
    }

    /**
     * Uploads payload to GitHub Code Scanning API.
     */
    private fun uploadToGitHub(payload: String): GitHubUploadResult {
        val endpoint = UPLOAD_ENDPOINT
            .replace("{owner}", owner)
            .replace("{repo}", repo)

        val url = URL("$apiUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $githubToken")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", API_VERSION)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Write payload
            connection.outputStream.use { it.write(payload.toByteArray()) }

            // Read response
            val responseCode = connection.responseCode
            val responseBody = if (responseCode < 400) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            return when (responseCode) {
                202 -> {
                    // Accepted - processing in background
                    val uploadId = parseUploadId(responseBody)
                    GitHubUploadResult(
                        success = true,
                        message = "SARIF uploaded successfully. Processing in background.",
                        uploadId = uploadId,
                        responseCode = responseCode
                    )
                }
                in 200..299 -> {
                    GitHubUploadResult(
                        success = true,
                        message = "SARIF uploaded successfully",
                        responseCode = responseCode
                    )
                }
                401 -> {
                    GitHubUploadResult(
                        success = false,
                        message = "Authentication failed. Check GITHUB_TOKEN permissions.",
                        responseCode = responseCode
                    )
                }
                403 -> {
                    GitHubUploadResult(
                        success = false,
                        message = "Forbidden. Code scanning may not be enabled for this repository.",
                        responseCode = responseCode
                    )
                }
                404 -> {
                    GitHubUploadResult(
                        success = false,
                        message = "Repository not found: $owner/$repo",
                        responseCode = responseCode
                    )
                }
                else -> {
                    GitHubUploadResult(
                        success = false,
                        message = "Upload failed with HTTP $responseCode: $responseBody",
                        responseCode = responseCode
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parses the upload ID from the GitHub API response.
     */
    private fun parseUploadId(responseBody: String): String? {
        // Simple JSON parsing for id field
        val idPattern = """"id"\s*:\s*"([^"]+)"""".toRegex()
        return idPattern.find(responseBody)?.groupValues?.get(1)
    }

    /**
     * Gets the current commit SHA from git repository.
     */
    fun getCurrentCommitSha(workingDirectory: File = File(".")): String? {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(workingDirectory)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            process.waitFor()
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validates the GitHub configuration.
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (githubToken.isBlank()) {
            errors.add("GitHub token is required")
        }

        if (owner.isBlank()) {
            errors.add("Repository owner is required")
        }

        if (repo.isBlank()) {
            errors.add("Repository name is required")
        }

        if (ref.isBlank()) {
            errors.add("Git reference is required")
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors
        )
    }
}

/**
 * Result of a GitHub SARIF upload operation.
 */
data class GitHubUploadResult(
    val success: Boolean,
    val message: String,
    val uploadId: String? = null,
    val responseCode: Int? = null,
    val error: Throwable? = null
)

/**
 * Result of validation checks.
 */
data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList()
)
