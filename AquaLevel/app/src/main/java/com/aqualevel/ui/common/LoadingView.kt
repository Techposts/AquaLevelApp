package com.aqualevel.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.aqualevel.R
import com.aqualevel.databinding.ViewLoadingBinding

/**
 * Custom view for showing loading states
 */
class LoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewLoadingBinding

    private val progressBar: ProgressBar
    private val messageTextView: TextView

    init {
        binding = ViewLoadingBinding.inflate(LayoutInflater.from(context), this, true)

        progressBar = binding.progressBar
        messageTextView = binding.tvLoadingMessage

        // Get attributes from XML if any
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingView)

        try {
            val message = typedArray.getString(R.styleable.LoadingView_loadingMessage)
            setMessage(message)
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * Set the loading message
     */
    fun setMessage(message: String?) {
        message?.let {
            messageTextView.text = it
            messageTextView.visibility = VISIBLE
        } ?: run {
            messageTextView.text = context.getString(R.string.loading)
        }
    }
}