package com.aqualevel

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.aqualevel.data.PreferenceManager
import com.aqualevel.databinding.ActivityMainBinding
import com.aqualevel.ui.dashboard.DashboardViewModel
import com.aqualevel.ui.onboarding.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val dashboardViewModel: DashboardViewModel by viewModels()

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle splash screen transition
        val splashScreen = installSplashScreen()

        // Keep splash screen visible while we check if onboarding is needed
        splashScreen.setKeepOnScreenCondition { true }

        super.onCreate(savedInstanceState)

        // Check if onboarding is needed
        val onboardingCompleted = runBlocking { preferenceManager.isOnboardingCompleted() }

        if (!onboardingCompleted) {
            // Start onboarding
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // Normal app startup
        splashScreen.setKeepOnScreenCondition { false }

        // Edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Set up Navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up Navigation UI
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.dashboardFragment),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)

        // Auto-reconnect to last used device if any
        lifecycleScope.launch {
            dashboardViewModel.reconnectToLastDevice()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}