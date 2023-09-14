package de.gmuth.http

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.net.URI
import java.util.logging.Logger.getLogger

open class HttpClientMock(config: Config = Config()) : HttpClient(config) {

    val log = getLogger(javaClass.name)
    lateinit var rawRequest: ByteArray
    var httpStatus: Int = 200
    var httpServer: String? = "HttpClientMock"
    var httpContentType: String? = "application/ipp"
    var httpContentFile: File? = null

    fun mockResponse(file: File) {
        httpContentFile = file
    }

    fun mockResponse(fileName: String, directory: String = "printers") =
        mockResponse(File(directory, fileName))

    override fun post(
        uri: URI,
        contentType: String,
        writeContent: (OutputStream) -> Unit,
        chunked: Boolean
    ) =
        Response(
            httpStatus,
            httpServer,
            httpContentType,
            httpContentFile?.inputStream()
        ).apply {
            rawRequest = ByteArrayOutputStream().run {
                writeContent(this)
                toByteArray()
            }
            log.info { "post ${rawRequest.size} bytes to $uri -> response '$server', $status, ${this.contentType}" }
        }
}