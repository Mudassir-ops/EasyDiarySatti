package com.example.easydiarysatti.paywalls

import com.android.billingclient.api.ProductDetails

/**
 * Extracts human-readable pricing strings from a [ProductDetails] object returned by Google Play.
 *
 * Subscription offers can have multiple pricing phases (e.g. free-trial → introductory → base).
 * This helper isolates each phase so Fragments can populate UI labels dynamically.
 */
object PricingHelper {

    // ─────────────────────────────────────────────
    // Base price  (the recurring charge after trial)
    // ─────────────────────────────────────────────

    /**
     * Returns the base recurring price string as formatted by Google Play,
     * e.g. "$11.99", "€9,99", "₹999".
     * Falls back to empty string if unavailable.
     */
    fun formattedPrice(details: ProductDetails): String {
        return basePricingPhase(details)?.formattedPrice ?: ""
    }

    /**
     * Returns the base price in micro-units (price × 1_000_000).
     * Useful for computing per-month breakdowns, e.g. annual / 12.
     */
    fun priceMicros(details: ProductDetails): Long {
        return basePricingPhase(details)?.priceAmountMicros ?: 0L
    }

    /**
     * Returns the ISO 4217 currency code of the base price, e.g. "USD", "EUR".
     */
    fun currencyCode(details: ProductDetails): String {
        return basePricingPhase(details)?.priceCurrencyCode ?: ""
    }

    // ─────────────────────────────────────────────
    // Billing period  (how often the base price recurs)
    // ─────────────────────────────────────────────

    /**
     * Returns a short, human-readable billing cadence string derived from the ISO 8601
     * billing period of the base phase:
     *   P1W  → "per week"
     *   P1M  → "per month"
     *   P3M  → "per 3 months"
     *   P1Y  → "per year"
     *
     * Returns the raw ISO string unchanged if it doesn't match a known pattern.
     */
    fun billingPeriod(details: ProductDetails): String {
        return when (basePricingPhase(details)?.billingPeriod) {
            "P1W"  -> "per week"
            "P2W"  -> "per 2 weeks"
            "P1M"  -> "per month"
            "P3M"  -> "per 3 months"
            "P6M"  -> "per 6 months"
            "P1Y"  -> "per year"
            else   -> basePricingPhase(details)?.billingPeriod ?: ""
        }
    }

    // ─────────────────────────────────────────────
    // Free trial
    // ─────────────────────────────────────────────

    /**
     * Returns the number of free-trial days as a plain integer, or 0 if there is no trial.
     *
     * Reads the first pricing phase whose recurrenceMode is FINITE_RECURRING or NON_RECURRING
     * and whose formattedPrice is "0" / "$0.00" (i.e. the free phase).
     */
    fun freeTrialDays(details: ProductDetails): Int {
        val trialPhase = trialPricingPhase(details) ?: return 0
        return iso8601PeriodToDays(trialPhase.billingPeriod)
    }

    /**
     * Returns a ready-to-display trial label such as "3-day free trial" or
     * an empty string when there is no trial.
     */
    fun freeTrialLabel(details: ProductDetails): String {
        val days = freeTrialDays(details)
        return if (days > 0) "$days-day free trial" else ""
    }

    // ─────────────────────────────────────────────
    // Full price label  (combines trial + price + cadence)
    // ─────────────────────────────────────────────

    /**
     * Builds the full price label shown on tvPrice, e.g.:
     *   "7 days free, then $139.99 per year"
     *   "3 days free, then $12.99 per month"
     *   "$4.99 per week"  (no trial)
     */
    fun fullPriceLabel(details: ProductDetails): String {
        val price  = formattedPrice(details)
        val period = billingPeriod(details)
        val trial  = freeTrialDays(details)
        return if (trial > 0) "$trial days free, then $price $period"
        else "$price $period"
    }

    // ─────────────────────────────────────────────
    // Per-month breakdown  (useful for annual plans)
    // ─────────────────────────────────────────────

    /**
     * For annual subscriptions, returns the effective monthly cost as a formatted string,
     * e.g. "$0.99/month".  Returns empty string for non-annual plans or when price unavailable.
     */
    fun perMonthLabel(details: ProductDetails): String {
        val phase = basePricingPhase(details) ?: return ""
        if (phase.billingPeriod != "P1Y") return ""

        val micros = phase.priceAmountMicros
        val currency = phase.priceCurrencyCode
        val perMonthMicros = micros / 12

        // Format using the system locale and the product's currency
        return try {
            val amount = perMonthMicros / 1_000_000.0
            val currencyInstance = java.util.Currency.getInstance(currency)
            val formatter = java.text.NumberFormat.getCurrencyInstance().apply {
                this.currency = currencyInstance
            }
            "${formatter.format(amount)}/month"
        } catch (e: Exception) {
            ""
        }
    }

    // ─────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────

    /**
     * Returns the first *non-free* pricing phase — this is the base recurring charge.
     * In a typical "free trial → paid" offer the phases are ordered [trial, base].
     */
    private fun basePricingPhase(
        details: ProductDetails
    ): ProductDetails.PricingPhase? {
        return details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull()   // last phase = the ongoing base price
    }

    /**
     * Returns the free-trial phase (price == 0), or null if no trial exists.
     */
    private fun trialPricingPhase(
        details: ProductDetails
    ): ProductDetails.PricingPhase? {
        return details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull { it.priceAmountMicros == 0L }
    }

    /**
     * Converts an ISO 8601 duration to a number of days.
     * Handles common subscription periods: P3D, P7D, P1W, P1M (≈30 days), P1Y (≈365 days).
     */
    private fun iso8601PeriodToDays(period: String): Int {
        return when {
            period.matches(Regex("P(\\d+)D")) ->
                period.removePrefix("P").removeSuffix("D").toIntOrNull() ?: 0
            period.matches(Regex("P(\\d+)W")) ->
                (period.removePrefix("P").removeSuffix("W").toIntOrNull() ?: 0) * 7
            period == "P1M" -> 30
            period == "P1Y" -> 365
            else -> 0
        }
    }
}