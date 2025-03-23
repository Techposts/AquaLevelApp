package com.aqualevel.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.aqualevel.R
import com.aqualevel.databinding.ViewErrorBinding

/**
 * Custom view for showing error states
 */
class ErrorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewErrorBinding

    private val titleTextView: TextView
    private val messageTextView: TextView
    private val imageView: ImageView
    private val retryButton: Button

    init {
        binding = ViewErrorBinding.inflate(LayoutInflater.from(context), this, true)

        titleTextView = binding.tvErrorTitle
        messageTextView = binding.tvErrorMessage
        imageView = binding.ivError
        retryButton = binding.btnRetry

        // Get attributes from XML if any
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ErrorView)

        try {
            val title = typedArray.getString(R.styleable.ErrorView_errorTitle)
            val message = typedArray.getString(R.styleable.ErrorView_errorMessage)
            val buttonText = typedArray.getString(R.styleable.ErrorView_retryButtonText)
            val imageRes = typedArray.getResourceId(R.styleable.ErrorView_errorImage, R.drawable.ic_error)

            setTitle(title)
            setMessage(message)
            setButtonText(buttonText)
            setImage(imageRes)
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * Set the title text
     */
    fun setTitle(title: String?) {
        title?.let {
            titleTextView.text = it
            titleTextView.visibility = VISIBLE
        } ?: run {
            titleTextView.visibility = GONE
        }
    }

    /**
     * Set the error message
     */
    fun setMessage(message: String?) {
        message?.let {
            messageTextView.text = it
            messageTextView.visibility = VISIBLE
        } ?: run {
            messageTextView.visibility = GONE
        }
    }

    /**
     * Set the image resource
     */
    fun setImage(resId: Int) {
        imageView.setImageResource(resId)
    }

    /**
     * Set the retry button text
     */
    fun setButtonText(text: String?) {
        text?.let {
            retryButton.text = it
            retryButton.visibility = VISIBLE
        } ?: run {
            retryButton.text = context.getString(R.string.retry)
        }
    }

    /**
     * Set the retry button click listener
     */
    fun setOnRetryClickListener(listener: OnClickListener) {
        retryButton.setOnClickListener(listener)
    }

    /**
     * Show the retry button
     */
    fun showRetryButton(show: Boolean) {
        retryButton.visibility = if (show) VISIBLE else GONE
    }
}