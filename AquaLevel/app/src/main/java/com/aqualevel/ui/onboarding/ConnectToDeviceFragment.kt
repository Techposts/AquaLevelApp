package com.aqualevel.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.aqualevel.R
import com.aqualevel.databinding.FragmentConnectToDeviceBinding
import com.aqualevel.util.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectToDeviceFragment : Fragment() {

    private var _binding: FragmentConnectToDeviceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeviceSetupViewModel by activityViewModels()

    @Inject
    lateinit var networkUtils: NetworkUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectToDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()

        // Start checking for device connection
        startConnectionCheck()
    }

    private fun setupUI() {
        // Open WiFi settings button
        binding.btnOpenWifiSettings.setOnClickListener {
            openWifiSettings()
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshWifiState()
        }
    }

    private fun observeViewModel() {
        // Observe connection status
        viewModel.connectedToDeviceAp.observe(viewLifecycleOwner) { connected ->
            updateConnectionStatus(connected)
        }

        // Observe current SSID
        viewModel.currentSsid.observe(viewLifecycleOwner) { ssid ->
            binding.txtCurrentNetwork.text = ssid ?: getString(R.string.not_connected)
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun updateConnectionStatus(connected: Boolean) {
        if (connected) {
            binding.imgStatus.setImageResource(R.drawable.ic_check_circle)
            binding.imgStatus.setColorFilter(resources.getColor(R.color.green_success, null))
            binding.txtStatus.text = getString(R.string.connected_to_device)
            binding.txtStatus.setTextColor(resources.getColor(R.color.green_success, null))
            binding.txtInstructions.visibility = View.GONE
            binding.btnOpenWifiSettings.visibility = View.GONE
            binding.animationView.setAnimation(R.raw.anim_success)
            binding.animationView.playAnimation()
        } else {
            binding.imgStatus.setImageResource(R.drawable.ic_warning)
            binding.imgStatus.setColorFilter(resources.getColor(R.color.orange_warning, null))
            binding.txtStatus.text = getString(R.string.not_connected_to_device)
            binding.txtStatus.setTextColor(resources.getColor(R.color.orange_warning, null))
            binding.txtInstructions.visibility = View.VISIBLE
            binding.btnOpenWifiSettings.visibility = View.VISIBLE
            binding.animationView.setAnimation(R.raw.anim_connect_wifi)
            binding.animationView.playAnimation()
        }
    }

    private fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
    }

    private fun startConnectionCheck() {
        // Initial check
        viewModel.refreshWifiState()

        // Start periodic checks
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                // Check every 3 seconds
                delay(3000)
                viewModel.refreshWifiState()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}