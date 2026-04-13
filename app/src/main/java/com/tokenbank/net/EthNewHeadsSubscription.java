package com.tokenbank.net;

import com.tokenbank.BuildConfig;
import com.tokenbank.utils.TLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Subscribes to Ethereum new block headers (newHeads) via the Alchemy WebSocket API.
 *
 * Usage:
 *   EthNewHeadsSubscription sub = new EthNewHeadsSubscription(listener);
 *   sub.connect();
 *   // ...
 *   sub.disconnect();
 */
public class EthNewHeadsSubscription {

    private static final String TAG = EthNewHeadsSubscription.class.getSimpleName();

    private static final String WSS_URL =
            "wss://eth-mainnet.ws.g.alchemy.com/v2/" + BuildConfig.ALCHEMY_API_KEY;

    private static final String SUBSCRIBE_MESSAGE =
            "{\"id\": 1, \"method\": \"eth_subscribe\", \"params\": [\"newHeads\"]}";

    /** Callback interface for new-head events and connection state changes. */
    public interface Listener {
        void onNewHead(String message);
        void onError(Throwable t);
        void onClosed(int code, String reason);
    }

    private final OkHttpClient client;
    private final Listener listener;
    private WebSocket webSocket;

    public EthNewHeadsSubscription(Listener listener) {
        this.client = new OkHttpClient();
        this.listener = listener;
    }

    /** Opens the WebSocket connection and sends the newHeads subscription. */
    public void connect() {
        Request request = new Request.Builder().url(WSS_URL).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket ws, @NotNull Response response) {
                TLog.d(TAG, "WebSocket opened, subscribing to newHeads");
                ws.send(SUBSCRIBE_MESSAGE);
            }

            @Override
            public void onMessage(@NotNull WebSocket ws, @NotNull String text) {
                TLog.d(TAG, "Received: " + text);
                if (listener != null) {
                    listener.onNewHead(text);
                }
            }

            @Override
            public void onFailure(@NotNull WebSocket ws, @NotNull Throwable t,
                                  @Nullable Response response) {
                TLog.e(TAG, "WebSocket failure: " + t.getMessage());
                if (listener != null) {
                    listener.onError(t);
                }
            }

            @Override
            public void onClosing(@NotNull WebSocket ws, int code, @NotNull String reason) {
                ws.close(code, reason);
            }

            @Override
            public void onClosed(@NotNull WebSocket ws, int code, @NotNull String reason) {
                TLog.d(TAG, "WebSocket closed: " + reason);
                if (listener != null) {
                    listener.onClosed(code, reason);
                }
            }
        });
    }

    /** Closes the WebSocket connection gracefully. */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnecting");
            webSocket = null;
        }
    }
}
