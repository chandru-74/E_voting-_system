package com.example.E_voting_System.stream;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final SseStreamService sseStreamService;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> stream() {
        return sseStreamService.getSink().asFlux()
                .mergeWith(Flux.interval(Duration.ofSeconds(10))
                        .map(i -> Map.of("heartbeat", "alive")));
    }
}