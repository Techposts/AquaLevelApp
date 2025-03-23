package com.aqualevel.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.aqualevel.MainActivity
import com.aqualevel.R
import com.aqualevel.data.PreferenceManager
import com.aqualevel.databinding.ActivityOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingAdapter: OnboardingPagerAdapter

    private val viewModel: DeviceSetupViewModel by viewModels()

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val LOCATION_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request required permissions
        requestRequiredPermissions()

        // Setup ViewPager for onboarding steps
        onboardingAdapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = onboardingAdapter

        // Disable swiping between pages (we'll handle navigation manually)
        binding.viewPager.isUserInputEnabled = false

        // Connect tab dots with ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ ->
            // No configuration needed for dots
        }.attach()

        // Setup navigation buttons
        setupNavigationButtons()

        // Observe device discovery/setup process
        observeSetupProcess()
    }

    private fun requestRequiredPermissions() {
        val requiredPermissions = mutableListOf<String>()

        // Location permission for Wi-Fi scanning
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // For Android 12+, add NEARBY_WIFI_DEVICES permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        // Request permissions if needed
        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toTypedArray(),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    private fun setupNavigationButtons() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < onboardingAdapter.itemCount - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                // Last page, complete onboarding
                completeOnboarding()
            }
        }

        binding.btnBack.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem > 0) {
                binding.viewPager.currentItem = currentItem - 1
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Update button visibility
                binding.btnBack.visibility = if (position > 0) android.view.View.VISIBLE else android.view.View.INVISIBLE

                // Update next button text for last page
                binding.btnNext.text = if (position == onboardingAdapter.itemCount - 1) {
                    getString(R.string.finish)
                } else {
                    getString(R.string.next)
                }

                // Update progress
                updateProgressIndicator(position)
            }
        })
    }

    private fun updateProgressIndicator(position: Int) {
        val progress = ((position + 1).toFloat() / onboardingAdapter.itemCount * 100).toInt()
        binding.progressIndicator.progress = progress
    }

    private fun observeSetupProcess() {
        // Observe connection to device AP
        viewModel.connectedToDeviceAp.observe(this) { connected ->
            if (connected) {
                // When connected to device AP, move to next page
                moveToNextPage()
            }
        }

        // Observe WiFi setup completion
        viewModel.wifiSetupComplete.observe(this) { success ->
            if (success) {
                // When WiFi setup is complete, move to device found page
                moveToNextPage()
            }
        }

        // Observe device discovery
        viewModel.deviceDiscovered.observe(this) { discovered ->
            if (discovered) {
                // When device is discovered on network, finish setup
                binding.btnNext.isEnabled = true
            }
        }
    }

    private fun moveToNextPage() {
        val currentItem = binding.viewPager.currentItem
        if (currentItem < onboardingAdapter.itemCount - 1) {
            binding.viewPager.currentItem = currentItem + 1
        }
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            // Mark onboarding as completed
            preferenceManager.setOnboardingCompleted(true)

            // Start main activity
            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            // Check if location permission was granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue with Wi-Fi functions
                viewModel.refreshWifiState()
            } else {
                // Permission denied - this will limit functionality
                // We can still try to proceed, but some features may not work
            }
        }
    }
}

/**
 * Adapter for onboarding pages
 */
class OnboardingPagerAdapter(activity: AppCompatActivity) :
    androidx.viewpager2.adapter.FragmentStateAdapter(activity) {

    private val fragments = listOf(
        WelcomeFragment(),
        ConnectToDeviceFragment(),
        WifiSetupFragment(),
        DeviceFoundFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int) = fragments[position]
}

/**
 * Welcome screen fragment
 */
class WelcomeFragment : androidx.fragment.app.Fragment(R.layout.fragment_welcome_onboarding)

/**
 * Final "device found" fragment
 */
class DeviceFoundFragment : androidx.fragment.app.Fragment(R.layout.fragment_device_found_onboarding)