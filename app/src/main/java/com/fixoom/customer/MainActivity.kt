package com.fixoom.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fixoom.customer.data.ThemePreferences
import com.fixoom.customer.navigation.NavGraph
import com.fixoom.customer.ui.components.BottomNavigationBar
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val themePreferences =
            ThemePreferences(applicationContext)

        setContent {
            val isDarkMode by themePreferences
                .isDarkMode
                .collectAsState(initial = false)

            val coroutineScope = rememberCoroutineScope()

            Fixoom_ServicesTheme(
                darkTheme = isDarkMode
            ) {
                FixoomApp(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = { enabled ->
                        coroutineScope.launch {
                            themePreferences.setDarkMode(enabled)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FixoomApp(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                navController = navController
            )
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            isDarkMode = isDarkMode,
            onDarkModeChange = onDarkModeChange
        )
    }
}