package com.dramaflow.app

import android.content.Context
import com.dramaflow.core.billing.BillingCatalogProvider
import com.dramaflow.core.billing.BillingRepository
import com.dramaflow.core.billing.DefaultBillingRepository
import com.dramaflow.core.billing.FakeBillingCatalogProvider
import com.dramaflow.core.billing.FakePurchaseLauncher
import com.dramaflow.core.billing.GooglePlayBillingClient
import com.dramaflow.core.billing.PurchaseLauncher
import com.dramaflow.core.common.auth.AuthSessionManager
import com.dramaflow.core.common.auth.DefaultAuthSessionManager
import com.dramaflow.core.common.auth.PreferenceTokenStore
import com.dramaflow.core.common.auth.TokenStore
import com.dramaflow.core.common.CatalogRepository
import com.dramaflow.core.common.DefaultMockBehaviorProvider
import com.dramaflow.core.common.DefaultDramaInteractionRepository
import com.dramaflow.core.common.DramaInteractionRepository
import com.dramaflow.core.common.EntitlementRepository
import com.dramaflow.core.common.FakeEntitlementRepository
import com.dramaflow.core.common.FakeSubscriptionRepository
import com.dramaflow.core.common.FeedRepository
import com.dramaflow.core.common.HybridPlaybackRepository
import com.dramaflow.core.common.HybridProgressRepository
import com.dramaflow.core.common.MockBehaviorProvider
import com.dramaflow.core.common.PlaybackRepository
import com.dramaflow.core.common.ProfileRepository
import com.dramaflow.core.common.ProgressRepository
import com.dramaflow.core.common.RemoteCatalogRepository
import com.dramaflow.core.common.RemoteEntitlementRepository
import com.dramaflow.core.common.RemoteFeedRepository
import com.dramaflow.core.common.RemoteProfileRepository
import com.dramaflow.core.common.RemoteSubscriptionRepository
import com.dramaflow.core.common.SubscriptionRepository
import com.dramaflow.core.database.DramaFlowDatabase
import com.dramaflow.core.database.DramaFlowPreferenceStore
import com.dramaflow.core.database.createDatabase
import com.dramaflow.core.network.source.AuthRemoteDataSource
import com.dramaflow.core.network.source.BillingRemoteDataSource
import com.dramaflow.core.network.source.ContentRemoteDataSource
import com.dramaflow.core.network.source.EntitlementRemoteDataSource
import com.dramaflow.core.network.source.FeedRemoteDataSource
import com.dramaflow.core.network.source.PlaybackRemoteDataSource
import com.dramaflow.core.network.source.ProgressRemoteDataSource
import com.dramaflow.core.network.source.RetrofitAuthRemoteDataSource
import com.dramaflow.core.network.source.RetrofitBillingRemoteDataSource
import com.dramaflow.core.network.source.RetrofitContentRemoteDataSource
import com.dramaflow.core.network.source.RetrofitEntitlementRemoteDataSource
import com.dramaflow.core.network.source.RetrofitFeedRemoteDataSource
import com.dramaflow.core.network.source.RetrofitPlaybackRemoteDataSource
import com.dramaflow.core.network.source.RetrofitProgressRemoteDataSource
import com.dramaflow.core.player.DefaultMedia3PlayerBridge
import com.dramaflow.core.player.Media3PlayerBridge
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds
    @Singleton
    abstract fun bindMockBehaviorProvider(impl: DefaultMockBehaviorProvider): MockBehaviorProvider

    @Binds
    @Singleton
    abstract fun bindFeedRepository(impl: RemoteFeedRepository): FeedRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: RemoteCatalogRepository): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackRepository(impl: HybridPlaybackRepository): PlaybackRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: HybridProgressRepository): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: RemoteProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: PreferenceTokenStore): TokenStore

    @Binds
    @Singleton
    abstract fun bindAuthSessionManager(impl: DefaultAuthSessionManager): AuthSessionManager

    @Binds
    @Singleton
    abstract fun bindDramaInteractionRepository(impl: DefaultDramaInteractionRepository): DramaInteractionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): DramaFlowDatabase = createDatabase(context)

    @Provides
    @Singleton
    fun providePreferenceStore(
        @ApplicationContext context: Context,
    ): DramaFlowPreferenceStore = DramaFlowPreferenceStore(context)

    @Provides
    @Singleton
    fun provideMedia3PlayerBridge(
        @ApplicationContext context: Context,
    ): Media3PlayerBridge = DefaultMedia3PlayerBridge(context)

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(): AuthRemoteDataSource = RetrofitAuthRemoteDataSource()

    @Provides
    @Singleton
    fun providePlaybackRemoteDataSource(
        tokenStore: TokenStore,
    ): PlaybackRemoteDataSource = RetrofitPlaybackRemoteDataSource(accessTokenProvider = tokenStore)

    @Provides
    @Singleton
    fun provideFeedRemoteDataSource(): FeedRemoteDataSource = RetrofitFeedRemoteDataSource()

    @Provides
    @Singleton
    fun provideContentRemoteDataSource(): ContentRemoteDataSource = RetrofitContentRemoteDataSource()

    @Provides
    @Singleton
    fun provideProgressRemoteDataSource(
        tokenStore: TokenStore,
    ): ProgressRemoteDataSource = RetrofitProgressRemoteDataSource(accessTokenProvider = tokenStore)

    @Provides
    @Singleton
    fun provideBillingRemoteDataSource(
        @ApplicationContext context: Context,
        tokenStore: TokenStore,
    ): BillingRemoteDataSource = RetrofitBillingRemoteDataSource(
        context = context,
        accessTokenProvider = tokenStore,
    )

    @Provides
    @Singleton
    fun provideEntitlementRemoteDataSource(
        tokenStore: TokenStore,
    ): EntitlementRemoteDataSource = RetrofitEntitlementRemoteDataSource(accessTokenProvider = tokenStore)

    @Provides
    @Singleton
    fun provideBillingCatalogProvider(
        fake: FakeBillingCatalogProvider,
        real: GooglePlayBillingClient,
    ): BillingCatalogProvider = if (com.dramaflow.core.common.AppEnvironment.current.useRealBilling) real else fake

    @Provides
    @Singleton
    fun providePurchaseLauncher(
        fake: FakePurchaseLauncher,
        real: GooglePlayBillingClient,
    ): PurchaseLauncher = if (com.dramaflow.core.common.AppEnvironment.current.useRealBilling) real else fake

    @Provides
    @Singleton
    fun provideBillingRepository(
        impl: DefaultBillingRepository,
    ): BillingRepository = impl

    @Provides
    @Singleton
    fun provideEntitlementRepository(
        fake: FakeEntitlementRepository,
        remote: RemoteEntitlementRepository,
    ): EntitlementRepository = if (com.dramaflow.core.common.AppEnvironment.current.useRealEntitlements) remote else fake

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        fake: FakeSubscriptionRepository,
        remote: RemoteSubscriptionRepository,
    ): SubscriptionRepository = if (com.dramaflow.core.common.AppEnvironment.current.useRealBilling) remote else fake
}
