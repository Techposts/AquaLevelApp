package com.aqualevel.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.roundToInt

/**
 * Extension functions for general use throughout the app
 */

/**
 * Launches a coroutine in a lifecycle-aware way
 */
fun LifecycleOwner.launchWhenStarted(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }
}

/**
 * Shows toast message
 */
fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

/**
 * Shows toast message from fragment
 */
fun Fragment.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    context?.toast(message, length)
}

/**
 * Hides the keyboard
 */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Shows the keyboard and focuses on the edit text
 */
fun EditText.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * Converts dp to pixels
 */
fun Context.dpToPx(dp: Float): Int {
    return (dp * resources.displayMetrics.density).roundToInt()
}

/**
 * Clamp a value between min and max
 */
fun Float.clamp(min: Float, max: Float): Float {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
}

/**
 * Safe decimal format
 */
fun Float.formatDecimal(digits: Int = 1): String {
    val pattern = StringBuilder().apply {
        append("0")
        if (digits > 0) {
            append(".")
            repeat(digits) { append("0") }
        }
    }.toString()

    return DecimalFormat(pattern).format(this)
}

/**
 * Check if view is visible
 */
fun View.isVisible(): Boolean {
    return visibility == View.VISIBLE
}

/**
 * Set view visibility to VISIBLE
 */
fun View.visible() {
    visibility = View.VISIBLE
}

/**
 * Set view visibility to INVISIBLE
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Set view visibility to GONE
 */
fun View.gone() {
    visibility = View.GONE
}

/**
 * Set view visibility based on condition
 */
fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Format timestamp to a readable date string
 */
fun Long.formatDateTime(pattern: String = "MM/dd/yyyy HH:mm"): String {
    val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
    return sdf.format(java.util.Date(this))
}

/**
 * Get the app version name
 */
fun Context.getAppVersionName(): String {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName
    } catch (e: Exception) {
        "Unknown"
    }
}