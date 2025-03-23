package com.aqualevel.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.aqualevel.R
import com.aqualevel.databinding.ViewEmptyListBinding

/**
 * Custom view for showing empty state in lists
 */
class EmptyListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewEmptyListBinding

    private val titleTextView: TextView
    private val messageTextView: TextView
    private val imageView: ImageView
    private val actionButton: Button

    init {
        binding = ViewEmptyListBinding.inflate(LayoutInflater.from(context), this, true)

        titleTextView = binding.tvEmptyTitle
        messageTextView = binding.tvEmptyMessage
        imageView = binding.ivEmpty
        actionButton = binding.btnEmptyAction

        // Get attributes from XML if any
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.EmptyListView)

        try {
            val title = typedArray.getString(R.styleable.EmptyListView_emptyTitle)
            val message = typedArray.getString(R.styleable.EmptyListView_emptyMessage)
            val buttonText = typedArray.getString(R.styleable.EmptyListView_buttonText)
            val imageRes = typedArray.getResourceId(R.styleable.EmptyListView_emptyImage, R.drawable.ic_empty_list)

            setTitle(title)
            setMessage(message)
            setButtonText(buttonText)
            setImage(imageRes)

            if (buttonText.isNullOrEmpty()) {
                actionButton.visibility = GONE
            }
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
     * Set the message text
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
     * Set the button text
     */
    fun setButtonText(text: String?) {
        text?.let {
            actionButton.text = it
            actionButton.visibility = VISIBLE
        } ?: run {
            actionButton.visibility = GONE
        }
    }

    /**
     * Set the button click listener
     */
    fun setOnActionButtonClickListener(listener: OnClickListener) {
        actionButton.setOnClickListener(listener)
    }
}