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
import com.aqualevel.api.model.TankSettingsRequest
import com.aqualevel.databinding.FragmentTankSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TankSettingsFragment : Fragment() {

    private var _binding: FragmentTankSettingsBinding? = null
    private val binding get() = _binding!!

    private val args: TankSettingsFragmentArgs by navArgs()
    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTankSettingsBinding.inflate(inflater, container, false)
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
                binding.etTankHeight.setText(settings.tankHeight.toString())
                binding.etTankDiameter.setText(settings.tankDiameter.toString())
                binding.etTankVolume.setText(settings.tankVolume.toString())
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
    }

    private fun saveSettings() {
        try {
            // Get values from form
            val tankHeight = binding.etTankHeight.text.toString().toFloatOrNull()
            val tankDiameter = binding.etTankDiameter.text.toString().toFloatOrNull()
            val tankVolume = binding.etTankVolume.text.toString().toFloatOrNull()

            // Validate values
            if (tankHeight == null || tankDiameter == null || tankVolume == null) {
                Toast.makeText(requireContext(), R.string.invalid_values, Toast.LENGTH_SHORT).show()
                return
            }

            // Create settings request
            val request = TankSettingsRequest(
                tankHeight = tankHeight,
                tankDiameter = tankDiameter,
                tankVolume = tankVolume
            )

            // Save settings
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.saveTankSettings(request)
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.invalid_values, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}