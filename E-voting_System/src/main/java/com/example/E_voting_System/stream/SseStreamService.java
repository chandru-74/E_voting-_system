package com.example.E_voting_System.stream;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Map;

@Service
public class SseStreamService {

    private final Sinks.Many<Map<String, Object>> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publishEvent(Map<String, Object> data) {
        sink.tryEmitNext(data);
    }

    public Sinks.Many<Map<String, Object>> getSink() {
        return sink;
    }
}