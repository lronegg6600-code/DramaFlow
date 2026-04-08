package com.dramaflow.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.dramaflow.core.common.auth.AuthSessionManager
import com.dramaflow.core.model.BillingPurchasePayload
import com.dramaflow.core.model.BillingSubscriptionStatus
import com.dramaflow.core.model.BillingSyncResult
import com.dramaflow.core.model.PurchaseResult
import com.dramaflow.core.model.SubscriptionBenefit
import com.dramaflow.core.model.SubscriptionOffer
import com.dramaflow.core.model.SubscriptionProduct
import com.dramaflow.core.network.source.BillingRemoteDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

data class BillingUiOffer(
    val id: String,
    val title: String,
    val billingPeriodLabel: String,
    val priceText: String,
    val monthlyEquivalentText: String,
    val trialBadge: String?,
    val badgeText: String?,
)

data class BillingUiProduct(
    val id: String,
    val title: String,
    val subtitle: String,
    val benefits: List<String>,
    val offers: List<BillingUiOffer>,
)

data class PurchaseUiState(
    val isProcessing: Boolean = false,
    val selectedProductId: String? = null,
    val selectedOfferId: String? = null,
    val message: String? = null,
)

enum class RestorePurchaseState {
    IDLE,
    RESTORING,
    RESTORED,
    FAILED,
}

interface BillingCatalogProvider {
    suspend fun loadProducts(): List<SubscriptionProduct>
}

interface PurchaseLauncher {
    suspend fun launchPurchase(
        productId: String,
        offerId: String,
        offerToken: String?,
    ): PurchaseResult

    suspend fun restorePurchases(): List<BillingPurchasePayload>
}

enum class FakePurchaseOutcome {
    SUCCESS,
    CANCELLED,
    ERROR,
}

@Singleton
class FakeBillingCatalogProvider @Inject constructor() : BillingCatalogProvider {
    override suspend fun loadProducts(): List<SubscriptionProduct> {
        delay(120)
        return listOf(
            SubscriptionProduct(
                id = "premium_access",
                title = "DramaFlow Premium",
                subtitle = "Unlock premium episodes and keep auto-play running",
                benefits = listOf(
                    SubscriptionBenefit("playback", "Premium playback", "Continue through premium episodes."),
                    SubscriptionBenefit("resume", "Resume sync", "Keep your latest episode and saved position."),
                    SubscriptionBenefit("drops", "Early drops", "Open new episodes sooner."),
                ),
                offers = listOf(
                    SubscriptionOffer(
                        id = "monthly_premium",
                        title = "Monthly",
                        offerToken = "monthly_premium_token",
                        billingPeriodLabel = "Billed every month",
                        priceText = "$12.99",
                        monthlyEquivalentText = "$12.99/mo",
                        trialBadge = "3-day trial",
                        badgeText = "Most Popular",
                        isDefault = true,
                    ),
                    SubscriptionOffer(
                        id = "quarterly_premium",
                        title = "Quarterly",
                        offerToken = "quarterly_premium_token",
                        billingPeriodLabel = "Billed every 3 months",
                        priceText = "$29.99",
                        monthlyEquivalentText = "$9.99/mo",
                        trialBadge = null,
                        badgeText = "Save 23%",
                        isDefault = false,
                    ),
                ),
            ),
        )
    }
}

@Singleton
class FakePurchaseLauncher @Inject constructor() : PurchaseLauncher {
    private var nextOutcome: FakePurchaseOutcome = FakePurchaseOutcome.SUCCESS

    fun setNextOutcome(outcome: FakePurchaseOutcome) {
        nextOutcome = outcome
    }

    override suspend fun launchPurchase(
        productId: String,
        offerId: String,
        offerToken: String?,
    ): PurchaseResult {
        delay(900)
        val outcome = nextOutcome
        nextOutcome = FakePurchaseOutcome.SUCCESS
        return when (outcome) {
            FakePurchaseOutcome.SUCCESS -> PurchaseResult.Success(
                purchase = BillingPurchasePayload(
                    productId = productId,
                    basePlanId = null,
                    offerId = offerId,
                    offerToken = offerToken,
                    purchaseToken = "fake:$productId:$offerId",
                    purchaseState = "purchased",
                    accountId = null,
                    profileId = null,
                ),
            )
            FakePurchaseOutcome.CANCELLED -> PurchaseResult.Cancelled
            FakePurchaseOutcome.ERROR -> PurchaseResult.Error("Purchase could not be completed. Please try again.")
        }
    }

    override suspend fun restorePurchases(): List<BillingPurchasePayload> {
        delay(250)
        return listOf(
            BillingPurchasePayload(
                productId = "premium_access",
                basePlanId = "monthly",
                offerId = "monthly_premium",
                offerToken = "monthly_premium_token",
                purchaseToken = "fake:premium_access:restore",
                purchaseState = "purchased",
                accountId = null,
                profileId = null,
            ),
        )
    }
}

object BillingUiProductMapper {
    fun map(products: List<SubscriptionProduct>): List<BillingUiProduct> {
        return products.map { product ->
            BillingUiProduct(
                id = product.id,
                title = product.title,
                subtitle = product.subtitle,
                benefits = product.benefits.map { it.title },
                offers = product.offers.map { it.toUiOffer() },
            )
        }
    }
}

private fun SubscriptionOffer.toUiOffer(): BillingUiOffer {
    return BillingUiOffer(
        id = id,
        title = title,
        billingPeriodLabel = billingPeriodLabel,
        priceText = priceText,
        monthlyEquivalentText = monthlyEquivalentText,
        trialBadge = trialBadge,
        badgeText = badgeText,
    )
}

@Singleton
class GooglePlayBillingClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : BillingCatalogProvider, PurchaseLauncher {
    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener { _, purchases ->
            purchaseContinuation?.let { continuation ->
                purchaseContinuation = null
                continuation.resume(mapPurchaseResult(purchases.orEmpty()))
            }
        }
        .enablePendingPurchases()
        .build()
    private var connected = false
    private var currentActivity: Activity? = null
    private var cachedProducts: List<ProductDetails> = emptyList()
    private var purchaseContinuation: kotlinx.coroutines.CancellableContinuation<PurchaseResult>? = null

    fun attachActivity(activity: Activity?) {
        currentActivity = activity
    }

    override suspend fun loadProducts(): List<SubscriptionProduct> {
        awaitReady()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("premium_access")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        val result = suspendCancellableCoroutine<Pair<BillingResult, List<ProductDetails>>> { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, details ->
                continuation.resume(billingResult to details)
            }
        }
        cachedProducts = result.second
        return result.second.map { details ->
            val offers = details.subscriptionOfferDetails.orEmpty().mapIndexed { index, offer ->
                SubscriptionOffer(
                    id = offer.offerId ?: "${details.productId}-${offer.basePlanId}-$index",
                    title = offer.offerId ?: offer.basePlanId,
                    offerToken = offer.offerToken,
                    billingPeriodLabel = offer.pricingPhases.pricingPhaseList.firstOrNull()?.billingPeriod ?: offer.basePlanId,
                    priceText = offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice ?: "",
                    monthlyEquivalentText = offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice ?: "",
                    trialBadge = offer.offerTags.firstOrNull { it.contains("trial", ignoreCase = true) },
                    badgeText = offer.offerTags.firstOrNull(),
                    isDefault = index == 0,
                )
            }
            SubscriptionProduct(
                id = details.productId,
                title = details.name,
                subtitle = details.description,
                benefits = listOf(
                    SubscriptionBenefit("premium_playback", "Premium playback", "Unlock premium episodes after backend entitlement sync."),
                    SubscriptionBenefit("restore", "Restore purchases", "Recover purchases through Google Play and backend resync."),
                    SubscriptionBenefit("truth_source", "Server entitlement", "Access is decided by backend entitlement truth, not client optimism."),
                ),
                offers = offers,
            )
        }
    }

    override suspend fun launchPurchase(
        productId: String,
        offerId: String,
        offerToken: String?,
    ): PurchaseResult {
        awaitReady()
        val activity = currentActivity ?: return PurchaseResult.Error("Billing activity is unavailable.")
        val product = cachedProducts.firstOrNull { it.productId == productId } ?: loadProducts().let {
            cachedProducts.firstOrNull { details -> details.productId == productId }
        } ?: return PurchaseResult.Error("Billing product is unavailable.")
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .apply {
                if (!offerToken.isNullOrBlank()) {
                    setOfferToken(offerToken)
                }
            }
            .build()
        return suspendCancellableCoroutine { continuation ->
            purchaseContinuation = continuation
            val response = billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productParams))
                    .build(),
            )
            if (response.responseCode != BillingClient.BillingResponseCode.OK) {
                purchaseContinuation = null
                continuation.resume(PurchaseResult.Error(response.debugMessage.ifBlank { "Billing flow could not start." }))
            }
        }
    }

    override suspend fun restorePurchases(): List<BillingPurchasePayload> {
        awaitReady()
        val result = suspendCancellableCoroutine<Pair<BillingResult, List<Purchase>>> { continuation ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                PurchasesResponseListener { billingResult, purchases ->
                    continuation.resume(billingResult to purchases)
                },
            )
        }
        return result.second.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .flatMap { purchase ->
                purchase.products.map { productId ->
                    BillingPurchasePayload(
                        productId = productId,
                        basePlanId = null,
                        offerId = null,
                        offerToken = null,
                        purchaseToken = purchase.purchaseToken,
                        purchaseState = "purchased",
                        accountId = purchase.accountIdentifiers?.obfuscatedAccountId,
                        profileId = purchase.accountIdentifiers?.obfuscatedProfileId,
                    )
                }
            }
    }

    private suspend fun awaitReady() {
        if (connected) return
        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    connected = false
                }

                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        connected = true
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(IllegalStateException(billingResult.debugMessage))
                    }
                }
            })
        }
    }

    private fun mapPurchaseResult(purchases: List<Purchase>): PurchaseResult {
        val purchase = purchases.firstOrNull() ?: return PurchaseResult.Cancelled
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                return PurchaseResult.Error("Purchase is pending confirmation.")
            }
            return PurchaseResult.Cancelled
        }
        val productId = purchase.products.firstOrNull() ?: return PurchaseResult.Error("Google Play did not return a product id.")
        return PurchaseResult.Success(
            purchase = BillingPurchasePayload(
                productId = productId,
                basePlanId = null,
                offerId = null,
                offerToken = null,
                purchaseToken = purchase.purchaseToken,
                purchaseState = "purchased",
                accountId = purchase.accountIdentifiers?.obfuscatedAccountId,
                profileId = purchase.accountIdentifiers?.obfuscatedProfileId,
            ),
        )
    }
}

interface BillingRepository {
    suspend fun syncPurchase(
        purchase: BillingPurchasePayload,
        source: String,
        sourcePage: String,
    ): BillingSyncResult

    suspend fun restorePurchases(sourcePage: String): List<BillingSyncResult>
    suspend fun getSubscriptionStatus(): BillingSubscriptionStatus?
}

@Singleton
class DefaultBillingRepository @Inject constructor(
    private val authSessionManager: AuthSessionManager,
    private val purchaseLauncher: PurchaseLauncher,
    private val billingRemoteDataSource: BillingRemoteDataSource,
) : BillingRepository {
    override suspend fun syncPurchase(
        purchase: BillingPurchasePayload,
        source: String,
        sourcePage: String,
    ): BillingSyncResult {
        authSessionManager.ensureGuestSession()
        return billingRemoteDataSource.syncPurchase(purchase, source, sourcePage)
    }

    override suspend fun restorePurchases(sourcePage: String): List<BillingSyncResult> {
        authSessionManager.ensureGuestSession()
        return purchaseLauncher.restorePurchases().map { purchase ->
            billingRemoteDataSource.syncPurchase(
                purchase = purchase,
                source = "restore",
                sourcePage = sourcePage,
            )
        }
    }

    override suspend fun getSubscriptionStatus(): BillingSubscriptionStatus? {
        authSessionManager.ensureGuestSession()
        return billingRemoteDataSource.getSubscriptionStatus()
    }
}
