package com.mybill.MyBill_Backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TwoDecimalPlacesValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TwoDecimalPlaces {
    String message() default "Value can have at most two decimal places";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
