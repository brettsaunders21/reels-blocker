package com.brettsaunders.reelsblocker

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.Secure
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var settings: Settings
    private lateinit var palette: Palette
    private lateinit var hero: LinearLayout
    private lateinit var heroTitle: TextView
    private lateinit var heroSubtitle: TextView
    private lateinit var setupCard: View
    private lateinit var windowList: LinearLayout

    private class Palette(dark: Boolean) {
        val background = if (dark) 0xFF0F0F12.toInt() else 0xFFF5F3F7.toInt()
        val card = if (dark) 0xFF1C1B21.toInt() else Color.WHITE
        val row = if (dark) 0xFF26252C.toInt() else 0xFFF3EFF6.toInt()
        val text = if (dark) 0xFFF2F0F5.toInt() else 0xFF1B1A1F.toInt()
        val subtle = if (dark) 0xFFA8A4B0.toInt() else 0xFF6B6672.toInt()
        val accent = 0xFFE1306C.toInt()
        val warning = if (dark) 0xFF3A2A12.toInt() else 0xFFFFF1DC.toInt()
        val warningText = if (dark) 0xFFFFD08A.toInt() else 0xFF7A4B00.toInt()
        val blocked = intArrayOf(0xFF833AB4.toInt(), 0xFFE1306C.toInt(), 0xFFF77737.toInt())
        val allowed = intArrayOf(0xFF2E7D6B.toInt(), 0xFF43A88C.toInt())
        val paused = intArrayOf(0xFF5D5A66.toInt(), 0xFF7E7A88.toInt())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        palette = Palette(dark)
        actionBar?.hide()
        window.statusBarColor = palette.background
        window.navigationBarColor = palette.background
        if (!dark) window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(32))
        }

        content.addView(text("Reels Blocker", 30f, palette.text, bold = true))
        content.addView(text("Instagram without the endless scroll", 15f, palette.subtle).withMargins(bottom = 20))

        hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(26), dp(24), dp(26))
        }
        hero.addView(text("REELS", 13f, 0xCCFFFFFF.toInt(), bold = true).apply { letterSpacing = 0.15f })
        heroTitle = text("", 34f, Color.WHITE, bold = true).also { hero.addView(it) }
        heroSubtitle = text("", 16f, 0xE6FFFFFF.toInt()).also { hero.addView(it) }
        content.addView(hero.withMargins(bottom = 16))

        setupCard = buildSetupCard().also { content.addView(it.withMargins(bottom = 16)) }

        content.addView(card {
            addView(sectionTitle("Blocking"))
            addView(toggle("Blocking enabled", "Master switch for the schedule", settings.enabled) {
                settings.enabled = it; refresh()
            })
            addView(toggle("Block all the time", "Ignore the schedule and always block Reels", settings.alwaysBlock) {
                settings.alwaysBlock = it; refresh()
            })
        }.withMargins(bottom = 16))

        content.addView(card {
            addView(sectionTitle("Schedule"))
            addView(text("Reels are blocked during these times. Feed, Stories and DMs keep working.", 14f, palette.subtle)
                .withMargins(bottom = 12))
            windowList = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            addView(windowList)
            addView(pillButton("+  Add block time", palette.accent, Color.WHITE) { editWindow(null) }.withMargins(top = 8))
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(palette.background)
            addView(content)
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        setupCard.visibility = if (isServiceEnabled()) View.GONE else View.VISIBLE

        val active = settings.activeWindow()
        val (title, subtitle, gradient) = when {
            !settings.enabled -> Triple("Allowed", "Blocking is paused", palette.paused)
            settings.alwaysBlock -> Triple("Blocked", "Blocking all the time", palette.blocked)
            active != null -> Triple(
                "Blocked",
                if (active.startMinute == active.endMinute) "All day today" else "Until ${BlockWindow.formatTime(active.endMinute)}",
                palette.blocked,
            )
            settings.windows.isEmpty() -> Triple("Allowed", "Add a block time below to get started", palette.allowed)
            else -> Triple("Allowed", "Outside your block times", palette.allowed)
        }
        heroTitle.text = title
        heroSubtitle.text = subtitle
        hero.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, gradient).apply { cornerRadius = dp(28).toFloat() }

        windowList.removeAllViews()
        val windows = settings.windows.sortedBy { it.startMinute }
        if (windows.isEmpty()) {
            windowList.addView(text("No block times yet.", 15f, palette.subtle).withMargins(bottom = 8))
        }
        windows.forEach { w -> windowList.addView(windowRow(w, w == active).withMargins(bottom = 10)) }
    }

    private fun buildSetupCard(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(18), dp(20), dp(18))
        background = rounded(palette.warning, 24)
        addView(text("Finish setup", 18f, palette.warningText, bold = true))
        addView(text(
            "Turn on Reels Blocker in Accessibility settings (under Downloaded or Installed apps) so it can see when Reels opens.",
            14f, palette.warningText,
        ).withMargins(top = 4, bottom = 12))
        addView(pillButton("Open accessibility settings", palette.warningText, palette.warning) {
            startActivity(Intent(ACTION_ACCESSIBILITY_SETTINGS))
        })
        addView(text(
            "Says \"Restricted setting\"? Open app info, tap ⋮ in the top corner, choose \"Allow restricted settings\", then try again.",
            13f, palette.warningText,
        ).withMargins(top = 12, bottom = 8))
        addView(pillButton("Open app info", 0x00000000, palette.warningText, outline = palette.warningText) {
            startActivity(Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        })
    }

    private fun windowRow(w: BlockWindow, isActive: Boolean): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(18), dp(14), dp(8), dp(14))
        background = rounded(palette.row, 20).apply { if (isActive) setStroke(dp(2), palette.accent) }
        isClickable = true
        setOnClickListener { editWindow(w) }

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(w.describeTime(), 22f, palette.text, bold = true))
            addView(text(if (isActive) "${w.describeDays()}  •  Active now" else w.describeDays(), 14f,
                if (isActive) palette.accent else palette.subtle))
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        addView(TextView(context).apply {
            text = "✕"
            textSize = 18f
            setTextColor(palette.subtle)
            gravity = Gravity.CENTER
            contentDescription = "Delete ${w.describeTime()}"
            setOnClickListener {
                settings.windows = settings.windows.filter { it.id != w.id }
                refresh()
            }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
    }

    private fun editWindow(existing: BlockWindow?) {
        val start = existing?.startMinute ?: 9 * 60
        val end = existing?.endMinute ?: 17 * 60
        pickTime("Block from", start) { newStart ->
            pickTime("Block until", end) { newEnd ->
                pickDays(existing?.days ?: 0b1111111) { days ->
                    val w = BlockWindow(existing?.id ?: System.currentTimeMillis(), newStart, newEnd, days)
                    settings.windows = settings.windows.filter { it.id != w.id } + w
                    refresh()
                }
            }
        }
    }

    private fun pickTime(title: String, initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60, true).apply {
            setTitle(title)
            show()
        }
    }

    private fun pickDays(initial: Int, onPicked: (Int) -> Unit) {
        val checked = BooleanArray(7) { initial and (1 shl it) != 0 }
        AlertDialog.Builder(this)
            .setTitle("On these days")
            .setMultiChoiceItems(BlockWindow.DAY_NAMES, checked) { _, i, isChecked -> checked[i] = isChecked }
            .setPositiveButton("Save") { _, _ ->
                val mask = checked.foldIndexed(0) { i, acc, c -> if (c) acc or (1 shl i) else acc }
                if (mask != 0) onPicked(mask)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Secure.getString(contentResolver, Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(this, ReelsBlockerService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun card(build: LinearLayout.() -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(18), dp(20), dp(18))
        background = rounded(palette.card, 24)
        elevation = dp(1).toFloat()
        build()
    }

    private fun sectionTitle(label: String) = text(label, 19f, palette.text, bold = true).withMargins(bottom = 6)

    private fun toggle(label: String, hint: String, initial: Boolean, onChange: (Boolean) -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            val switch = Switch(context).apply {
                isChecked = initial
                setOnCheckedChangeListener { _, v -> onChange(v) }
            }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(label, 16f, palette.text))
                addView(text(hint, 13f, palette.subtle))
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(switch)
            setOnClickListener { switch.toggle() }
        }

    private fun pillButton(label: String, fill: Int, textColor: Int, outline: Int? = null, onClick: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(textColor)
            stateListAnimator = null
            minHeight = dp(52)
            background = rounded(fill, 26).apply { outline?.let { setStroke(dp(1), it) } }
            setOnClickListener { onClick() }
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun <T : View> T.withMargins(top: Int = 0, bottom: Int = 0): T = apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
