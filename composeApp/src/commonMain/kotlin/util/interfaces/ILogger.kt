package util.interfaces

interface ILogger {
    fun info(msg: String)
    fun error(msg: String, throwable: Throwable? = null)
    fun warn(msg: String, throwable: Throwable? = null)
    fun debug(msg: String)
}