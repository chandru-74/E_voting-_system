package com.example.E_voting_System.stream;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final SseStreamService sseStreamService;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseStreamService.subscribe();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status",      "UP",
                "subscribers", sseStreamService.subscriberCount()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Integer>> status() {
        return ResponseEntity.ok(Map.of("subscribers", sseStreamService.subscriberCount()));
    }

    @PostMapping("/publish")
    public ResponseEntity<Map<String, String>> publish(@RequestBody Map<String, String> body) {
        sseStreamService.broadcast(
                body.getOrDefault("event", "update"),
                body.getOrDefault("data",  "{}")
        );
        return ResponseEntity.ok(Map.of("status", "broadcast sent"));
    }
}