package de.gmuth.http

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.Base64.getEncoder
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import kotlin.text.Charsets.UTF_8

abstract class HttpClient(val config: Config) {

    data class Config(
        var timeout: Int = 30000, // milli seconds
        var userAgent: String? = null,
        var user: String? = null,
        var password: String? = null,
        var sslContext: SSLContext? = null,
        // trust any certificate: sslContextForAnyCertificate()
        // use individual certificate: sslContext(loadCertificate(FileInputStream("printer.pem")))
        // use truststore: sslContext(loadKeyStore(FileInputStream("printer.jks"), "changeit"))
        var verifySSLHostname: Boolean = true,
        var accept: String? = "application/ipp", // avoid 'text/html' with sun.net.www.protocol.http.HttpURLConnection
        var acceptEncoding: String? = "identity", // avoid 'gzip' with Androids OkHttp
        var debugLogging: Boolean = false
    ) {
        fun trustAnyCertificateAndSSLHostname() {
            sslContext = SSLHelper.sslContextForAnyCertificate()
            verifySSLHostname = false
        }

        fun authorization() =
            "Basic " + getEncoder().encodeToString("$user:$password".toByteArray(UTF_8))
    }

    fun basicAuth(user: String, password: String) {
        config.user = user
        config.password = password
    }

    data class Response(
        val status: Int,
        val server: String?,
        val contentType: String?,
        val contentStream: InputStream?
    )

    class Exception(
        message: String,
        cause: Throwable? = null,
        val responseCode: Int,
        val responseMessage: String,
        val responseHeaderFields: Map<String?, List<String>>,
        val responseStream: InputStream
    ) : IOException(message, cause) {
        fun log(logger: Logger, level: Level = Level.SEVERE) = logger.run {
            log(level) { message }
            log(level) { "HTTP Response: code=$responseCode, message=$responseMessage" }
            for ((key, values) in responseHeaderFields) {
                log(level) { "$key = $values" }
            }
            log(level) { "HTTP Content:\n" + responseStream.bufferedReader().use { it.readText() } }
        }
    }


    abstract fun post(
        uri: URI,
        contentType: String,
        writeContent: (OutputStream) -> Unit,
        chunked: Boolean = false
    ): Response

}