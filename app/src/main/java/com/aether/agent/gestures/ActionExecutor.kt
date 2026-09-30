package com.aether.agent.gestures

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.MediaStore
import android.view.KeyEvent
import android.widget.Toast
import com.aether.agent.MainActivity
import com.aether.agent.service.AetherAccessibilityService
import com.aether.agent.service.CursorTriggerService
import com.aether.agent.service.IslandService

object ActionExecutor {

    @Volatile private var torchOn = false

    fun run(context: Context, action: NotchAction) {
        if (action == NotchAction.NONE) return
        try {
            when (action) {
                NotchAction.NONE -> {}
                NotchAction.OPEN_AETHER -> {
                    context.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                NotchAction.OPEN_CHAT -> {
                    context.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra("open", "chat")
                    )
                }
                NotchAction.SCREENSHOT -> {
                    // Best-effort: open system screenshot if possible
                    Toast.makeText(context, "Screenshot: use system gesture or Accessibility", Toast.LENGTH_SHORT).show()
                    AetherAccessibilityService.instance?.let {
                        // Global screenshot action exists API 28+
                        if (Build.VERSION.SDK_INT >= 28) {
                            it.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                        }
                    }
                }
                NotchAction.FLASHLIGHT -> toggleTorch(context)
                NotchAction.POWER_MENU -> {
                    AetherAccessibilityService.instance?.globalPowerDialog()
                        ?: toast(context, "Enable Accessibility")
                }
                NotchAction.NOTIFICATIONS -> {
                    AetherAccessibilityService.instance?.globalNotifications()
                        ?: toast(context, "Enable Accessibility")
                }
                NotchAction.QUICK_SETTINGS -> {
                    val svc = AetherAccessibilityService.instance
                    if (svc != null && Build.VERSION.SDK_INT >= 29) {
                        svc.performGlobalAction(
                            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
                        )
                    } else toast(context, "Enable Accessibility")
                }
                NotchAction.RECENT_APPS -> {
                    AetherAccessibilityService.instance?.globalRecents()
                        ?: toast(context, "Enable Accessibility")
                }
                NotchAction.HOME -> {
                    AetherAccessibilityService.instance?.globalHome()
                        ?: toast(context, "Enable Accessibility")
                }
                NotchAction.BACK -> {
                    AetherAccessibilityService.instance?.globalBack()
                        ?: toast(context, "Enable Accessibility")
                }
                NotchAction.LOCK_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= 28) {
                        AetherAccessibilityService.instance?.performGlobalAction(
                            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
                        ) ?: toast(context, "Enable Accessibility")
                    } else toast(context, "Lock needs Android 9+")
                }
                NotchAction.VOLUME_UP -> volume(context, true)
                NotchAction.VOLUME_DOWN -> volume(context, false)
                NotchAction.BRIGHTNESS_UP -> brightness(context, +30)
                NotchAction.BRIGHTNESS_DOWN -> brightness(context, -30)
                NotchAction.MEDIA_PLAY_PAUSE -> media(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                NotchAction.MEDIA_NEXT -> media(context, KeyEvent.KEYCODE_MEDIA_NEXT)
                NotchAction.MEDIA_PREV -> media(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                NotchAction.OPEN_CAMERA -> {
                    context.startActivity(
                        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                NotchAction.TOGGLE_CURSOR -> {
                    CursorTriggerService.start(context)
                }
            }
        } catch (e: Exception) {
            toast(context, "Action failed: ${e.message}")
        }
    }

    private fun toast(c: Context, m: String) =
        Toast.makeText(c, m, Toast.LENGTH_SHORT).show()

    private fun toggleTorch(context: Context) {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull() ?: return
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
            toast(context, if (torchOn) "Flashlight ON" else "Flashlight OFF")
        } catch (e: Exception) {
            toast(context, "Flashlight unavailable")
        }
    }

    private fun volume(context: Context, up: Boolean) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun brightness(context: Context, delta: Int) {
        try {
            val cr = context.contentResolver
            val cur = android.provider.Settings.System.getInt(
                cr, android.provider.Settings.System.SCREEN_BRIGHTNESS, 128
            )
            val next = (cur + delta).coerceIn(1, 255)
            android.provider.Settings.System.putInt(
                cr, android.provider.Settings.System.SCREEN_BRIGHTNESS, next
            )
            toast(context, "Brightness $next")
        } catch (_: Exception) {
            toast(context, "Allow modify system settings for brightness")
        }
    }

    private fun media(context: Context, keyCode: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        am.dispatchMediaKeyEvent(eventDown)
        am.dispatchMediaKeyEvent(eventUp)
    }
}
