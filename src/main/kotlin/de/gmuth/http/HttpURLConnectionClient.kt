package de.gmuth.http

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger.getLogger
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

class HttpURLConnectionClient(config: Config = Config()) : HttpClient(config) {

    val log = getLogger(javaClass.name)

    init {
        log.fine { "HttpURLConnectionClient created" }
        if (config.debugLogging) {
            getLogger("sun.net.www.protocol.http.HttpURLConnection").level = Level.FINER
        }
    }

    override fun post(
        uri: URI, contentType: String, writeContent: (OutputStream) -> Unit, chunked: Boolean
    ): Response {
        with(uri.toURL().openConnection() as HttpURLConnection) {
            if (this is HttpsURLConnection && config.sslContext != null) {
                sslSocketFactory = config.sslContext!!.socketFactory
                if (!config.verifySSLHostname) hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
            doOutput = true // trigger POST method
            config.run {
                connectTimeout = timeout
                readTimeout = timeout
                accept?.let { setRequestProperty("Accept", it) }
                userAgent?.let { setRequestProperty("User-Agent", it) }
                acceptEncoding?.let { setRequestProperty("Accept-Encoding", it) }
                if (user != null && password != null) setRequestProperty("Authorization", authorization())
            }
            setRequestProperty("Content-Type", contentType)
            if (chunked) setChunkedStreamingMode(0)
            writeContent(outputStream)
            return try {
                Response(
                    responseCode,
                    getHeaderField("Server"),
                    getHeaderField("Content-Type"),
                    inputStream
                )
            } catch (throwable: Throwable) {
                throw Exception(
                    "Post '$contentType' to $uri failed with response code $responseCode",
                    throwable,
                    responseCode,
                    responseMessage,
                    headerFields,
                    errorStream
                )
            }
        }
    }
}