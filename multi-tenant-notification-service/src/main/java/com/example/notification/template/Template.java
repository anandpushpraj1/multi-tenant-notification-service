package com.example.notification.template;

import com.example.notification.channel.ChannelType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="templates", uniqueConstraints=@UniqueConstraint(name="uk_template_tenant_name_channel", columnNames={"tenant_id","name","channel"}))
public class Template {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;

    @Column(name="tenant_id", nullable=false) private UUID tenantId;

    @Column(nullable=false, length=100) private String name;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private ChannelType channel;

    @Column(length=200) private String subject;

    @Column(nullable=false, columnDefinition="TEXT") private String body;

    @Column(nullable=false) private int version=1;

    @Column(nullable=false) private boolean active=true;

    @Column(nullable=false) private Instant createdAt;

    @Column(nullable=false) private Instant updatedAt;

    protected Template() {

    }

    public Template(UUID tenantId,
                    String name,
                    ChannelType channel,
                    String subject,
                    String body) {
        this.tenantId=tenantId;
        this.name=name;
        this.channel=channel;
        this.subject=subject;
        this.body=body;
    }

    @PrePersist
    void p() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void u() {
        updatedAt = Instant.now();
        version++;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId(){
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public int getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    public void update(String subject,String body) {
        this.subject=subject;
        this.body=body;
    }
}
