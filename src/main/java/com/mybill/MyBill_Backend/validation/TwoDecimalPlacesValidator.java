package com.mybill.MyBill_Backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class TwoDecimalPlacesValidator implements ConstraintValidator<TwoDecimalPlaces, Double> {

    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return true;
        }

        return BigDecimal.valueOf(value).stripTrailingZeros().scale() <= 2;
    }
}
