package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.TaxType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaxCalculatorTest {

    @Test
    void noTaxMatchesLegacyTotalsExactly() {
        TaxCalculator.TaxBreakdown t = TaxCalculator.calculate(1000.00, 0.0, TaxType.NONE);
        assertThat(t.rate).isEqualTo(0.0);
        assertThat(t.type).isEqualTo(TaxType.NONE);
        assertThat(t.taxableAmount).isEqualTo(1000.00);
        assertThat(t.taxAmount).isEqualTo(0.0);
        assertThat(t.cgstAmount).isEqualTo(0.0);
        assertThat(t.sgstAmount).isEqualTo(0.0);
        assertThat(t.igstAmount).isEqualTo(0.0);
        assertThat(t.total).isEqualTo(1000.00);
    }

    @Test
    void intraState18SplitsCgstSgstEvenly() {
        // taxable 1000 at 18% intra-state -> tax 180 -> CGST 90, SGST 90, total 1180
        TaxCalculator.TaxBreakdown t = TaxCalculator.calculate(1000.00, 18.0, TaxType.INTRA_STATE);
        assertThat(t.taxAmount).isEqualTo(180.00);
        assertThat(t.cgstAmount).isEqualTo(90.00);
        assertThat(t.sgstAmount).isEqualTo(90.00);
        assertThat(t.igstAmount).isEqualTo(0.0);
        assertThat(t.total).isEqualTo(1180.00);
    }

    @Test
    void interState12SingleIgst() {
        // taxable 500 at 12% inter-state -> IGST 60, total 560
        TaxCalculator.TaxBreakdown t = TaxCalculator.calculate(500.00, 12.0, TaxType.INTER_STATE);
        assertThat(t.taxAmount).isEqualTo(60.00);
        assertThat(t.cgstAmount).isEqualTo(0.0);
        assertThat(t.sgstAmount).isEqualTo(0.0);
        assertThat(t.igstAmount).isEqualTo(60.00);
        assertThat(t.total).isEqualTo(560.00);
    }

    @Test
    void allSupportedRatesRoundCorrectly() {
        assertThat(TaxCalculator.calculate(200.00, 5.0, TaxType.INTRA_STATE).total).isEqualTo(210.00);
        assertThat(TaxCalculator.calculate(200.00, 12.0, TaxType.INTRA_STATE).total).isEqualTo(224.00);
        assertThat(TaxCalculator.calculate(200.00, 18.0, TaxType.INTRA_STATE).total).isEqualTo(236.00);
        assertThat(TaxCalculator.calculate(200.00, 28.0, TaxType.INTRA_STATE).total).isEqualTo(256.00);
    }

    @Test
    void decimalAmountsRoundToTwoDecimals() {
        // 333.33 at 18% intra -> tax 59.9994 -> rounds to 60.00, split 30.00/30.00, total 393.33
        TaxCalculator.TaxBreakdown t = TaxCalculator.calculate(333.33, 18.0, TaxType.INTRA_STATE);
        assertThat(t.taxAmount).isEqualTo(60.00);
        assertThat(t.cgstAmount).isEqualTo(30.00);
        assertThat(t.sgstAmount).isEqualTo(30.00);
        assertThat(t.total).isEqualTo(393.33);
    }

    @Test
    void halfCentRoundingIsHalfUp() {
        // 50 at 18% intra -> tax 9.00, half 4.50 -> CGST 4.50, SGST 4.50
        TaxCalculator.TaxBreakdown t = TaxCalculator.calculate(50.00, 18.0, TaxType.INTRA_STATE);
        assertThat(t.cgstAmount).isEqualTo(4.50);
        assertThat(t.sgstAmount).isEqualTo(4.50);
        assertThat(t.total).isEqualTo(59.00);
    }

    @Test
    void resolvesIntraWhenStatesEqual() {
        assertThat(TaxCalculator.resolveTaxType("Maharashtra", "maharashtra")).isEqualTo(TaxType.INTRA_STATE);
    }

    @Test
    void resolvesInterWhenStatesDiffer() {
        assertThat(TaxCalculator.resolveTaxType("Maharashtra", "Gujarat")).isEqualTo(TaxType.INTER_STATE);
    }

    @Test
    void defaultsInterWhenStateMissing() {
        assertThat(TaxCalculator.resolveTaxType(null, "Gujarat")).isEqualTo(TaxType.INTER_STATE);
        assertThat(TaxCalculator.resolveTaxType("Maharashtra", null)).isEqualTo(TaxType.INTER_STATE);
        assertThat(TaxCalculator.resolveTaxType(null, null)).isEqualTo(TaxType.INTER_STATE);
        assertThat(TaxCalculator.resolveTaxType(" ", "")).isEqualTo(TaxType.INTER_STATE);
    }

    @Test
    void advanceAndAmountDue() {
        assertThat(TaxCalculator.capAdvance(500.00, 1180.00)).isEqualTo(500.00);
        assertThat(TaxCalculator.capAdvance(5000.00, 1180.00)).isEqualTo(1180.00);
        assertThat(TaxCalculator.capAdvance(-10.00, 1180.00)).isEqualTo(0.0);
        assertThat(TaxCalculator.amountDue(1180.00, 500.00)).isEqualTo(680.00);
        assertThat(TaxCalculator.amountDue(1180.00, 1180.00)).isEqualTo(0.0);
        assertThat(TaxCalculator.amountDue(1000.00, 1500.00)).isEqualTo(0.0);
    }

    @Test
    void supportedRatesReflectRequirement() {
        assertThat(TaxCalculator.isSupportedRate(0.0)).isTrue();
        assertThat(TaxCalculator.isSupportedRate(5.0)).isTrue();
        assertThat(TaxCalculator.isSupportedRate(12.0)).isTrue();
        assertThat(TaxCalculator.isSupportedRate(18.0)).isTrue();
        assertThat(TaxCalculator.isSupportedRate(28.0)).isTrue();
        assertThat(TaxCalculator.isSupportedRate(7.0)).isFalse();
    }
}