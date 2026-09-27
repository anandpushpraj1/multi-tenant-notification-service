package com.example.notification.template;

import static org.junit.jupiter.api.Assertions.*;

import com.example.notification.common.ApiException;

import org.junit.jupiter.api.Test;

import java.util.*;

class TemplateRendererTest {
    private final TemplateRenderer r = new TemplateRenderer();

    @Test
    void renders() {
        assertEquals("Hello Anand", r.render("Hello {{name}}", Map.of("name", "Anand")));
    }

    @Test
    void missingVariableFails() {
        assertThrows(ApiException.class, () -> r.render("Hello {{name}}", Map.of()));
    }
}
