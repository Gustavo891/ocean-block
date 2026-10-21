package textura_studio

import java.io.File
import java.net.BindException
import kotlin.system.exitProcess

private const val DEFAULT_PORT = 7070

fun main() {
    System.setProperty("java.awt.headless", "true")
    val data = File(System.getProperty("studio.data") ?: System.getenv("TEXTURA_DATA") ?: "server/plugins/Textura")
    if (!File(data, "configs/ui").isDirectory) {
        System.err.println("Pasta do Textura não encontrada em ${data.absolutePath} (esperado configs/ui). Use TEXTURA_DATA.")
        exitProcess(1)
    }
    val web = System.getProperty("studio.web")?.let(::File)
    val port = System.getProperty("studio.port")?.toIntOrNull() ?: DEFAULT_PORT

    val studio = Studio(data)
    try {
        StudioServer(studio, web, port).start()
    } catch (error: BindException) {
        System.err.println("A porta $port já está em uso. Feche o outro Studio ou rode com -Dstudio.port=<porta>.")
        exitProcess(1)
    }
    println("Textura Studio em http://127.0.0.1:$port  (dados: ${data.absolutePath})")
    studio.assets().warnings.forEach { println("[aviso] $it") }
}
