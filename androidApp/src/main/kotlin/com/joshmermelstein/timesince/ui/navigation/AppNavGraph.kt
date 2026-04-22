package com.joshmermelstein.timesince.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.joshmermelstein.timesince.ui.settings.SettingsScreen
import com.joshmermelstein.timesince.ui.taskedit.TaskEditScreen
import com.joshmermelstein.timesince.ui.tasklist.TaskListScreen

object Routes {
    const val TASK_LIST = "tasks"
    const val TASK_NEW = "tasks/new"
    const val TASK_EDIT = "tasks/edit/{taskId}"
    const val SETTINGS = "settings"

    fun editTask(taskId: String): String = "tasks/edit/$taskId"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
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
            )
        }
        composable(Routes.TASK_NEW) {
            TaskEditScreen(
                taskId = null,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
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
