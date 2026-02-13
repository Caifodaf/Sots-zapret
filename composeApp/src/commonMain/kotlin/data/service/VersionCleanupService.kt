package data.service

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VersionCleanupService {
    fun runCleanupIfNeeded() {
        val localAppData = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
        val sotsDir = Paths.get(localAppData, "Sots")
        if (hasVersionFile(sotsDir)) {
            Files.list(sotsDir).use { paths ->
                paths.filter { it.fileName.toString() != "win" }
                    .forEach { deleteRecursively(it) }
            }
        }
    }

    private fun hasVersionFile(dir: Path): Boolean {
        Files.newDirectoryStream(dir) { path ->
            val name = path.fileName.toString()
            name.endsWith(".version")
        }.use { stream ->
            return stream.iterator().hasNext()
        }
    }

    private fun deleteRecursively(path: Path) {
        if (Files.isDirectory(path)) {
            Files.list(path).use { stream ->
                stream.forEach { deleteRecursively(it) }
            }
        }
        try {
            Files.deleteIfExists(path)
        } catch (e: Exception) {
            // Можно залогировать, если нужно:
            // println("Не удалось удалить $path: ${e.message}")
        }
    }
} 