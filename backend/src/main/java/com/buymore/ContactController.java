package com.buymore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import java.util.Map;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    final JdbcTemplate db;
    ContactController(JdbcTemplate db) {this.db=db;}

    public record ContactSubmission(
        @NotBlank @Size(max=100) String name,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(max=200) String subject,
        @NotBlank @Size(max=4000) String message
    ) {}

    @PostMapping
    @ResponseStatus(CREATED)
    Map<String,Object> submit(@Valid @RequestBody ContactSubmission input) {
        Long id = db.queryForObject(
            "INSERT INTO contact_submissions(name, email, subject, message, created_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP) RETURNING id",
            Long.class, input.name().trim(), input.email().trim().toLowerCase(Locale.ROOT), input.subject().trim(), input.message().trim()
        );
        return Map.of("ok", true, "submissionId", id);
    }
}