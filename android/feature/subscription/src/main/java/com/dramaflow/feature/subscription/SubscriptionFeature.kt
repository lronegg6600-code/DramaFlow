package com.dramaflow.feature.subscription

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dramaflow.core.billing.BillingCatalogProvider
import com.dramaflow.core.billing.GooglePlayBillingClient
import com.dramaflow.core.billing.BillingUiProductMapper
import com.dramaflow.core.billing.PurchaseLauncher
import com.dramaflow.core.billing.RestorePurchaseState
import com.dramaflow.core.common.DataResult
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.common.EntitlementRepository
import com.dramaflow.core.common.SubscriptionRepository
import com.dramaflow.core.designsystem.component.DfBenefitCard
import com.dramaflow.core.designsystem.component.DfPriceCard
import com.dramaflow.core.designsystem.component.DfPrimaryButton
import com.dramaflow.core.designsystem.component.DfWhiteMessageCard
import com.dramaflow.core.designsystem.theme.DramaFlowTheme
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.PurchaseResult
import com.dramaflow.core.model.SubscriptionProduct
import com.dramaflow.core.ui.DfLoadState
import com.dramaflow.core.ui.DfScreenScaffold
import com.dramaflow.core.ui.DfScrollableColumn
import com.dramaflow.core.ui.DfStateLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SubscriptionEntrySource {
    PLAYER,
    DETAIL,
    PROFILE,
    FEED,
}

data class SubscriptionUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val entrySource: SubscriptionEntrySource = SubscriptionEntrySource.DETAIL,
    val heroTitle: String = "",
    val heroSubtitle: String = "",
    val products: List<SubscriptionProduct> = emptyList(),
    val selectedProductId: String? = null,
    val selectedOfferId: String? = null,
    val isPremium: Boolean = false,
    val isProcessing: Boolean = false,
    val restoreState: RestorePurchaseState = RestorePurchaseState.IDLE,
    val message: String? = null,
    val errorMessage: String = "Unable to load membership options.",
)

sealed interface SubscriptionAction {
    data class SelectOffer(val productId: String, val offerId: String) : SubscriptionAction
    data object ContinuePurchase : SubscriptionAction
    data object Retry : SubscriptionAction
    data object RestorePurchases : SubscriptionAction
    data object DismissMessage : SubscriptionAction
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val billingCatalogProvider: BillingCatalogProvider,
    private val purchaseLauncher: PurchaseLauncher,
    private val entitlementRepository: EntitlementRepository,
    private val subscriptionSyncCoordinator: SubscriptionSyncCoordinator,
    savedStateHandle: SavedStateHandle,
) : androidx.lifecycle.ViewModel() {
    private val entrySource = runCatching {
        SubscriptionEntrySource.valueOf((savedStateHandle["source"] ?: "DETAIL").toString().uppercase())
    }.getOrDefault(SubscriptionEntrySource.DETAIL)
    private val _uiState = MutableStateFlow(SubscriptionUiState(entrySource = entrySource))
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        observeSubscription()
        preloadBillingCatalog()
    }

    fun billingLauncher(): PurchaseLauncher = purchaseLauncher

    fun onAction(action: SubscriptionAction, onPurchaseSuccess: (() -> Unit)? = null) {
        when (action) {
            is SubscriptionAction.SelectOffer -> {
                _uiState.update { it.copy(selectedProductId = action.productId, selectedOfferId = action.offerId) }
                viewModelScope.launch { repository.setSelectedOffer(action.productId, action.offerId) }
            }
            SubscriptionAction.ContinuePurchase -> startPurchase(onPurchaseSuccess)
            SubscriptionAction.Retry -> observeSubscription()
            SubscriptionAction.RestorePurchases -> restorePurchases()
            SubscriptionAction.DismissMessage -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun observeSubscription() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeSubscription().collect { result ->
                _uiState.update { current ->
                    when (result) {
                        DataResult.Loading -> current.copy(loadState = DfLoadState.LOADING)
                        DataResult.Empty -> current.copy(loadState = DfLoadState.EMPTY)
                        is DataResult.Error -> current.copy(loadState = DfLoadState.ERROR, errorMessage = result.message)
                        is DataResult.Success -> current.copy(
                            loadState = DfLoadState.SUCCESS,
                            heroTitle = result.value.heroTitle,
                            heroSubtitle = result.value.heroSubtitle,
                            products = if (current.products.isEmpty()) result.value.products else current.products,
                            selectedProductId = result.value.selectedProductId,
                            selectedOfferId = result.value.selectedOfferId,
                            isPremium = result.value.entitlementState.isPremium,
                        )
                    }
                }
            }
        }
    }

    private fun preloadBillingCatalog() {
        viewModelScope.launch {
            val remoteProducts = billingCatalogProvider.loadProducts()
            if (remoteProducts.isNotEmpty()) {
                _uiState.update { it.copy(products = remoteProducts) }
            }
        }
    }

    private fun startPurchase(onPurchaseSuccess: (() -> Unit)?) {
        val state = uiState.value
        val product = state.products.firstOrNull { it.id == state.selectedProductId } ?: return
        val offer = product.offers.firstOrNull { it.id == state.selectedOfferId } ?: product.offers.first()
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, message = null) }
            when (val result = purchaseLauncher.launchPurchase(product.id, offer.id, offer.offerToken)) {
                is PurchaseResult.Success -> {
                    val syncResult = subscriptionSyncCoordinator.syncPurchase(
                        purchase = result.purchase,
                        sourcePage = uiState.value.entrySource.name.lowercase(),
                    )
                    repository.setSelectedOffer(product.id, offer.id)
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            isPremium = syncResult.second.isPremium,
                            message = if (syncResult.first.syncAccepted) {
                                "Purchase synced. Entitlement state: ${syncResult.first.entitlementState}."
                            } else {
                                "Purchase captured but backend sync is pending."
                            },
                        )
                    }
                    if (syncResult.second.isPremium) {
                        onPurchaseSuccess?.invoke()
                    }
                }
                PurchaseResult.Cancelled -> _uiState.update { it.copy(isProcessing = false, message = "Purchase cancelled.") }
                is PurchaseResult.Error -> _uiState.update { it.copy(isProcessing = false, message = result.message) }
            }
        }
    }

    private fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(restoreState = RestorePurchaseState.RESTORING, message = null) }
            val restoreResult = subscriptionSyncCoordinator.restorePurchases(uiState.value.entrySource.name.lowercase())
            _uiState.update {
                it.copy(
                    restoreState = RestorePurchaseState.RESTORED,
                    isPremium = restoreResult.second.isPremium,
                    message = "Restore synced ${restoreResult.first.size} purchase(s).",
                )
            }
        }
    }
}

@Composable
fun SubscriptionRoute(
    onBack: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    DisposableEffect(context, viewModel) {
        val activity = context.findActivity()
        (viewModel.billingLauncher() as? GooglePlayBillingClient)?.attachActivity(activity)
        onDispose {
            (viewModel.billingLauncher() as? GooglePlayBillingClient)?.attachActivity(null)
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SubscriptionScreen(
        uiState = uiState,
        onAction = { action -> viewModel.onAction(action, onPurchaseSuccess) },
        onBack = onBack,
    )
}

@Composable
fun SubscriptionScreen(
    uiState: SubscriptionUiState,
    onAction: (SubscriptionAction) -> Unit,
    onBack: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    DfScreenScaffold {
        DfStateLayout(
            state = uiState.loadState,
            modifier = Modifier.padding(it),
            errorMessage = uiState.errorMessage,
            emptyTitle = "No plans available",
            emptyMessage = "Plans will appear here once pricing is configured.",
            onRetry = { onAction(SubscriptionAction.Retry) },
        ) {
            DfScrollableColumn(modifier = Modifier.padding(it)) {
                Surface(
                    modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable(onClick = onBack),
                    color = colors.surface,
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.padding(12.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DramaFlowThemeTokens.shapes.large)
                        .background(DramaFlowThemeTokens.gradients.premium)
                        .padding(spacing.xxl),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        Text(uiState.heroTitle, style = DramaFlowThemeTokens.typography.headlineMedium, color = colors.textInverse)
                        Text(uiState.heroSubtitle, style = DramaFlowThemeTokens.typography.bodyLarge, color = colors.textInverse)
                        Text("Opened from ${uiState.entrySource.name.lowercase()}", color = colors.textInverse)
                    }
                }

                val mapped = BillingUiProductMapper.map(uiState.products)
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    uiState.products.forEach { product ->
                        product.benefits.forEach { benefit -> DfBenefitCard(benefit = benefit) }
                        product.offers.forEach { offer ->
                            DfPriceCard(
                                offer = offer,
                                selected = uiState.selectedOfferId == offer.id,
                                onClick = { onAction(SubscriptionAction.SelectOffer(product.id, offer.id)) },
                            )
                        }
                    }
                }

                val selectedOffer = uiState.products.flatMap { it.offers }.firstOrNull { it.id == uiState.selectedOfferId }
                DfPrimaryButton(
                    label = when {
                        uiState.isProcessing -> "Processing..."
                        uiState.isPremium -> "Subscription active"
                        else -> "Start subscription${selectedOffer?.let { " · ${it.priceText}" } ?: ""}"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { if (!uiState.isProcessing && !uiState.isPremium) onAction(SubscriptionAction.ContinuePurchase) },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable { onAction(SubscriptionAction.RestorePurchases) },
                        color = colors.surface,
                    ) {
                        Row(modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Text(" Restore purchase", color = colors.textPrimary)
                        }
                    }
                    Text("Terms", color = colors.textSecondary)
                    Text("Privacy", color = colors.textSecondary)
                }

                uiState.message?.let {
                    DfWhiteMessageCard(title = "Purchase status", body = it)
                }

                DfWhiteMessageCard(
                    title = "Billing-ready shape",
                    body = "Mapped ${mapped.size} product groups into a Billing-style shape with selected offer tokens, processing state, restore flow, and purchase outcomes.",
                )
            }
        }
    }
}

@Preview
@Composable
private fun SubscriptionPreview() {
    DramaFlowTheme {
        SubscriptionScreen(
            uiState = SubscriptionUiState(
                loadState = DfLoadState.SUCCESS,
                heroTitle = "Stay inside the scene instead of hitting paywalls",
                heroSubtitle = "Subscription keeps auto-play smooth.",
                products = DramaFlowMockData.subscriptionProducts,
                selectedProductId = "premium_access",
                selectedOfferId = "monthly_premium",
            ),
            onAction = {},
            onBack = {},
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
