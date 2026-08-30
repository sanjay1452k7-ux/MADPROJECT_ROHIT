package com.cit.attendance.core.common

/**
 * The result type crossing every repository boundary.
 *
 * Repositories never throw into the presentation layer and never leak a
 * transport-level exception; they translate failures into [AppError], which the
 * UI turns into human wording (Section 37).
 */
sealed interface Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(data))
    is Outcome.Failure -> this
}

fun <T> Outcome<T>.dataOrNull(): T? = (this as? Outcome.Success)?.data
