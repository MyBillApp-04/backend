package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.TaxType;

import java.util.Set;

/**
 * Authoritative, stateless GST/Tax calculation path for invoice totals.
 *
 * <p>Every layer (invoice service, offline provider/PDF) MUST use the exact same
 * sequence so results never drift. The order is:</p>
 *
 * <pre>
 *   Subtotal - Discount = Taxable Amount
 *   Taxable Amount x Rate = Tax (GST)
 *   Taxable Amount + Tax = Total
 *   Total - Advance Applied = Amount Due (net payable)
 * </pre>
 *
 * <p>Rounding is applied at each published amount using half-up to 2 decimals,
 * matching the application's existing money convention.</p>
 */
public final class TaxCalculator {

    /** Supported GST rates in percent. */
    public static final Set<Double> SUPPORTED_RATES = Set.of(0.0, 5.0, 12.0, 18.0, 28.0);

    private TaxCalculator() {
    }

    /** @return a GSTIN-sanctioned rate if present, otherwise false (kept, but note-only). */
    public static boolean isSupportedRate(double rate) {
        return SUPPORTED_RATES.contains(roundMoney(rate));
    }

    /**
     * @param businessState resolved state for the business, may be blank
     * @param customerState resolved state for the customer, may be blank
     * @return INTRA_STATE when both states are non-blank and equal, otherwise INTER_STATE
     */
    public static TaxType resolveTaxType(String businessState, String customerState) {
        boolean businessKnown = businessState != null && !businessState.isBlank();
        boolean customerKnown = customerState != null && !customerState.isBlank();
        if (businessKnown && customerKnown) {
            return businessState.trim().equalsIgnoreCase(customerState.trim())
                    ? TaxType.INTRA_STATE
                    : TaxType.INTER_STATE;
        }
        // Safe fallback when state data is unavailable: bill IGST (inter-state), which is
        // the conservative default and can be overridden explicitly.
        return TaxType.INTER_STATE;
    }

    /**
     * Produces the tax snapshot for a taxable (post-discount) base.
     *
     * @param taxableBase amount after discount, must be >= 0
     * @param rate        applicable GST percentage (0 = no tax)
     * @param taxType     INTRA/INTER/NONE
     * @return the fully-populated tax breakdown
     */
    public static TaxBreakdown calculate(double taxableBase, double rate, TaxType taxType) {
        double safeBase = Math.max(taxableBase, 0.0);
        double safeRate = roundMoney(rate == 0.0 ? 0.0 : rate);

        if (safeRate == 0.0) {
            return TaxBreakdown.none(safeBase);
        }

        if (taxType != TaxType.INTRA_STATE && taxType != TaxType.INTER_STATE) {
            return TaxBreakdown.none(safeBase);
        }

        double totalTax = roundMoney(safeBase * safeRate / 100.0);
        if (taxType == TaxType.INTRA_STATE) {
            double half = roundMoney(totalTax / 2.0);
            return new TaxBreakdown(
                    safeRate,
                    taxType,
                    roundMoney(safeBase),
                    totalTax,
                    half,
                    half,
                    0.0,
                    roundMoney(safeBase + totalTax));
        }

        return new TaxBreakdown(
                safeRate,
                taxType,
                roundMoney(safeBase),
                totalTax,
                0.0,
                0.0,
                totalTax,
                roundMoney(safeBase + totalTax));
    }

    /**
     * Advances applied are capped to the GST-inclusive total. Returns {@code min(advance, total)}.
     */
    public static double capAdvance(double advance, double total) {
        return roundMoney(Math.min(Math.max(advance, 0.0), Math.max(total, 0.0)));
    }

    /** Amount due = total (GST-inclusive) - advance applied, floored at 0. */
    public static double amountDue(double total, double advance) {
        return roundMoney(Math.max(total - advance, 0.0));
    }

    static double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Immutable tax breakdown for one invoice. */
    public static final class TaxBreakdown {
        public final double rate;
        public final TaxType type;
        public final double taxableAmount;
        public final double taxAmount;
        public final double cgstAmount;
        public final double sgstAmount;
        public final double igstAmount;
        public final double total;

        TaxBreakdown(double rate, TaxType type, double taxableAmount, double taxAmount,
                     double cgstAmount, double sgstAmount, double igstAmount, double total) {
            this.rate = rate;
            this.type = type;
            this.taxableAmount = taxableAmount;
            this.taxAmount = taxAmount;
            this.cgstAmount = cgstAmount;
            this.sgstAmount = sgstAmount;
            this.igstAmount = igstAmount;
            this.total = total;
        }

        private static TaxBreakdown none(double taxable) {
            return new TaxBreakdown(0.0, TaxType.NONE, taxable, 0.0, 0.0, 0.0, 0.0, taxable);
        }
    }
}