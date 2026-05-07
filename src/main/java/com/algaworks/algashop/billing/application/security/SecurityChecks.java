package com.algaworks.algashop.billing.application.security;

import java.util.UUID;

public interface SecurityChecks {
    UUID getAuthenticatedUserId();
    boolean isAuthenticated();
    boolean isMachineAuthenticated();
}

