package cz.maresdemo.examples;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A table the migrations create, so the database is visibly part of the deployment. */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final JdbcClient jdbc;

    public NoteController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return jdbc.sql("SELECT id, text, created_at FROM notes ORDER BY id").query().listOfRows();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "").trim();
        if (text.isEmpty() || text.length() > 500) {
            throw new IllegalArgumentException("text must be 1 to 500 characters");
        }
        return jdbc.sql("INSERT INTO notes (text) VALUES (:text) RETURNING id, text, created_at")
                .param("text", text)
                .query().singleRow();
    }
}
