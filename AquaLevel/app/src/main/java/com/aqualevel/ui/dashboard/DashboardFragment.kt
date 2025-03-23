package com.aqualevel.ui.dashboard

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.aqualevel.R
import com.aqualevel.data.database.entity.Device
import com.aqualevel.databinding.FragmentDashboardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment(), MenuProvider {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupUI()
        setupDevicesList()
        observeViewModel()

        // Start device discovery
        viewModel.startDeviceDiscovery()
    }

    private fun setupMenu() {
        // Add menu items without using the Fragment Menu APIs
        // Note that the menu will not be shown until you call activity?.invalidateOptionsMenu()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupUI() {
        // Fab to add new device
        binding.fabAddDevice.setOnClickListener {
            showAddDeviceDialog()
        }

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshDevices()
        }
    }

    private fun setupDevicesList() {
        deviceAdapter = DeviceAdapter(
            onDeviceClick = { device ->
                // Navigate to device detail
                findNavController().navigate(
                    DashboardFragmentDirections.actionDashboardFragmentToDeviceDetailFragment(device.id)
                )
            },
            onFavoriteClick = { device ->
                // Toggle favorite status
                lifecycleScope.launch {
                    viewModel.toggleFavorite(device.id)
                }
            }
        )

        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }

    private fun observeViewModel() {
        // Observe devices list
        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            deviceAdapter.submitList(devices)

            // Update empty state
            if (devices.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvDevices.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvDevices.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        // Observe discovered devices
        viewModel.discoveredDevices.observe(viewLifecycleOwner) { devices ->
            if (devices.isNotEmpty()) {
                // Update the adapter with discovered devices if there are no saved devices
                if ((viewModel.devices.value?.isEmpty() == true) && binding.emptyState.visibility == View.VISIBLE) {
                    showDevicesFoundSnackbar(devices.size)
                }
            }
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showDevicesFoundSnackbar(count: Int) {
        val message = resources.getQuantityString(
            R.plurals.devices_found_message,
            count,
            count
        )

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.view) {
                viewModel.addDiscoveredDevices()
            }
            .show()
    }

    private fun showAddDeviceDialog() {
        // Navigate to add device flow
        findNavController().navigate(
            DashboardFragmentDirections.actionDashboardFragmentToAddDeviceFragment()
        )
    }

    // Menu methods
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshDevices()
                true
            }
            R.id.action_settings -> {
                findNavController().navigate(
                    DashboardFragmentDirections.actionDashboardFragmentToSettingsFragment()
                )
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}