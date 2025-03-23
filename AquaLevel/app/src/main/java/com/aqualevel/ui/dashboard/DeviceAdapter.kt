package com.aqualevel.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aqualevel.R
import com.aqualevel.data.database.entity.Device
import com.aqualevel.databinding.ItemDeviceBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DeviceAdapter(
    private val onDeviceClick: (Device) -> Unit,
    private val onFavoriteClick: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Device) {
            val context = binding.root.context

            // Set device name
            binding.tvDeviceName.text = device.name

            // Set status icon and text based on last seen time
            val twelveHoursAgo = System.currentTimeMillis() - (12 * 60 * 60 * 1000)
            val isOnline = device.lastSeen.time > twelveHoursAgo

            if (isOnline) {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_online)
                binding.tvStatus.text = context.getString(R.string.online)
                binding.tvStatus.setTextColor(context.getColor(R.color.green_success))
            } else {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_offline)
                binding.tvStatus.text = context.getString(R.string.offline)
                binding.tvStatus.setTextColor(context.getColor(R.color.grey_disabled))
            }

            // Set last seen date
            val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
            binding.tvLastSeen.text = context.getString(
                R.string.last_seen,
                dateFormat.format(device.lastSeen)
            )

            // Set water level
            device.lastPercentage?.let { percentage ->
                binding.waterLevelIndicator.progress = percentage.toInt()
                binding.tvWaterLevel.text = context.getString(
                    R.string.water_level_percent,
                    percentage.toInt()
                )

                // Set water level color based on percentage
                val levelColor = when {
                    percentage <= 10 -> context.getColor(R.color.red_low)
                    percentage <= 30 -> context.getColor(R.color.orange_warning)
                    else -> context.getColor(R.color.blue_water)
                }
                binding.waterLevelIndicator.setIndicatorColor(levelColor)

                // Make water level visible
                binding.waterLevelContainer.visibility = android.view.View.VISIBLE
            } ?: run {
                // Hide water level if no data
                binding.waterLevelContainer.visibility = android.view.View.GONE
            }

            // Set favorite icon
            binding.btnFavorite.setImageResource(
                if (device.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_outline
            )

            // Click listeners
            binding.root.setOnClickListener { onDeviceClick(device) }
            binding.btnFavorite.setOnClickListener { onFavoriteClick(device) }
        }
    }
}

/**
 * DiffUtil callback for Device items
 */
class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
    override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
        return oldItem == newItem
    }
}