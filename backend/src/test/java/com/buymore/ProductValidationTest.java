package com.buymore;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductValidationTest {
    @Test void checkoutRejectsInvalidQuantitiesAndEmptyCarts() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            assertFalse(validator.validate(new OrderService.Checkout(List.of())).isEmpty());
            assertFalse(validator.validate(new OrderService.Checkout(List.of(new OrderService.Item("product",0)))).isEmpty());
            assertFalse(validator.validate(new OrderService.Checkout(List.of(new OrderService.Item("product",101)))).isEmpty());
            assertTrue(validator.validate(new OrderService.Checkout(List.of(new OrderService.Item("product",1)))).isEmpty());
        }
    }
    @Test void registrationRequiresKnownRoleAndStrongEnoughPassword() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            assertFalse(validator.validate(new AuthController.Registration("Tester","a@example.test","short","CUSTOMER",null)).isEmpty());
            assertFalse(validator.validate(new AuthController.Registration("Tester","a@example.test","valid-password","SUPERUSER",null)).isEmpty());
            assertFalse(validator.validate(new AuthController.Registration("Tester","invalid","valid-password","CUSTOMER",null)).isEmpty());
        }
    }
}
