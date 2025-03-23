package com.aqualevel.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aqualevel.R
import com.aqualevel.api.model.WiFiNetwork
import com.aqualevel.databinding.FragmentWifiSetupBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WifiSetupFragment : Fragment() {

    private var _binding: FragmentWifiSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeviceSetupViewModel by activityViewModels()
    private lateinit var networksAdapter: WifiNetworksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWifiSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupNetworksList()
        observeViewModel()

        // Scan for networks on first load
        scanNetworks()
    }

    private fun setupUI() {
        // Scan button
        binding.btnScanNetworks.setOnClickListener {
            scanNetworks()
        }

        // Connect button
        binding.btnConnect.setOnClickListener {
            configureWifi()
        }

        // Setup text change listeners for validation
        binding.etDeviceName.doOnTextChanged { _, _, _, _ -> validateInputs() }
        binding.etWifiPassword.doOnTextChanged { _, _, _, _ -> validateInputs() }

        // Initially disable connect button
        binding.btnConnect.isEnabled = false
    }

    private fun setupNetworksList() {
        networksAdapter = WifiNetworksAdapter { network ->
            // When network is selected
            binding.etWifiSsid.setText(network.ssid)
            binding.etWifiPassword.requestFocus()
            validateInputs()
        }

        binding.rvNetworks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = networksAdapter
        }
    }

    private fun observeViewModel() {
        // Observe available networks
        viewModel.availableNetworks.observe(viewLifecycleOwner) { networks ->
            networksAdapter.submitList(networks)

            // Update UI based on whether we found networks
            if (networks.isEmpty()) {
                binding.tvNoNetworks.visibility = View.VISIBLE
                binding.rvNetworks.visibility = View.GONE
            } else {
                binding.tvNoNetworks.visibility = View.GONE
                binding.rvNetworks.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnScanNetworks.isEnabled = !loading
            binding.btnConnect.isEnabled = !loading && isFormValid()
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observe WiFi setup status
        viewModel.wifiSetupComplete.observe(viewLifecycleOwner) { completed ->
            if (completed) {
                // Show success UI
                binding.containerForm.visibility = View.GONE
                binding.containerSuccess.visibility = View.VISIBLE
                binding.animationView.setAnimation(R.raw.anim_success)
                binding.animationView.playAnimation()
            }
        }
    }

    private fun scanNetworks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanNetworks()
        }
    }

    private fun configureWifi() {
        val ssid = binding.etWifiSsid.text.toString().trim()
        val password = binding.etWifiPassword.text.toString()
        val deviceName = binding.etDeviceName.text.toString().trim()

        if (ssid.isNotEmpty() && deviceName.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.configureDeviceWifi(ssid, password, deviceName)
            }
        }
    }

    private fun validateInputs() {
        binding.btnConnect.isEnabled = isFormValid() && !viewModel.loading.value!!
    }

    private fun isFormValid(): Boolean {
        val ssid = binding.etWifiSsid.text.toString().trim()
        val deviceName = binding.etDeviceName.text.toString().trim()

        // SSID and device name are required
        return ssid.isNotEmpty() && deviceName.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adapter for WiFi networks list
 */
class WifiNetworksAdapter(
    private val onNetworkSelected: (WiFiNetwork) -> Unit
) : androidx.recyclerview.widget.ListAdapter<WiFiNetwork, WifiNetworksAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<WiFiNetwork>() {
        override fun areItemsTheSame(oldItem: WiFiNetwork, newItem: WiFiNetwork): Boolean {
            return oldItem.ssid == newItem.ssid
        }

        override fun areContentsTheSame(oldItem: WiFiNetwork, newItem: WiFiNetwork): Boolean {
            return oldItem == newItem
        }
    }
) {

    inner class ViewHolder(private val binding: com.aqualevel.databinding.ItemWifiNetworkBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(network: WiFiNetwork) {
            binding.tvSsid.text = network.ssid

            // Set signal strength icon based on level (0-4)
            val signalIconRes = when (network.getSignalStrength()) {
                4 -> R.drawable.ic_signal_4
                3 -> R.drawable.ic_signal_3
                2 -> R.drawable.ic_signal_2
                1 -> R.drawable.ic_signal_1
                else -> R.drawable.ic_signal_0
            }
            binding.ivSignalStrength.setImageResource(signalIconRes)

            // Show security icon if secure
            binding.ivSecurity.visibility = if (network.secure) View.VISIBLE else View.GONE

            // Click listener
            binding.root.setOnClickListener {
                onNetworkSelected(network)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = com.aqualevel.databinding.ItemWifiNetworkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}