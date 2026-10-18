package one_block.textura.http

import team.unnamed.creative.BuiltResourcePack
import team.unnamed.creative.server.handler.ResourcePackRequestHandler
import team.unnamed.creative.server.ResourcePackServer as CreativeServer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ResourcePackServer(private val port: Int, private val bind: String?) {

    private var server: CreativeServer? = null
    private var executor: ExecutorService? = null

    @Volatile
    private var handler: ResourcePackRequestHandler? = null

    fun start() {
        if (server != null) return

        val pool = Executors.newVirtualThreadPerTaskExecutor()
        val builder = CreativeServer.server()
        if (bind == null) builder.address(port) else builder.address(bind, port)

        val built = runCatching {
            builder
                .executor(pool)
                .handler { request, exchange ->
                    val current = handler
                    if (current == null) {
                        val response = "Not Found".toByteArray()
                        exchange.sendResponseHeaders(404, response.size.toLong())
                        exchange.responseBody.use { it.write(response) }
                        return@handler
                    }
                    current.onRequest(request, exchange)
                }
                .build()
        }.getOrElse {
            pool.shutdownNow()
            throw it
        }
        server = built
        executor = pool
        built.start()
    }

    fun stop() {
        server?.stop(0)
        server = null
        executor?.shutdownNow()
        executor = null
    }

    fun publish(bytes: ByteArray, hash: String) {
        handler = ResourcePackRequestHandler.fixed(BuiltResourcePack.of(bytes, hash))
    }
}
