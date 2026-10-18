package one_block.textura.pack

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object PackHash {

    /**
     * Calcula o SHA-1 de [file] em hexadecimal.
     *
     * Propaga [java.io.IOException]/erros em vez de mascarar a falha: um hash vazio
     * faria o cliente baixar o pack sem validação de integridade a cada conexão.
     */
    fun calculateSHA1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        FileInputStream(file).use { fis ->
            val byteArray = ByteArray(8192)
            var bytesCount: Int
            while (fis.read(byteArray).also { bytesCount = it } != -1) {
                digest.update(byteArray, 0, bytesCount)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
