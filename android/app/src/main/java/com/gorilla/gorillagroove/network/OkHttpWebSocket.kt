package com.gorilla.gorillagroove.network

import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class OkHttpWebSocket : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)

        logDebug("WebSocket was opened")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)

        logDebug("WebSocket was closed")
//        //Log.d(TAG, "onClosed: $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)

        logError("WebSocket failure", t)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
//        //Log.d(TAG, "onMessage: $text")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
        //Log.d(TAG, "onMessage: ${bytes.hex()}")
    }
}
