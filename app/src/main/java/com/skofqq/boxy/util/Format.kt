package com.skofqq.boxy.util

import android.content.Context
import com.skofqq.boxy.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Byte sizes and speeds with localized units (B/KB/MB… or Б/КБ/МБ…). */
object Format {
    fun bytes(context: Context, value: Long): String {
        val units = intArrayOf(R.string.unit_b, R.string.unit_kb, R.string.unit_mb, R.string.unit_gb, R.string.unit_tb, R.string.unit_pb)
        var v = value.coerceAtLeast(0).toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024
            i++
        }
        val number = when {
            i == 0 -> v.toLong().toString()
            i >= 3 -> String.format(Locale.getDefault(), "%.2f", v)
            v < 10 -> String.format(Locale.getDefault(), "%.1f", v)
            else -> String.format(Locale.getDefault(), "%.0f", v)
        }
        return "$number ${context.getString(units[i])}"
    }

    fun speed(context: Context, bytesPerSec: Long): String = context.getString(R.string.unit_per_second, bytes(context, bytesPerSec))

    fun ms(context: Context, value: Long): String = context.getString(R.string.unit_ms, value)

    fun date(unixSeconds: Long): String =
        if (unixSeconds <= 0) "-" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(unixSeconds * 1000))

    fun dateTime(millis: Long): String = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    fun uptime(context: Context, seconds: Long): String {
        val minutes = seconds / 60
        return when {
            minutes < 1 -> context.getString(R.string.home_uptime_lt_minute)
            minutes < 60 -> context.getString(R.string.home_uptime_minutes, minutes.toInt())
            else -> context.getString(R.string.home_uptime_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
        }
    }
}
