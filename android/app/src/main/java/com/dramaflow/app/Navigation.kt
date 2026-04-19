package com.dramaflow.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.dramaflow.feature.detail.DetailRoute
import com.dramaflow.feature.feed.FeedRoute
import com.dramaflow.feature.feed.SearchRoute
import com.dramaflow.feature.player.PlayerRoute
import com.dramaflow.feature.profile.ProfileRoute
import com.dramaflow.feature.subscription.SubscriptionEntrySource
import com.dramaflow.feature.subscription.SubscriptionRoute

sealed class DramaFlowDestination(val route: String) {
    data object Feed : DramaFlowDestination("feed")
    data object Detail : DramaFlowDestination("detail/{dramaId}") {
        fun createRoute(dramaId: String) = "detail/$dramaId"
    }

    data object Player : DramaFlowDestination("player/{episodeId}") {
        fun createRoute(episodeId: String) = "player/$episodeId"
    }

    data object Subscription : DramaFlowDestination("subscription?source={source}") {
        fun createRoute(source: SubscriptionEntrySource) = "subscription?source=${source.name.lowercase()}"
    }

    data object Profile : DramaFlowDestination("profile")
    data object Search : DramaFlowDestination("search")
}

@Composable
fun DramaFlowApp() {
    RootNavHost(navController = rememberNavController())
}

@Composable
fun RootNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = DramaFlowDestination.Feed.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(DramaFlowDestination.Feed.route) {
            FeedRoute(
                onDramaClick = { navController.navigate(DramaFlowDestination.Detail.createRoute(it)) },
                onContinueWatching = { navController.navigate(DramaFlowDestination.Player.createRoute(it)) },
                onSearchClick = { navController.navigate(DramaFlowDestination.Search.route) },
            )
        }
        composable(DramaFlowDestination.Search.route) {
            SearchRoute(
                onBack = { navController.popBackStack() },
                onDramaClick = { navController.navigate(DramaFlowDestination.Detail.createRoute(it)) },
            )
        }
        composable(
            route = DramaFlowDestination.Detail.route,
            arguments = listOf(navArgument("dramaId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "dramaflow://open/detail/{dramaId}" }),
        ) {
            DetailRoute(
                onBack = { navController.popBackStack() },
                onPlayEpisode = { navController.navigate(DramaFlowDestination.Player.createRoute(it)) },
                onSubscriptionClick = { navController.navigate(DramaFlowDestination.Subscription.createRoute(SubscriptionEntrySource.DETAIL)) },
                onDramaClick = { navController.navigate(DramaFlowDestination.Detail.createRoute(it)) },
            )
        }
        composable(
            route = DramaFlowDestination.Player.route,
            arguments = listOf(navArgument("episodeId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "dramaflow://open/player/{episodeId}" }),
        ) {
            PlayerRoute(
                onBack = { navController.popBackStack() },
                onUnlock = { navController.navigate(DramaFlowDestination.Subscription.createRoute(SubscriptionEntrySource.PLAYER)) },
            )
        }
        composable(
            route = DramaFlowDestination.Subscription.route,
            arguments = listOf(
                navArgument("source") {
                    type = NavType.StringType
                    defaultValue = SubscriptionEntrySource.DETAIL.name.lowercase()
                },
            ),
        ) {
            SubscriptionRoute(
                onBack = { navController.popBackStack() },
                onPurchaseSuccess = { navController.popBackStack() },
            )
        }
        composable(DramaFlowDestination.Profile.route) {
            ProfileRoute(
                onBack = { navController.popBackStack() },
                onSubscriptionClick = { navController.navigate(DramaFlowDestination.Subscription.createRoute(SubscriptionEntrySource.PROFILE)) },
                onContinueWatching = { navController.navigate(DramaFlowDestination.Player.createRoute(it)) },
            )
        }
    }
}
