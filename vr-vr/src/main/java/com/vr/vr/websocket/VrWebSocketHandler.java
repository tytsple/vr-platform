package com.vr.vr.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vr.vr.engine.MessageRouter;
import com.vr.vr.mapper.VenueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class VrWebSocketHandler extends TextWebSocketHandler implements MessageRouter {

    private static final Logger log = LoggerFactory.getLogger(VrWebSocketHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final VenueMapper venueMapper;

    private final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Consumer<Message>>> handlers = new ConcurrentHashMap<>();

    public VrWebSocketHandler(VenueMapper venueMapper) {
        this.venueMapper = venueMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) { session.close(); return; }

        String query = uri.getQuery();
        String token = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "token".equals(kv[0])) { token = kv[1]; break; }
            }
        }

        if (token == null) { session.close(CloseStatus.POLICY_VIOLATION); return; }

        var venue = venueMapper.selectVenueByToken(token);
        if (venue == null) { session.close(CloseStatus.POLICY_VIOLATION); return; }

        session.getAttributes().put("venueId", venue.getId());
        session.getAttributes().put("lastActive", Instant.now());

        WebSocketSession old = sessions.put(venue.getId(), session);
        if (old != null && old.isOpen()) {
            try { old.close(); } catch (IOException e) { /* ignore */ }
        }
        log.info("controller connected: venue={}", venue.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Message msg = mapper.readValue(textMessage.getPayload(), Message.class);
        Long venueId = (Long) session.getAttributes().get("venueId");
        msg.setVenueId(venueId);
        msg.setTimestamp(Instant.now().getEpochSecond());
        session.getAttributes().put("lastActive", Instant.now());

        if ("heartbeat".equals(msg.getType())) {
            sendToSession(session, Map.of("type", "heartbeat_ack"));
        }

        List<Consumer<Message>> hs = handlers.get(msg.getType());
        if (hs != null) {
            for (Consumer<Message> h : hs) h.accept(msg);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long venueId = (Long) session.getAttributes().get("venueId");
        if (venueId != null) {
            sessions.remove(venueId);
            log.info("controller disconnected: venue={}", venueId);
        }
    }

    @Override
    public void sendToVenue(Long venueId, Object message) {
        WebSocketSession session = sessions.get(venueId);
        if (session != null && session.isOpen()) {
            synchronized (session) {
                try {
                    String json = mapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("send failed for venue={}", venueId, e);
                }
            }
        }
    }

    @Override
    public void onMessage(String type, Consumer<Message> handler) {
        handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    @Override
    public boolean isConnected(Long venueId) {
        WebSocketSession s = sessions.get(venueId);
        return s != null && s.isOpen();
    }

    @Override
    public List<Long> getActiveVenues() {
        return new ArrayList<>(sessions.keySet());
    }

    @Scheduled(fixedDelay = 5000)
    public void checkHeartbeats() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.SECONDS);
        sessions.forEach((venueId, session) -> {
            Instant lastActive = (Instant) session.getAttributes().get("lastActive");
            if (lastActive != null && lastActive.isBefore(threshold)) {
                try { session.close(); } catch (IOException e) { /* ignore */ }
                sessions.remove(venueId);
                log.info("heartbeat timeout: venue={}", venueId);
            }
        });
    }

    private void sendToSession(WebSocketSession session, Object msg) throws IOException {
        String json = mapper.writeValueAsString(msg);
        session.sendMessage(new TextMessage(json));
    }
}
