package gosbry

// доменные ошибки; в HTTP-ответы их превращает StatusPages.kt

class LogParseException(message: String) : Exception(message)

class UnsupportedFileTypeException(fileName: String?) : RuntimeException("Unsupported file type: $fileName")

class UnsupportedLoaderException(loader: String) : RuntimeException("Unsupported loader: $loader")

class FileTooLargeException(maxBytes: Long) : RuntimeException("File is larger than $maxBytes bytes")

class PlayerTooLongException(maxLength: Int) : RuntimeException("Player name is longer than $maxLength characters")
