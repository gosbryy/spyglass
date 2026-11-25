package gosbry

// доменные ошибки; в HTTP-ответы их превращает StatusPages.kt

class LogParseException(message: String) : Exception(message)

class UnsupportedFileTypeException(fileName: String?) : RuntimeException("Unsupported file type: $fileName")

class UnsupportedLoaderException(loader: String) : RuntimeException("Unsupported loader: $loader")

class FileTooLargeException(maxBytes: Long) : RuntimeException("File is larger than $maxBytes bytes")

class FileNameTooLongException(maxLength: Int) : RuntimeException("File name is longer than $maxLength characters")

class InvalidPlayerLengthException(maxLength: Int, minLength: Int? = null) : RuntimeException(
    if (minLength == null) "Player name must be at most $maxLength characters"
    else "Player name must be between $minLength and $maxLength characters",
)

class InvalidEmailException(message: String) : RuntimeException(message)

class InvalidPasswordLengthException(minLength: Int, maxLength: Int) :
    RuntimeException("Password must be between $minLength and $maxLength characters")
