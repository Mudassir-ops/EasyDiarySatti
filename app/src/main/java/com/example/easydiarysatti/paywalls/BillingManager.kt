package com.example.easydiarysatti.paywalls

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.*

class BillingManager(
    private val activity: Activity,
    private val onPurchaseSuccess: (String) -> Unit
) {

    // billingClient is var so it can be rebuilt after endConnection() destroys it.
    // A closed BillingClient can NEVER reconnect — must create a new instance.
    private var billingClient: BillingClient = buildClient()

    // Handler for connection timeout — cancelled once the client connects or fails.
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

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
     *
     * [onConnected]  — fires on the UI thread once the connection is ready.
     * [onFailed]     — fires on the UI thread when the connection fails or times out.
     * [timeoutMs]    — ms before [onFailed] is triggered if no response (default 8 s).
     *
     * Covers three failure modes:
     *  1. Billing service returns a non-OK response code  → onFailed immediately
     *  2. No Google Play Services / no network at all     → onFailed after timeout
     *  3. Very slow network                               → onFailed after timeout
     */
    fun startConnection(
        onConnected: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null,
        timeoutMs: Long = 8_000L
    ) {
        if (billingClient.isReady) {
            activity.runOnUiThread { onConnected?.invoke() }
            return
        }
        // Rebuild before connecting — a closed BillingClient can NEVER reconnect
        billingClient = buildClient()

        // Cancel any previous pending timeout before arming a new one
        cancelTimeout()
        timeoutRunnable = Runnable {
            if (!billingClient.isReady) {
                Log.w("BillingManager", "startConnection: timed out after ${timeoutMs}ms")
                activity.runOnUiThread { onFailed?.invoke() }
            }
        }.also { mainHandler.postDelayed(it, timeoutMs) }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                cancelTimeout()
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing Client Connected")
                    activity.runOnUiThread { onConnected?.invoke() }
                } else {
                    Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
                    activity.runOnUiThread { onFailed?.invoke() }
                }
            }

            override fun onBillingServiceDisconnected() {
                // This fires after a mid-session disconnection, not just on initial setup,
                // so we intentionally do NOT call onFailed here — that would produce
                // spurious error dialogs after a purchase completes or during backgrounding.
                Log.e("BillingManager", "Billing Client Disconnected")
                cancelTimeout()
            }
        })
    }

    /** Cancel any pending connection-timeout runnable. */
    private fun cancelTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    /**
     * Ensures billing is connected, then runs [block] on the billing thread.
     * If already connected, runs [block] immediately.
     * If disconnected, starts a new connection and runs [block] once connected.
     */
    private fun ensureConnected(block: () -> Unit) {
        if (billingClient.isReady) {
            block()
            return
        }
        Log.d("BillingManager", "ensureConnected: not ready, rebuilding and reconnecting...")
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
     * Automatically reconnects if billing service is disconnected.
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
     * Fetches the formatted price for a one-time IN_APP product.
     * Automatically reconnects if billing service is disconnected.
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
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) {
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
        cancelTimeout()
        if (billingClient.isReady) billingClient.endConnection()
    }
}