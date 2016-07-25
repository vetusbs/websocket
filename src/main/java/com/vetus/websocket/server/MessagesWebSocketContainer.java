package com.vetus.websocket.server;

import org.springframework.integration.websocket.ServerWebSocketContainer;
import org.springframework.integration.websocket.WebSocketListener;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.websocket.Session;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesWebSocketContainer extends ServerWebSocketContainer {

    Map<String, WebSocketSession> sessionMap = new HashMap<String, WebSocketSession>();

    public MessagesWebSocketContainer(String... paths) {
        super(paths);
        setSupportedProtocols("html");
        setMessageListener(new WebSocketListener() {
            public void onMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                System.out.println(message.getPayload().toString());
                String userId = extractUserTo(message.getPayload().toString());
                if (sessionMap.containsKey(userId) && sessionMap.get(userId).isOpen()) {
                    sessionMap.get(userId).sendMessage(new TextMessage("Message from" + extractUserId(session)));
                }
            }

            public void afterSessionStarted(WebSocketSession session) throws Exception {
                System.out.println("connected");
                String userId = extractUserId(session);
                sessionMap.put(userId, session);
                System.out.println(sessionMap.keySet().toString());
            }

            public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                System.out.println("desconnecting");
            }

            public List<String> getSubProtocols() {
                return Arrays.asList("HTTP");
            }
        });
    }

    public static String extractUserId(WebSocketSession session) {
        String pathInfo = session.getUri().getSchemeSpecificPart();
        String[] pathParts = pathInfo.split("/");
        String part1 = pathParts[2];
        return part1;
    }

    public static String extractUserTo(String message) {
        String[] pathParts = message.split("#");
        String part1 = pathParts[1];
        return part1;
    }

}
