package de.gmuth.http

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.lang.System.setProperty
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger.getLogger

// requires Java >=11
class JavaHttpClient(config: Config = Config()) : HttpClient(config) {

    companion object {
        val log = getLogger(JavaHttpClient::javaClass.name)
        fun isSupported() = try {
            java.net.http.HttpClient.newHttpClient()
            true
        } catch (exception: ClassNotFoundException) {
            log.log(Level.FINER, exception, { "HttpClient not found" })
            false
        }.apply {
            log.fine { "Java HttpClient supported: $this" }
        }
    }

    init {
        log.fine { "JavaHttpClient created" }
        if (!config.verifySSLHostname)
            setProperty("jdk.internal.httpclient.disableHostnameVerification", true.toString())
    }

    val httpClient by lazy {
        java.net.http.HttpClient.newBuilder().run {
            config.sslContext?.let { sslContext(it) }
            build()
        }
    }

    override fun post(
        uri: URI,
        contentType: String,
        writeContent: (OutputStream) -> Unit,
        chunked: Boolean
    ): Response {
        val content = ByteArrayOutputStream().also { writeContent(it) }.toByteArray()
        val request = HttpRequest.newBuilder().run {
            with(config) {
                timeout(Duration.ofMillis(timeout.toLong()))
                userAgent?.let { header("User-Agent", it) }
                acceptEncoding?.let { header("Accept-Encoding", it) }
                //basicAuth?.let { header("Authorization", it.authorization()) }
            }
            header("Content-Type", contentType)
            POST(BodyPublishers.ofInputStream { ByteArrayInputStream(content) })
            uri(uri)
            build()
        }
        return httpClient.send(request, BodyHandlers.ofInputStream()).run {
            Response(
                statusCode(),
                headers().firstValue("server").run { if (isPresent) get() else null },
                headers().firstValue("content-type").get(),
                body()
            )
        }
    }
}