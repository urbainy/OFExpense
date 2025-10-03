package io.oworld.ofexpense.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import io.oworld.ofexpense.R
import io.oworld.ofexpense.ui.screen.ExpenseEditScreen
import io.oworld.ofexpense.ui.screen.ExpensesScreen
import io.oworld.ofexpense.ui.screen.SettingsScreen
import io.oworld.ofexpense.ui.screen.StatisticsScreen
import io.oworld.ofexpense.ui.theme.OFExpenseTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            var title by remember { mutableStateOf("") }
            LaunchedEffect(navController, title) {
                navController.currentBackStackEntryFlow.collect { backStackEntry ->
                    val route = backStackEntry.destination.route.toString()
                    val indexOfSlash = route.indexOf('/')
                    title = if (indexOfSlash == -1) {
                        route
                    } else {
                        route.substring(0, indexOfSlash)
                    }
                }
            }
            OFExpenseTheme {
                Scaffold(
                    modifier = Modifier.Companion.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(title) },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            actions = {
                                IconButton(onClick = {
                                    navController.navigate(resources.getString(R.string.expenses)) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }) {
                                    Icon(
                                        imageVector = ImageVector.Companion.vectorResource(R.drawable.round_attach_money_24),
                                        contentDescription = LocalResources.current.getString(R.string.statistics)
                                    )
                                }
                                IconButton(onClick = {
                                    val myRoute = resources.getString(R.string.statistics)
                                    if (navController.currentDestination?.route != myRoute) {
                                        navController.navigate(myRoute)
                                    }
                                }) {
                                    Icon(
                                        imageVector = ImageVector.Companion.vectorResource(R.drawable.round_pie_chart_24),
                                        contentDescription = LocalResources.current.getString(R.string.statistics)
                                    )
                                }
                                IconButton(onClick = {
                                    val myRoute = resources.getString(R.string.settings)
                                    if (navController.currentDestination?.route != myRoute) {
                                        navController.navigate(myRoute)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = LocalResources.current.getString(R.string.settings),
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = resources.getString(R.string.expenses)
                    ) {
                        composable(resources.getString(R.string.expenses)) {
                            ExpensesScreen(innerPadding, navController = navController)
                        }
                        composable(resources.getString(R.string.statistics)) {
                            StatisticsScreen(innerPadding)
                        }
                        composable(resources.getString(R.string.settings)) {
                            SettingsScreen(innerPadding)
                        }
                        composable(
                            resources.getString(R.string.edit_expense) + "/{expenseId}",
                            arguments = listOf(
                                navArgument("expenseId") { type = NavType.StringType }
                            )) { ExpenseEditScreen(innerPadding, navController) }
                    }
                }
            }
        }
    }
}