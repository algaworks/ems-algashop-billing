package com.algaworks.algashop.billing.infrastructure.kafka;

import com.algaworks.algashop.billing.application.invoice.command.CancelInvoiceIntegrationCommand;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class KafkaBeanValidationConfigurerTest {

    private LocalValidatorFactoryBean validator;
    private KafkaListenerEndpointRegistrar registrar;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        registrar = new KafkaListenerEndpointRegistrar();
        new KafkaBeanValidationConfigurer(validator).configureKafkaListeners(registrar);
    }

    @Test
    void givenConfigurer_whenConfigureKafkaListeners_shouldRegisterTheValidator() {
        Assertions.assertThat(registrar.getValidator()).isSameAs(validator);
    }

    @Test
    void givenInvalidPayload_whenValidateWithRegisteredValidator_shouldReportViolation() {
        CancelInvoiceIntegrationCommand command = new CancelInvoiceIntegrationCommand("");
        Errors errors = new BeanPropertyBindingResult(command, "command");

        registrar.getValidator().validate(command, errors);

        Assertions.assertThat(errors.hasFieldErrors("orderId")).isTrue();
    }

    @Test
    void givenValidPayload_whenValidateWithRegisteredValidator_shouldNotReportViolation() {
        CancelInvoiceIntegrationCommand command = new CancelInvoiceIntegrationCommand("01K1Z5T7Q4XK1B9Q2H3J4K5M6N");
        Errors errors = new BeanPropertyBindingResult(command, "command");

        registrar.getValidator().validate(command, errors);

        Assertions.assertThat(errors.hasErrors()).isFalse();
    }
}
