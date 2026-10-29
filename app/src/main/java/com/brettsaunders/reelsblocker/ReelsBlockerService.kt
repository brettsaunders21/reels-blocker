package com.brettsaunders.reelsblocker

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class ReelsBlockerService : AccessibilityService() {

    private lateinit var settings: Settings
    private var lastCheck = 0L
    private var lastAction = 0L
    private var lastToast = 0L

    override fun onServiceConnected() {
        settings = Settings(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName?.toString() != INSTAGRAM) return
        val now = SystemClock.uptimeMillis()
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && now - lastCheck < CHECK_INTERVAL_MS) return
        lastCheck = now
        if (now - lastAction < ACTION_COOLDOWN_MS) return
        if (!settings.isBlockingNow()) return

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != INSTAGRAM) return
        if (!isShowingReels(root)) return

        // A second hit shortly after pressing back means back didn't leave Reels
        // (e.g. Reels tab is the root), so switch to the Home tab instead of backing out of Instagram.
        val recentlyPressedBack = now - lastAction < RETRY_WINDOW_MS
        val homeTab = if (recentlyPressedBack) findVisible(root, HOME_TAB_IDS) else null
        if (homeTab != null && homeTab.isClickable) {
            homeTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        lastAction = now

        if (now - lastToast > TOAST_INTERVAL_MS) {
            lastToast = now
            Toast.makeText(this, "Reels are blocked right now", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isShowingReels(root: AccessibilityNodeInfo): Boolean {
        if (findVisible(root, REELS_VIEWER_IDS) != null) return true
        val reelsTab = findVisible(root, REELS_TAB_IDS)
        if (reelsTab != null) return reelsTab.isSelected
        return root.findAccessibilityNodeInfosByText(REELS_LABEL).any {
            it.isSelected && it.isVisibleToUser && it.contentDescription?.toString() == REELS_LABEL
        }
    }

    private fun findVisible(root: AccessibilityNodeInfo, ids: List<String>): AccessibilityNodeInfo? {
        for (id in ids) {
            val match = root.findAccessibilityNodeInfosByViewId("$INSTAGRAM:id/$id")
                .firstOrNull { it.isVisibleToUser }
            if (match != null) return match
        }
        return null
    }

    override fun onInterrupt() {}

    companion object {
        const val INSTAGRAM = "com.instagram.android"
        private const val CHECK_INTERVAL_MS = 250L
        private const val ACTION_COOLDOWN_MS = 700L
        private const val RETRY_WINDOW_MS = 2500L
        private const val TOAST_INTERVAL_MS = 4000L

        val REELS_VIEWER_IDS = listOf("clips_viewer_view_pager", "clips_viewer_container", "clips_swipe_refresh_container")
        val REELS_TAB_IDS = listOf("clips_tab")
        val HOME_TAB_IDS = listOf("feed_tab")
        const val REELS_LABEL = "Reels"
    }
}
