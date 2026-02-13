package util

import util.interfaces.ILogger

inline fun <T> Result<T>.onSuccessOrLog(action: (T) -> Unit, logger: ILogger, logTag: String = "Result") {
    if (isSuccess) action(getOrThrow())
    else logger.error("[$logTag] Error: ${exceptionOrNull()?.message}")
}

inline fun <T> Result<T>.onFailureLog(logger: ILogger, logTag: String = "Result", action: (Throwable) -> Unit = {}) {
    if (isFailure) {
        logger.error("[$logTag] Error: ${exceptionOrNull()?.message}")
        exceptionOrNull()?.let { action(it) }
    }
}

