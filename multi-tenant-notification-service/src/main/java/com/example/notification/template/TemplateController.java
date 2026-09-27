package com.example.notification.template;

import com.example.notification.channel.ChannelType;

import jakarta.validation.constraints.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/templates")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class TemplateController {
    private final TemplateService s;

    public TemplateController(TemplateService s) {
        this.s = s;
    }

    public record Create(
            @NotBlank String name,
            @NotNull ChannelType channel,
            String subject,
            @NotBlank String body) {}

    public record Update(String subject, @NotBlank String body) {}

    @PostMapping
    public Template create(@RequestBody @jakarta.validation.Valid Create r) {
        return s.create(r.name(), r.channel(), r.subject(), r.body());
    }

    @GetMapping
    public List<Template> list() {
        return s.list();
    }

    @GetMapping("/{id}")
    public Template get(@PathVariable UUID id) {
        return s.get(id);
    }

    @PutMapping("/{id}")
    public Template update(@PathVariable UUID id, @RequestBody @jakarta.validation.Valid Update r) {
        return s.update(id, r.subject(), r.body());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        s.delete(id);
    }
}
