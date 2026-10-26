package com.miaugrid.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miaugrid.app.MiauGridApp
import com.miaugrid.app.di.AppContainer
import com.miaugrid.app.ui.daily.DailyScreen
import com.miaugrid.app.ui.daily.DailyViewModel
import com.miaugrid.app.ui.game.GameRoute
import com.miaugrid.app.ui.game.GameViewModel
import com.miaugrid.app.ui.home.HomeScreen
import com.miaugrid.app.ui.home.HomeViewModel
import com.miaugrid.app.ui.levels.LevelSelectScreen
import com.miaugrid.app.ui.levels.LevelSelectViewModel
import com.miaugrid.app.ui.settings.SettingsScreen
import com.miaugrid.app.ui.settings.SettingsViewModel
import com.miaugrid.app.ui.splash.SplashScreen
import com.miaugrid.app.ui.stats.StatsScreen
import com.miaugrid.app.ui.stats.StatsViewModel
import com.miaugrid.core.game.GameModeId

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val LEVELS = "levels"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val DAILY = "daily"
    const val GAME = "game/{${GameViewModel.ARG_MODE}}/{${GameViewModel.ARG_KEY}}"

    fun game(mode: GameModeId, key: String) = "game/${mode.name}/$key"
}

/** Cria ViewModels com acesso ao contêiner de dependências e ao SavedStateHandle. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer, SavedStateHandle) -> VM): VM {
    val container = (LocalContext.current.applicationContext as MiauGridApp).container
    return viewModel(factory = viewModelFactory { initializer { create(container, createSavedStateHandle()) } })
}

/**
 * Navega apenas se a tela atual estiver ativa (RESUMED). Durante uma transição a tela de
 * origem não está RESUMED, então toques repetidos não empilham telas duplicadas.
 */
fun NavController.navigateSafely(route: String, popUpToRoute: String? = null, inclusive: Boolean = false) {
    if (currentBackStackEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) return
    navigate(route) {
        launchSingleTop = true
        if (popUpToRoute != null) popUpTo(popUpToRoute) { this.inclusive = inclusive }
    }
}

fun NavController.popSafely() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) popBackStack()
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            // Chamado uma única vez; navegação direta para não ficar presa se o app for minimizado.
            SplashScreen(onFinished = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            val vm = appViewModel { c, _ -> HomeViewModel(c) }
            HomeScreen(
                viewModel = vm,
                onPlayLevel = { id -> navController.navigateSafely(Routes.game(GameModeId.CLASSIC, id)) },
                onOpenLevels = { navController.navigateSafely(Routes.LEVELS) },
                onOpenDaily = { navController.navigateSafely(Routes.DAILY) },
                onOpenStats = { navController.navigateSafely(Routes.STATS) },
                onOpenSettings = { navController.navigateSafely(Routes.SETTINGS) },
            )
        }
        composable(Routes.LEVELS) {
            val vm = appViewModel { c, _ -> LevelSelectViewModel(c) }
            LevelSelectScreen(
                viewModel = vm,
                onBack = { navController.popSafely() },
                onPlay = { id -> navController.navigateSafely(Routes.game(GameModeId.CLASSIC, id)) },
            )
        }
        composable(
            Routes.GAME,
            arguments = listOf(
                navArgument(GameViewModel.ARG_MODE) { type = NavType.StringType },
                navArgument(GameViewModel.ARG_KEY) { type = NavType.StringType },
            ),
        ) {
            val vm = appViewModel { c, handle -> GameViewModel(c, handle) }
            GameRoute(
                viewModel = vm,
                onExit = { navController.popBackStack() },
                onOpenSettings = { navController.navigateSafely(Routes.SETTINGS) },
                onOpenLevel = { id ->
                    navController.navigate(Routes.game(GameModeId.CLASSIC, id)) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.DAILY) {
            val vm = appViewModel { c, _ -> DailyViewModel(c) }
            DailyScreen(
                viewModel = vm,
                onBack = { navController.popSafely() },
                onPlay = { day -> navController.navigateSafely(Routes.game(GameModeId.DAILY, GameViewModel.dailyKey(day))) },
            )
        }
        composable(Routes.STATS) {
            val vm = appViewModel { c, _ -> StatsViewModel(c) }
            StatsScreen(viewModel = vm, onBack = { navController.popSafely() })
        }
        composable(Routes.SETTINGS) {
            val vm = appViewModel { c, _ -> SettingsViewModel(c) }
            SettingsScreen(viewModel = vm, onBack = { navController.popSafely() })
        }
    }
}
