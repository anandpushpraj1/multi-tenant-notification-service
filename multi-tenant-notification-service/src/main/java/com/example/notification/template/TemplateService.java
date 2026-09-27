package com.example.notification.template;

import com.example.notification.channel.ChannelType;
import com.example.notification.common.ApiException;
import com.example.notification.security.SecurityUtils;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TemplateService {
    private final TemplateRepository repo;

    public TemplateService(TemplateRepository repo) {
        this.repo = repo;
    }

    public Template create(String name, ChannelType channel, String subject, String body) {
        UUID t = SecurityUtils.tenant();
        if (t == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Tenant context required");
        return repo.save(new Template(t, name, channel, subject, body));
    }

    public Template get(UUID id) {
        Template t =
                repo.findById(id)
                        .orElseThrow(
                                () -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
        if (!SecurityUtils.current().isPlatformAdmin()
                && !t.getTenantId().equals(SecurityUtils.tenant()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Tenant isolation violation");
        return t;
    }

    public List<Template> list() {
        return SecurityUtils.current().isPlatformAdmin()
                ? repo.findAll()
                : repo.findByTenantIdOrderByCreatedAtDesc(SecurityUtils.tenant());
    }

    public Template update(UUID id, String subject, String body) {
        Template t = get(id);
        t.update(subject, body);
        return repo.save(t);
    }

    public void delete(UUID id) {
        Template t = get(id);
        repo.delete(t);
    }
}
