package com.aqualevel.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aqualevel.R
import com.aqualevel.api.ApiResult
import com.aqualevel.api.model.TankData
import com.aqualevel.databinding.FragmentDeviceDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class DeviceDetailFragment : Fragment(), MenuProvider {

    private var _binding: FragmentDeviceDetailBinding? = null
    private val binding get() = _binding!!

    private val args: DeviceDetailFragmentArgs by navArgs()
    private val viewModel: DeviceDetailViewModel by viewModels()

    private var refreshJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupUI()
        observeViewModel()

        // Initialize with device ID from arguments
        viewModel.initialize(args.deviceId)

        // Start automatic refresh
        startAutoRefresh()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupUI() {
        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshDeviceData()
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(
                DeviceDetailFragmentDirections.actionDeviceDetailFragmentToSettingsFragment(args.deviceId)
            )
        }

        // Calibrate buttons
        binding.btnCalibrateEmpty.setOnClickListener {
            showCalibrateEmptyDialog()
        }

        binding.btnCalibrateFull.setOnClickListener {
            showCalibrateFullDialog()
        }

        // History button
        binding.btnHistory.setOnClickListener {
            findNavController().navigate(
                DeviceDetailFragmentDirections.actionDeviceDetailFragmentToHistoryFragment(args.deviceId)
            )
        }
    }

    private fun observeViewModel() {
        // Observe device
        viewModel.device.observe(viewLifecycleOwner) { device ->
            device?.let {
                // Set device name in toolbar
                requireActivity().title = it.name
            }
        }

        // Observe tank data
        viewModel.tankData.observe(viewLifecycleOwner) { tankData ->
            updateTankVisualization(tankData)
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        // Observe refresh time
        viewModel.lastRefreshed.observe(viewLifecycleOwner) { time ->
            updateLastRefreshedTime(time)
        }

        // Observe connection state
        viewModel.isConnected.observe(viewLifecycleOwner) { connected ->
            updateConnectionStatus(connected)
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateTankVisualization(tankData: TankData?) {
        tankData?.let { data ->
            // Set water level
            binding.tankView.setWaterLevel(data.percentage / 100f)

            // Set water level text
            binding.tvWaterLevel.text = getString(R.string.water_level_percent, data.percentage.toInt())

            // Set water volume
            binding.tvVolume.text = getString(R.string.volume_value, data.volume, data.tankVolume)

            // Set progress indicator
            binding.progressIndicator.progress = data.percentage.toInt()

            // Set alert status
            val isLowAlert = data.alertsEnabled && data.percentage <= data.alertLevelLow
            val isHighAlert = data.alertsEnabled && data.percentage >= data.alertLevelHigh

            binding.alertLow.visibility = if (isLowAlert) View.VISIBLE else View.GONE
            binding.alertHigh.visibility = if (isHighAlert) View.VISIBLE else View.GONE

            // Show content
            binding.contentContainer.visibility = View.VISIBLE
            binding.errorContainer.visibility = View.GONE
        } ?: run {
            // Hide content if no data
            binding.contentContainer.visibility = View.GONE
            binding.errorContainer.visibility = View.VISIBLE
        }
    }

    private fun updateLastRefreshedTime(time: Long?) {
        time?.let {
            val dateFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
            binding.tvLastUpdated.text = getString(
                R.string.last_updated,
                dateFormat.format(Date(it))
            )
        }
    }

    private fun updateConnectionStatus(connected: Boolean) {
        if (connected) {
            binding.tvConnectionStatus.text = getString(R.string.connected)
            binding.tvConnectionStatus.setTextColor(requireContext().getColor(R.color.green_success))
            binding.ivConnectionStatus.setImageResource(R.drawable.ic_check_circle)
            binding.ivConnectionStatus.setColorFilter(requireContext().getColor(R.color.green_success))
        } else {
            binding.tvConnectionStatus.text = getString(R.string.disconnected)
            binding.tvConnectionStatus.setTextColor(requireContext().getColor(R.color.red_error))
            binding.ivConnectionStatus.setImageResource(R.drawable.ic_error)
            binding.ivConnectionStatus.setColorFilter(requireContext().getColor(R.color.red_error))
        }
    }

    private fun showCalibrateEmptyDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.calibrate_empty)
            .setMessage(R.string.calibrate_empty_message)
            .setPositiveButton(R.string.calibrate) { _, _ ->
                viewModel.calibrateEmpty()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCalibrateFullDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.calibrate_full)
            .setMessage(R.string.calibrate_full_message)
            .setPositiveButton(R.string.calibrate) { _, _ ->
                viewModel.calibrateFull()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startAutoRefresh() {
        stopAutoRefresh()

        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                viewModel.refreshDeviceData()
                delay(30000) // 30 seconds
            }
        }
    }

    private fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    // Menu methods
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.device_detail_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshDeviceData()
                true
            }
            R.id.action_rename -> {
                showRenameDialog()
                true
            }
            R.id.action_delete -> {
                showDeleteDialog()
                true
            }
            else -> false
        }
    }

    private fun showRenameDialog() {
        val device = viewModel.device.value ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_rename_device, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDeviceName)
        editText.setText(device.name)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename_device)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.renameDevice(newName)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_device)
            .setMessage(R.string.delete_device_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteDevice()
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoRefresh()
        _binding = null
    }
}