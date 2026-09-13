package com.example.E_voting_System.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseStreamService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError(e       -> emitters.remove(emitter));
        log.info("New SSE subscriber — total: {}", emitters.size());
        return emitter;
    }

    public void broadcast(String eventName, String jsonPayload) {
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(jsonPayload));
            } catch (IOException e) {
                dead.add(emitter);
                log.debug("SSE emitter disconnected, removing");
            }
        }
        emitters.removeAll(dead);
    }

    public int subscriberCount() { return emitters.size(); }
}