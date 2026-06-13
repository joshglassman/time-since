package com.scribbles.timesince.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scribbles.timesince.ui.categories.CategoriesScreen
import com.scribbles.timesince.ui.settings.SettingsScreen
import com.scribbles.timesince.ui.taskedit.TaskEditScreen
import com.scribbles.timesince.ui.tasklist.TaskListScreen

object Routes {
    const val TASK_LIST = "tasks"
    const val TASK_NEW = "tasks/new"
    const val TASK_EDIT = "tasks/edit/{taskId}"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"

    fun editTask(taskId: String): String = "tasks/edit/$taskId"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    scrollToTaskId: String? = null,
    onScrollConsumed: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TASK_LIST,
    ) {
        composable(Routes.TASK_LIST) {
            TaskListScreen(
                onAddTask = { navController.navigate(Routes.TASK_NEW) },
                onEditTask = { id -> navController.navigate(Routes.editTask(id)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                scrollToTaskId = scrollToTaskId,
                onScrollConsumed = onScrollConsumed,
            )
        }
        composable(Routes.TASK_NEW) {
            TaskEditScreen(
                taskId = null,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onManageCategories = { navController.navigate(Routes.CATEGORIES) },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.TASK_EDIT,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { backStackEntry ->
            TaskEditScreen(
                taskId = backStackEntry.arguments?.getString("taskId"),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
