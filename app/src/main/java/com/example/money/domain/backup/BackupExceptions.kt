package com.example.money.domain.backup

sealed class BackupException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class BackupFormatException(
    message: String,
    cause: Throwable? = null,
) : BackupException(message, cause)

class UnsupportedBackupVersionException(
    val actualVersion: Int,
) : BackupException("Unsupported backup format version: $actualVersion")

class BackupValidationException(
    message: String,
) : BackupException(message)

class BackupIntegrityException(
    message: String,
) : BackupException(message)

open class BackupEnvelopeException(
    message: String,
    cause: Throwable? = null,
) : BackupException(message, cause)

class UnsupportedBackupEnvelopeVersionException(
    val actualVersion: Int,
) : BackupException("Unsupported encrypted backup envelope version: $actualVersion")

class BackupAuthenticationException(
    cause: Throwable? = null,
) : BackupEnvelopeException(
    message = "Backup password is incorrect or the encrypted backup is corrupted",
    cause = cause,
)
