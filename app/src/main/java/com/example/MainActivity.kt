package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.theme.APKComparatorTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate950

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "apk_comparator_db"
        ).fallbackToDestructiveMigration(true).build()
    }

    private val repository by lazy {
        HistoryRepository(db.comparisonHistoryDao())
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            APKComparatorTheme {
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()

                // Handle Toast Notice
                LaunchedEffect(uiState.userNotice) {
                    uiState.userNotice?.let { notice ->
                        Toast.makeText(this@MainActivity, notice, Toast.LENGTH_LONG).show()
                        viewModel.clearNotice()
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onOpenReport = { navController.navigate("report") }
                            )
                        }
                        composable("report") {
                            ReportScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Global Loading Overlay Dialog
                    if (uiState.isLoading) {
                        Dialog(onDismissRequest = {}) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = CyanGlow)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Memproses Analisis DEX...",
                                        color = Slate100,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.loadingMessage,
                                        color = Slate400,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
