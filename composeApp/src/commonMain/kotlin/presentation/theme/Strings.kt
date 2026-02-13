package theme

import java.io.File
import java.io.InputStreamReader
import java.util.Properties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalLang = staticCompositionLocalOf { "ru" }

object Strings {
    @Volatile
    private var cachedBundle: Properties? = null
    @Volatile
    private var cachedLang: String? = null

    private fun getBundle(lang: String): Properties {
        if (cachedBundle != null && cachedLang == lang) {
            return cachedBundle!!
        }
        val fileName = when (lang) {
            "ru" -> "strings_ru.properties"
            "en" -> "strings_en.properties"
            else -> "strings_en.properties"
        }
        val props = Properties()
        try {
            val stream = Strings::class.java.classLoader.getResourceAsStream(fileName)
            if (stream != null) {
                props.load(InputStreamReader(stream, Charsets.UTF_8))
                cachedBundle = props
                cachedLang = lang
            }
        } catch (_: Exception) {}
        return props
    }

    @Composable
    fun get(key: String): String {
        val lang = LocalLang.current
        return getBundle(lang).getProperty(key) ?: key
    }

    @Composable
    fun getWithParams(key: String, vararg params: String): String {
        val template = get(key)
        return params.foldIndexed(template) { index, result, param ->
            result.replace("{${index}}", param)
        }
    }

    fun restartApplication() {
        try {
            val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            val currentJar = File(Strings::class.java.protectionDomain.codeSource.location.toURI().path)

            if (!currentJar.name.endsWith(".jar")) {
                return
            }

            val command = arrayOf(javaBin, "-jar", currentJar.absolutePath)
            val processBuilder = ProcessBuilder(*command)
            processBuilder.start()

            System.exit(0)
        } catch (e: Exception) {
            //LogManager.error("Error when restarting the application", e)
        }
    }
}