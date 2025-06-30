package com.scan.patterns

import java.util.regex.Pattern

/**
 * Database connection pattern definitions for detecting sensitive database credentials and
 * connection strings. This class provides comprehensive patterns for various database systems and
 * connection formats.
 */
object DatabasePatterns {

    /** Data class representing a database pattern with metadata */
    data class DatabasePattern(
            val name: String,
            val pattern: Pattern,
            val description: String,
            val severity: Severity = Severity.HIGH,
            val confidence: Confidence = Confidence.HIGH,
            val tags: Set<String> = emptySet()
    )

    enum class Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    enum class Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    /** Complete list of all database patterns */
    val ALL_PATTERNS: List<DatabasePattern> by lazy {
        listOf(
                // Generic database URL patterns
                *GENERIC_DB_URLS.toTypedArray(),

                // Specific database systems
                *MYSQL_PATTERNS.toTypedArray(),
                *POSTGRESQL_PATTERNS.toTypedArray(),
                *MONGODB_PATTERNS.toTypedArray(),
                *REDIS_PATTERNS.toTypedArray(),
                *ORACLE_PATTERNS.toTypedArray(),
                *SQL_SERVER_PATTERNS.toTypedArray(),
                *SQLITE_PATTERNS.toTypedArray(),
                *CASSANDRA_PATTERNS.toTypedArray(),
                *ELASTICSEARCH_PATTERNS.toTypedArray(),
                *INFLUXDB_PATTERNS.toTypedArray(),

                // Cloud database services
                *AWS_DATABASE_PATTERNS.toTypedArray(),
                *AZURE_DATABASE_PATTERNS.toTypedArray(),
                *GCP_DATABASE_PATTERNS.toTypedArray(),

                // Database credentials
                *DATABASE_CREDENTIALS.toTypedArray(),

                // Configuration patterns
                *CONFIG_FILE_PATTERNS.toTypedArray()
        )
    }

    /** Generic database URL patterns that might contain credentials */
    private val GENERIC_DB_URLS =
            listOf(
                    DatabasePattern(
                            name = "generic_db_url_with_credentials",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:jdbc:|mongodb\+srv://|redis://|postgresql://|mysql://|oracle:|sqlserver:)(?:[^:\s]+):(?:[^@\s]+)@(?:[^/\s]+)""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Generic database URL containing username and password",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("database", "credentials", "url")
                    ),
                    DatabasePattern(
                            name = "database_connection_string",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:server|host|hostname|data\s*source|initial\s*catalog|database)\s*=\s*[^;]+;\s*(?:user\s*id|uid|username|user)\s*=\s*[^;]+;\s*(?:password|pwd)\s*=\s*[^;]+""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Database connection string with credentials",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("database", "connection-string", "credentials")
                    )
            )

    /** MySQL specific patterns */
    private val MYSQL_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "mysql_jdbc_url",
                            pattern =
                                    Pattern.compile(
                                            """jdbc:mysql://(?:[^:\s]+):(?:[^@\s]+)@[^/\s]+(?:/[^\s?]+)?(?:\?[^\s]*)?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "MySQL JDBC URL with embedded credentials",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("mysql", "jdbc", "credentials")
                    ),
                    DatabasePattern(
                            name = "mysql_connection_string",
                            pattern =
                                    Pattern.compile(
                                            """(?i)server\s*=\s*[^;]+;\s*(?:port\s*=\s*\d+;\s*)?database\s*=\s*[^;]+;\s*uid\s*=\s*[^;]+;\s*pwd\s*=\s*[^;]+""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "MySQL connection string format",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("mysql", "connection-string")
                    ),
                    DatabasePattern(
                            name = "mysql_root_password",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:mysql_root_password|root_password)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "MySQL root password configuration",
                            severity = Severity.CRITICAL,
                            confidence = Confidence.HIGH,
                            tags = setOf("mysql", "root", "password")
                    )
            )

    /** PostgreSQL specific patterns */
    private val POSTGRESQL_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "postgresql_jdbc_url",
                            pattern =
                                    Pattern.compile(
                                            """jdbc:postgresql://(?:[^:\s]+):(?:[^@\s]+)@[^/\s]+(?:/[^\s?]+)?(?:\?[^\s]*)?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "PostgreSQL JDBC URL with embedded credentials",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("postgresql", "jdbc", "credentials")
                    ),
                    DatabasePattern(
                            name = "postgresql_connection_url",
                            pattern =
                                    Pattern.compile(
                                            """postgresql://(?:[^:\s]+):(?:[^@\s]+)@[^/\s]+(?:/[^\s?]+)?(?:\?[^\s]*)?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "PostgreSQL connection URL with credentials",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("postgresql", "credentials")
                    ),
                    DatabasePattern(
                            name = "postgres_env_vars",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:PGPASSWORD|POSTGRES_PASSWORD|POSTGRESQL_PASSWORD)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "PostgreSQL password environment variables",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("postgresql", "environment", "password")
                    )
            )

    /** MongoDB specific patterns */
    private val MONGODB_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "mongodb_connection_string",
                            pattern =
                                    Pattern.compile(
                                            """mongodb(?:\+srv)?://(?:[^:\s]+):(?:[^@\s]+)@[^/\s]+(?:/[^\s?]+)?(?:\?[^\s]*)?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "MongoDB connection string with credentials",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("mongodb", "credentials")
                    ),
                    DatabasePattern(
                            name = "mongodb_atlas_connection",
                            pattern =
                                    Pattern.compile(
                                            """mongodb\+srv://[^:]+:[^@]+@[^.]+\.mongodb\.net""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "MongoDB Atlas cloud connection string",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("mongodb", "atlas", "cloud")
                    )
            )

    /** Redis specific patterns */
    private val REDIS_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "redis_connection_url",
                            pattern =
                                    Pattern.compile(
                                            """redis://(?::[^@\s]+@)?[^/\s]+(?:/\d+)?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Redis connection URL with password",
                            severity = Severity.HIGH,
                            confidence = Confidence.MEDIUM,
                            tags = setOf("redis", "credentials")
                    ),
                    DatabasePattern(
                            name = "redis_auth_password",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:redis_password|redis_auth|auth)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Redis authentication password",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("redis", "auth", "password")
                    )
            )

    /** Oracle database patterns */
    private val ORACLE_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "oracle_jdbc_url",
                            pattern =
                                    Pattern.compile(
                                            """jdbc:oracle:thin:(?:[^/\s]+)/(?:[^@\s]+)@[^:\s]+:\d+[:/][^\s]+""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Oracle JDBC thin client URL with credentials",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("oracle", "jdbc", "credentials")
                    ),
                    DatabasePattern(
                            name = "oracle_tns_connection",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:user|username)\s*=\s*[^;\s]+;\s*password\s*=\s*[^;\s]+;\s*data\s*source\s*=\s*[^;\s]+""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Oracle TNS connection string",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("oracle", "tns", "credentials")
                    )
            )

    /** SQL Server patterns */
    private val SQL_SERVER_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "sqlserver_jdbc_url",
                            pattern =
                                    Pattern.compile(
                                            """jdbc:sqlserver://[^;]+;(?:.*?)user=([^;]+);(?:.*?)password=([^;]+)""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "SQL Server JDBC URL with credentials",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("sqlserver", "jdbc", "credentials")
                    ),
                    DatabasePattern(
                            name = "sqlserver_connection_string",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:server|data\s*source)\s*=\s*[^;]+;\s*(?:initial\s*catalog|database)\s*=\s*[^;]+;\s*(?:user\s*id|uid)\s*=\s*[^;]+;\s*password\s*=\s*[^;]+""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "SQL Server connection string",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("sqlserver", "connection-string")
                    )
            )

    /** SQLite patterns */
    private val SQLITE_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "sqlite_jdbc_url",
                            pattern =
                                    Pattern.compile(
                                            """jdbc:sqlite:[^\s;]+\.db(?:\?[^\s]*)?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "SQLite JDBC URL",
                            severity = Severity.MEDIUM,
                            confidence = Confidence.HIGH,
                            tags = setOf("sqlite", "jdbc", "file")
                    )
            )

    /** Cassandra patterns */
    private val CASSANDRA_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "cassandra_credentials",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:cassandra_username|cassandra_password)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Cassandra database credentials",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("cassandra", "credentials")
                    )
            )

    /** Elasticsearch patterns */
    private val ELASTICSEARCH_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "elasticsearch_url_with_auth",
                            pattern =
                                    Pattern.compile(
                                            """https?://[^:\s]+:[^@\s]+@[^/\s]+(?::\d+)?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Elasticsearch URL with authentication",
                            severity = Severity.HIGH,
                            confidence = Confidence.MEDIUM,
                            tags = setOf("elasticsearch", "credentials", "url")
                    )
            )

    /** InfluxDB patterns */
    private val INFLUXDB_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "influxdb_token",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:influx_token|influxdb_token)\s*[=:]\s*['"]?([a-zA-Z0-9_-]{20,})['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "InfluxDB authentication token",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("influxdb", "token", "auth")
                    )
            )

    /** AWS database service patterns */
    private val AWS_DATABASE_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "aws_rds_endpoint",
                            pattern =
                                    Pattern.compile(
                                            """[a-zA-Z0-9-]+\.rds\.amazonaws\.com""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "AWS RDS endpoint",
                            severity = Severity.MEDIUM,
                            confidence = Confidence.HIGH,
                            tags = setOf("aws", "rds", "endpoint")
                    ),
                    DatabasePattern(
                            name = "aws_dynamodb_credentials",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:aws_access_key_id|aws_secret_access_key)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "AWS credentials for DynamoDB access",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("aws", "dynamodb", "credentials")
                    )
            )

    /** Azure database service patterns */
    private val AZURE_DATABASE_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "azure_sql_connection",
                            pattern =
                                    Pattern.compile(
                                            """(?i)server\s*=\s*[^.]+\.database\.windows\.net;\s*database\s*=\s*[^;]+;\s*user\s*id\s*=\s*[^;]+;\s*password\s*=\s*[^;]+""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Azure SQL Database connection string",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("azure", "sql", "credentials")
                    ),
                    DatabasePattern(
                            name = "azure_cosmos_key",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:azure_cosmos_key|cosmos_key|primary_key)\s*[=:]\s*['"]?([a-zA-Z0-9+/=]{64,})['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Azure Cosmos DB primary key",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("azure", "cosmos", "key")
                    )
            )

    /** Google Cloud Platform database patterns */
    private val GCP_DATABASE_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "gcp_cloud_sql_connection",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:cloud_sql_connection_name|sql_connection_name)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Google Cloud SQL connection name",
                            severity = Severity.MEDIUM,
                            confidence = Confidence.HIGH,
                            tags = setOf("gcp", "cloud-sql", "connection")
                    ),
                    DatabasePattern(
                            name = "firestore_credentials",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:firestore_key|firebase_key)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Google Firestore/Firebase credentials",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("gcp", "firestore", "firebase", "credentials")
                    )
            )

    /** Generic database credential patterns */
    private val DATABASE_CREDENTIALS =
            listOf(
                    DatabasePattern(
                            name = "db_password_env",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:DB_PASSWORD|DATABASE_PASSWORD|DB_PASS)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Database password environment variable",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("database", "password", "environment")
                    ),
                    DatabasePattern(
                            name = "db_username_env",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:DB_USERNAME|DATABASE_USER|DB_USER)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Database username environment variable",
                            severity = Severity.MEDIUM,
                            confidence = Confidence.HIGH,
                            tags = setOf("database", "username", "environment")
                    ),
                    DatabasePattern(
                            name = "db_host_env",
                            pattern =
                                    Pattern.compile(
                                            """(?i)(?:DB_HOST|DATABASE_HOST|DB_HOSTNAME)\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Database host environment variable",
                            severity = Severity.LOW,
                            confidence = Confidence.MEDIUM,
                            tags = setOf("database", "host", "environment")
                    )
            )

    /** Configuration file specific patterns */
    private val CONFIG_FILE_PATTERNS =
            listOf(
                    DatabasePattern(
                            name = "hibernate_connection_password",
                            pattern =
                                    Pattern.compile(
                                            """(?i)hibernate\.connection\.password\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Hibernate database connection password",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("hibernate", "password", "config")
                    ),
                    DatabasePattern(
                            name = "spring_datasource_password",
                            pattern =
                                    Pattern.compile(
                                            """(?i)spring\.datasource\.password\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Spring Boot datasource password",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("spring", "datasource", "password")
                    ),
                    DatabasePattern(
                            name = "quarkus_datasource_password",
                            pattern =
                                    Pattern.compile(
                                            """(?i)quarkus\.datasource\.password\s*[=:]\s*['"]?([^'"\s;]+)['"]?""",
                                            Pattern.CASE_INSENSITIVE
                                    ),
                            description = "Quarkus datasource password",
                            severity = Severity.HIGH,
                            confidence = Confidence.HIGH,
                            tags = setOf("quarkus", "datasource", "password")
                    )
            )

    /** Get patterns by database type */
    fun getPatternsByType(type: String): List<DatabasePattern> {
        return when (type.lowercase()) {
            "mysql" -> MYSQL_PATTERNS
            "postgresql", "postgres" -> POSTGRESQL_PATTERNS
            "mongodb", "mongo" -> MONGODB_PATTERNS
            "redis" -> REDIS_PATTERNS
            "oracle" -> ORACLE_PATTERNS
            "sqlserver", "mssql" -> SQL_SERVER_PATTERNS
            "sqlite" -> SQLITE_PATTERNS
            "cassandra" -> CASSANDRA_PATTERNS
            "elasticsearch", "elastic" -> ELASTICSEARCH_PATTERNS
            "influxdb" -> INFLUXDB_PATTERNS
            "aws" -> AWS_DATABASE_PATTERNS
            "azure" -> AZURE_DATABASE_PATTERNS
            "gcp", "google" -> GCP_DATABASE_PATTERNS
            else -> emptyList()
        }
    }

    /** Get patterns by severity level */
    fun getPatternsBySeverity(severity: Severity): List<DatabasePattern> {
        return ALL_PATTERNS.filter { it.severity == severity }
    }

    /** Get patterns by confidence level */
    fun getPatternsByConfidence(confidence: Confidence): List<DatabasePattern> {
        return ALL_PATTERNS.filter { it.confidence == confidence }
    }

    /** Get patterns by tags */
    fun getPatternsByTags(tags: Set<String>): List<DatabasePattern> {
        return ALL_PATTERNS.filter { pattern ->
            tags.any { tag -> pattern.tags.contains(tag.lowercase()) }
        }
    }

    /** Get high-risk patterns (HIGH or CRITICAL severity) */
    fun getHighRiskPatterns(): List<DatabasePattern> {
        return ALL_PATTERNS.filter {
            it.severity == Severity.HIGH || it.severity == Severity.CRITICAL
        }
    }

    /** Get credential-related patterns only */
    fun getCredentialPatterns(): List<DatabasePattern> {
        return ALL_PATTERNS.filter {
            it.tags.contains("credentials") ||
                    it.tags.contains("password") ||
                    it.tags.contains("auth") ||
                    it.tags.contains("token") ||
                    it.tags.contains("key")
        }
    }
}
