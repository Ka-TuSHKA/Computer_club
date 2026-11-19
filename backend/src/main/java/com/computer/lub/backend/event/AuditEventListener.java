package com.computer.lub.backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    @EventListener
    public void onAudit(AuditEvent e) {
        AUDIT.info("{} | user={} | {}", e.getAction(), e.getUsername(), e.getDetails());
    }
}
