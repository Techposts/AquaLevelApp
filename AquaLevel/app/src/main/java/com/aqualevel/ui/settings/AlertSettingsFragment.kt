package com.aqualevel.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aqualevel.R
import com.aqualevel.api.model.AlertSettingsRequest
import com.aqualevel.databinding.FragmentAlertSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlertSettingsFragment : Fragment() {

    private var _binding: FragmentAlertSettingsBinding? = null
    private val binding get() = _binding!!

    private val args: AlertSettingsFragmentArgs by navArgs()
    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()

        // Initialize with device ID from arguments
        viewModel.initialize(args.deviceId)
    }

    private fun setupUI() {
        // Save button
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        // Observe device settings
        viewModel.deviceSettings.observe(viewLifecycleOwner) { settings ->
            settings?.let {
                // Populate form with current settings
                binding.sliderLowLevel.value = settings.alertLevelLow.toFloat()
                binding.sliderHighLevel.value = settings.alertLevelHigh.toFloat()
                binding.switchAlerts.isChecked = settings.alertsEnabled

                // Update text views
                updateAlertLevelText()
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !loading
        }

        // Observe save result
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Slider value change listeners
        binding.sliderLowLevel.addOnChangeListener { _, value, _ ->
            binding.tvLowLevel.text = getString(R.string.low_level_percent, value.toInt())

            // Ensure low level is less than high level
            if (value > binding.sliderHighLevel.value) {
                binding.sliderHighLevel.value = value
                binding.tvHighLevel.text = getString(R.string.high_level_percent, value.toInt())
            }
        }

        binding.sliderHighLevel.addOnChangeListener { _, value, _ ->
            binding.tvHighLevel.text = getString(R.string.high_level_percent, value.toInt())

            // Ensure high level is greater than low level
            if (value < binding.sliderLowLevel.value) {
                binding.sliderLowLevel.value = value
                binding.tvLowLevel.text = getString(R.string.low_level_percent, value.toInt())
            }
        }
    }

    private fun updateAlertLevelText() {
        binding.tvLowLevel.text = getString(
            R.string.low_level_percent,
            binding.sliderLowLevel.value.toInt()
        )
        binding.tvHighLevel.text = getString(
            R.string.high_level_percent,
            binding.sliderHighLevel.value.toInt()
        )
    }

    private fun saveSettings() {
        // Get values from form
        val alertLevelLow = binding.sliderLowLevel.value.toInt()
        val alertLevelHigh = binding.sliderHighLevel.value.toInt()
        val alertsEnabled = binding.switchAlerts.isChecked

        // Create settings request
        val request = AlertSettingsRequest(
            alertLevelLow = alertLevelLow,
            alertLevelHigh = alertLevelHigh,
            alertsEnabled = alertsEnabled
        )

        // Save settings
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveAlertSettings(request)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}