package com.aether.agent.gestures

/**
 * All actions the user can assign to a notch / island gesture.
 */
enum class NotchAction(val id: String, val label: String) {
    NONE("none", "None"),
    OPEN_AETHER("open_aether", "Open Aether"),
    OPEN_CHAT("open_chat", "Open AI Chat"),
    SCREENSHOT("screenshot", "Screenshot"),
    FLASHLIGHT("flashlight", "Toggle flashlight"),
    POWER_MENU("power_menu", "Power menu"),
    NOTIFICATIONS("notifications", "Notification shade"),
    QUICK_SETTINGS("quick_settings", "Quick settings"),
    RECENT_APPS("recent_apps", "Recent apps"),
    HOME("home", "Home"),
    BACK("back", "Back"),
    LOCK_SCREEN("lock", "Lock screen"),
    VOLUME_UP("vol_up", "Volume up"),
    VOLUME_DOWN("vol_down", "Volume down"),
    BRIGHTNESS_UP("bri_up", "Brightness up"),
    BRIGHTNESS_DOWN("bri_down", "Brightness down"),
    MEDIA_PLAY_PAUSE("media_pp", "Play / Pause"),
    MEDIA_NEXT("media_next", "Next track"),
    MEDIA_PREV("media_prev", "Previous track"),
    OPEN_CAMERA("camera", "Open camera"),
    TOGGLE_CURSOR("cursor", "Toggle Quick Cursor");

    companion object {
        fun fromId(id: String): NotchAction =
            entries.find { it.id == id } ?: NONE
    }
}

enum class NotchGesture(val id: String, val label: String) {
    SINGLE_TAP("single", "Single tap"),
    DOUBLE_TAP("double", "Double tap"),
    LONG_PRESS("long", "Long press"),
    SWIPE_LEFT("left", "Swipe left"),
    SWIPE_RIGHT("right", "Swipe right"),
    SWIPE_DOWN("down", "Swipe down"),
    SWIPE_UP("up", "Swipe up");
}
