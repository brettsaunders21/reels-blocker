package com.brettsaunders.reelsblocker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** Days are a bitmask: bit 0 = Monday ... bit 6 = Sunday. Windows where end <= start run past midnight. */
data class BlockWindow(
    val id: Long,
    val startMinute: Int,
    val endMinute: Int,
    val days: Int,
) {
    fun isActive(cal: Calendar): Boolean {
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val today = dayIndex(cal)
        val yesterday = (today + 6) % 7
        if (startMinute == endMinute) return hasDay(today)
        return if (startMinute < endMinute) {
            hasDay(today) && now >= startMinute && now < endMinute
        } else {
            (hasDay(today) && now >= startMinute) || (hasDay(yesterday) && now < endMinute)
        }
    }

    fun hasDay(index: Int) = days and (1 shl index) != 0

    fun describeDays(): String {
        val dayText = when (days) {
            0b1111111 -> "Every day"
            0b0011111 -> "Weekdays"
            0b1100000 -> "Weekends"
            else -> DAY_NAMES.filterIndexed { i, _ -> hasDay(i) }.joinToString(", ") { it.take(3) }
        }
        return dayText
    }

    fun describeTime(): String =
        if (startMinute == endMinute) "All day" else "${formatTime(startMinute)} – ${formatTime(endMinute)}"


    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("start", startMinute).put("end", endMinute).put("days", days)

    companion object {
        val DAY_NAMES = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        fun fromJson(o: JSONObject) = BlockWindow(
            o.getLong("id"), o.getInt("start"), o.getInt("end"), o.getInt("days"),
        )

        fun dayIndex(cal: Calendar): Int = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

        fun formatTime(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
    }
}

class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) = prefs.edit().putBoolean("enabled", v).apply()

    var alwaysBlock: Boolean
        get() = prefs.getBoolean("always_block", false)
        set(v) = prefs.edit().putBoolean("always_block", v).apply()

    var windows: List<BlockWindow>
        get() {
            val raw = prefs.getString("windows", null) ?: return emptyList()
            val arr = JSONArray(raw)
            return (0 until arr.length()).map { BlockWindow.fromJson(arr.getJSONObject(it)) }
        }
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(it.toJson()) }
            prefs.edit().putString("windows", arr.toString()).apply()
        }

    fun activeWindow(cal: Calendar = Calendar.getInstance()): BlockWindow? =
        if (enabled) windows.firstOrNull { it.isActive(cal) } else null

    fun isBlockingNow(cal: Calendar = Calendar.getInstance()): Boolean {
        if (!enabled) return false
        if (alwaysBlock) return true
        return windows.any { it.isActive(cal) }
    }
}
