package com.aqualevel.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aqualevel.R
import com.aqualevel.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val args: SettingsFragmentArgs? by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        // If a device ID was passed, show device-specific settings
        args?.deviceId?.let { deviceId ->
            // Show device-specific settings
            binding.cardDeviceSettings.visibility = View.VISIBLE

            // Tank Settings
            binding.btnTankSettings.setOnClickListener {
                findNavController().navigate(
                    SettingsFragmentDirections.actionSettingsFragmentToTankSettingsFragment(deviceId)
                )
            }

            // Alert Settings
            binding.btnAlertSettings.setOnClickListener {
                findNavController().navigate(
                    SettingsFragmentDirections.actionSettingsFragmentToAlertSettingsFragment(deviceId)
                )
            }

            // Sensor Settings
            binding.btnSensorSettings.setOnClickListener {
                findNavController().navigate(
                    SettingsFragmentDirections.actionSettingsFragmentToSensorSettingsFragment(deviceId)
                )
            }
        } ?: run {
            // No device ID, hide device-specific settings
            binding.cardDeviceSettings.visibility = View.GONE
        }

        // App Settings
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Handle dark mode toggle
            updateTheme(isChecked)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // Handle notifications toggle
            updateNotifications(isChecked)
        }

        // About
        binding.btnAbout.setOnClickListener {
            findNavController().navigate(
                SettingsFragmentDirections.actionSettingsFragmentToAboutFragment()
            )
        }

        // Help & Support
        binding.btnHelp.setOnClickListener {
            findNavController().navigate(
                SettingsFragmentDirections.actionSettingsFragmentToHelpFragment()
            )
        }
    }

    private fun updateTheme(darkMode: Boolean) {
        // This would be implemented to change the app theme
        // Would use AppCompatDelegate.setDefaultNightMode() in a real implementation
    }

    private fun updateNotifications(enabled: Boolean) {
        // This would be implemented to enable/disable notifications
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}