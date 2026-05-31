package org.tpkprav.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = SgNricValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SgNric {
    String message() default "Invalid Singapore NRIC/FIN";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}