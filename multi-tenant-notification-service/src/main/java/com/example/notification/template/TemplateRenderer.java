package com.example.notification.template;

import com.example.notification.common.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.*;

@Component
public class TemplateRenderer {
    private static final Pattern P = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}\\}");

    public String render(String text, Map<String, Object> vars) {
        if (text == null) return null;

        Matcher m = P.matcher(text);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            String k = m.group(1);
            if (!vars.containsKey(k))
                throw new ApiException(HttpStatus.BAD_REQUEST, "Missing template variable: " + k);
            m.appendReplacement(b, Matcher.quoteReplacement(String.valueOf(vars.get(k))));
        }
        m.appendTail(b);
        return b.toString();
    }
}
