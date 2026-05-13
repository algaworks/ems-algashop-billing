package com.algaworks.algashop.billing.infrastructure.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class SecurityAnnotations {

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_invoices:write') and @securityChecks.isMachineAuthenticated()")
    public @interface CanGenerateInvoices {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_invoices:read') and not hasRole('CUSTOMER')")
    public @interface CanReadInvoices {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_invoices:read') and hasRole('CUSTOMER')")
    public @interface CanReadMyInvoices {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_credit-cards:read') and hasRole('CUSTOMER')")
    public @interface CanReadMyCreditCards {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_credit-cards:write') and hasRole('CUSTOMER')")
    public @interface CanWriteMyCreditCards {}

}

