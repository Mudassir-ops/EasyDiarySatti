package com.example.easydiarysatti.paywalls

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*

class BillingManager(
    private val activity: Activity,
    private val onPurchaseSuccess: (String) -> Unit
) {

    // billingClient is var so it can be rebuilt after endConnection() destroys it.
    // A closed BillingClient can NEVER reconnect — must create a new instance.
    private var billingClient: BillingClient = buildClient()

    private fun buildClient(): BillingClient =
        BillingClient.newBuilder(activity)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) { handlePurchase(purchase) }
                }
            }
            .enablePendingPurchases()
            .build()

    /** True only when the billing connection is fully established and ready. */
    val isReady: Boolean get() = billingClient.isReady

    /**
     * Establish connection to Google Play Store.
     * [onConnected] fires on the UI thread once the connection is ready.
     */
    fun startConnection(onConnected: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            activity.runOnUiThread { onConnected?.invoke() }
            return
        }
        // Rebuild before connecting — a closed BillingClient can never reconnect
        billingClient = buildClient()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing Client Connected")
                    activity.runOnUiThread { onConnected?.invoke() }
                } else {
                    Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.e("BillingManager", "Billing Client Disconnected")
            }
        })
    }

    /**
     * Ensures billing is connected, then runs [block] on the billing thread.
     * If already connected, runs [block] immediately.
     * If disconnected, starts a new connection and runs [block] once connected.
     * This is the core guard used by fetchInAppPrice and launchInAppBillingFlow.
     */
    private fun ensureConnected(block: () -> Unit) {
        if (billingClient.isReady) {
            block()
            return
        }
        Log.d("BillingManager", "ensureConnected: not ready, rebuilding and reconnecting...")
        // CRITICAL: rebuild the client — a closed BillingClient can NEVER reconnect.
        // This is what caused: "Client was already closed and can't be reused."
        billingClient = buildClient()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "ensureConnected: reconnected, running block")
                    block()
                } else {
                    Log.e("BillingManager", "ensureConnected: reconnect failed: ${billingResult.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.e("BillingManager", "ensureConnected: onBillingServiceDisconnected")
            }
        })
    }

    /**
     * Fetches live product details (price, free-trial period, billing cadence) for
     * the given [productIds] from Google Play.
     *
     * The result is delivered on the **UI thread** as a Map<productId, ProductDetails>.
     * Use [PricingHelper] to extract human-readable strings from each ProductDetails.
     *
     * Example usage in a Fragment:
     * ```
     * billingManager.fetchProductDetails(
     *     listOf(PaywallCatalog.SPLASH_ANNUAL, PaywallCatalog.SPLASH_MONTHLY)
     * ) { map ->
     *     map[PaywallCatalog.SPLASH_ANNUAL]?.let { details ->
     *         tvPrice.text       = PricingHelper.formattedPrice(details)
     *         tvTrialDays.text   = PricingHelper.freeTrialDays(details)
     *         tvBillingPeriod.text = PricingHelper.billingPeriod(details)
     *     }
     * }
     * ```
     */
    fun fetchProductDetails(
        productIds: List<String>,
        onResult: (Map<String, ProductDetails>) -> Unit
    ) {
        val productList = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                activity.runOnUiThread { onResult(map) }
            } else {
                Log.e("BillingManager", "fetchProductDetails failed: ${billingResult.debugMessage}")
                activity.runOnUiThread { onResult(emptyMap()) }
            }
        }
    }

    /**
     * Launches the Google Play Purchase Flow for a SUBSCRIPTION product.
     * Use for: annual, monthly, weekly subscription plans.
     */
    fun launchBillingFlow(productId: String) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetails = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    ).build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                Log.e("BillingManager", "launchBillingFlow query failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Launches the Google Play Purchase Flow for a one-time IN_APP product.
     * Use for: lifetime_inter_close_in_app_purchase (Remove Ads — $9.99 Lifetime).
     *
     * IN_APP products have no subscriptionOfferDetails / offerToken — do NOT use
     * launchBillingFlow() for them as that will silently fail.
     *
     * Usage:
     *   billingManager.launchInAppBillingFlow("lifetime_inter_close_in_app_purchase")
     */
    /**
     * Launches the Google Play Purchase Flow for a one-time IN_APP product.
     * Automatically reconnects if billing service is disconnected.
     * Use for: lifetime_inter_close_in_app_purchase (Remove Ads — $9.99 Lifetime).
     */
    fun launchInAppBillingFlow(productId: String) {
        ensureConnected {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(productList).build()
            ) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetails = productDetailsList.firstOrNull() ?: run {
                        Log.e("BillingManager", "launchInAppBillingFlow: no product details for $productId")
                        return@queryProductDetailsAsync
                    }
                    // IN_APP — no offerToken needed
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .build()
                            )
                        ).build()
                    activity.runOnUiThread {
                        billingClient.launchBillingFlow(activity, billingFlowParams)
                    }
                } else {
                    Log.e("BillingManager", "launchInAppBillingFlow query failed: ${billingResult.debugMessage}")
                }
            }
        }
    }

    /**
     * Fetches the formatted price for a one-time IN_APP product from Google Play.
     * Returns price string (e.g. "$9.99") via [onResult], or "" on failure.
     *
     * Usage:
     *   billingManager.fetchInAppPrice("lifetime_inter_close_in_app_purchase") { price ->
     *       showRemoveAdsDialog(price = "$price - Lifetime")
     *   }
     */
    /**
     * Fetches the formatted price for a one-time IN_APP product.
     * Automatically reconnects if billing service is disconnected.
     * Returns price string (e.g. "$9.99") via [onResult], or "" on failure.
     */
    fun fetchInAppPrice(productId: String, onResult: (String) -> Unit) {
        ensureConnected {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(productList).build()
            ) { billingResult, productDetailsList ->
                val price = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    productDetailsList.firstOrNull()
                        ?.oneTimePurchaseOfferDetails
                        ?.formattedPrice ?: ""
                } else {
                    Log.e("BillingManager", "fetchInAppPrice failed: ${billingResult.debugMessage}")
                    ""
                }
                activity.runOnUiThread { onResult(price) }
            }
        }
    }

    /** Acknowledges the purchase (both SUBS and IN_APP). */
    private fun handlePurchase(purchase: Purchase) {
        // Guard: only process completed, unacknowledged purchases
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) {
            // Already acknowledged (e.g. restored) — still fire success so UI updates
            val purchasedId = purchase.products.firstOrNull() ?: return
            onPurchaseSuccess(purchasedId)
            return
        }
        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        ) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchasedId = purchase.products.firstOrNull() ?: ""
                onPurchaseSuccess(purchasedId)
            }
        }
    }

    /** Release resources. Call in Fragment's onDestroyView(). */
    fun endConnection() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}