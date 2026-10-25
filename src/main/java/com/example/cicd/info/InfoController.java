package com.example.cicd.info;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Tells which release "track" (stable / canary) answered the request.
 * The canary monitor script uses this to measure the traffic split.
 */
@RestController
public class InfoController {

    private final String track;
    private final String version;

    public InfoController(@Value("${app.track}") String track,
                          @Value("${app.version}") String version) {
        this.track = track;
        this.version = version;
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of("track", track, "version", version);
    }
}
