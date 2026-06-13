package com.nuclearboy.common

import android.content.Context

/**
 * 轻量应用设置存储（SharedPreferences）。目前承载"自定义指令"——
 * 用户可写入自己的人设/规则，追加进系统提示，让核弹男孩按个人偏好回复。
 */
class AppSettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("nuclearboy_app_settings", Context.MODE_PRIVATE)

    /** 自定义指令（追加到系统提示）。 */
    fun customInstructions(): String = prefs.getString(KEY_CUSTOM_INSTRUCTIONS, "") ?: ""

    fun setCustomInstructions(text: String) {
        prefs.edit().putString(KEY_CUSTOM_INSTRUCTIONS, text.trim()).apply()
    }

    private companion object {
        const val KEY_CUSTOM_INSTRUCTIONS = "custom_instructions"
    }
}
