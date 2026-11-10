package cz.maresdemo.examples;

import java.net.InetAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tells which build is running where: the first thing to look at after a deployment. */
@RestController
public class InfoController {

    private final String name;
    private final String version;
    private final String build;
    private final Environment environment;

    public InfoController(@Value("${spring.application.name}") String name,
                          @Value("${app.version}") String version,
                          @Value("${app.build}") String build,
                          Environment environment) {
        this.name = name;
        this.version = version;
        this.build = build;
        this.environment = environment;
    }

    @GetMapping("/")
    public Map<String, Object> info() throws Exception {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", name);
        info.put("version", version);
        info.put("build", build);
        info.put("environment", String.join(",", environment.getActiveProfiles()));
        info.put("host", InetAddress.getLocalHost().getHostName());
        info.put("java", System.getProperty("java.version"));
        info.put("time", Instant.now().toString());
        return info;
    }
}
