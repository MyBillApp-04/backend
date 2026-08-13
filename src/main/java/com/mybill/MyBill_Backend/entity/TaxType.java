package com.mybill.MyBill_Backend.entity;

/**
 * GST transaction type for an invoice.
 *
 * <p>INTRA_STATE means the business and customer are in the same state, so tax splits
 * into CGST + SGST. INTER_STATE means different states (or indeterminate), so a single
 * IGST applies. NONE means no tax is charged on the invoice.</p>
 */
public enum TaxType {
    NONE,
    INTRA_STATE,
    INTER_STATE
}