import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class Result(val file: String, val lines: Int)

fun main() {
    print("Введите путь к файлу: ")
    val path = readLine()?.trim() ?: return

    try {
        val lines = File(path).useLines { it.count() }
        val result = Result(File(path).name, lines)
        val json = Json { prettyPrint = true }.encodeToString(result)

        println("\nРезультат: $json")
    } catch (e: Exception) {
        println("Ошибка: ${e.message}")
    }
}