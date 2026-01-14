package io.oworld.ofexpense.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
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
import io.oworld.ofexpense.ui.screen.SynchronizeScreen
import io.oworld.ofexpense.ui.theme.OFExpenseTheme
import kotlin.time.ExperimentalTime


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            var title by remember { mutableStateOf("") }
            if (!Environment.isExternalStorageManager()) {
                val getPermission = Intent()
                getPermission.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(getPermission)
            }
            LaunchedEffect(navController, title) {
                navController.currentBackStackEntryFlow.collect { backStackEntry ->
                    val route = backStackEntry.destination.route.toString()
                    val indexOfSlash = route.indexOf('/')
                    title = if (indexOfSlash == -1) {
                        route
                    } else {
                        route.take(indexOfSlash)
                    }
                }
            }
            OFExpenseTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
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
                                        imageVector = ImageVector.vectorResource(R.drawable.round_attach_money_24),
                                        contentDescription = LocalResources.current.getString(R.string.statistics)
                                    )
                                }
                                IconButton(onClick = {
                                    val myRoute = resources.getString(R.string.synchronize)
                                    if (navController.currentDestination?.route != myRoute) {
                                        navController.navigate(myRoute)
                                    }
                                }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.round_sync_alt_24),
                                        contentDescription = LocalResources.current.getString(R.string.synchronize)
                                    )
                                }
                                IconButton(onClick = {
                                    val myRoute = resources.getString(R.string.statistics)
                                    if (navController.currentDestination?.route != myRoute) {
                                        navController.navigate(myRoute)
                                    }
                                }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.round_pie_chart_24),
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
                        composable(resources.getString(R.string.synchronize)) {
                            SynchronizeScreen(innerPadding)
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