package com.vr.vr.engine;

import com.vr.vr.websocket.Message;

import java.util.List;
import java.util.function.Consumer;

/** Routes typed messages to registered handlers and pushes messages to venue controllers. */
public interface MessageRouter {
    void sendToVenue(Long venueId, Object message);
    void onMessage(String type, Consumer<Message> handler);
    boolean isConnected(Long venueId);
    List<Long> getActiveVenues();
}
