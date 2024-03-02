package com.jacobstechnologies.birthdayreminders

import android.content.Context

fun saveData(key: String, value: Any, context : Context){
    val preferences = context.getSharedPreferences(SharedPreferencesConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    val editor = preferences.edit()
    when(value){
        is Boolean -> editor.putBoolean(key, value)
        is String -> editor.putString(key, value)
        is Int -> editor.putInt(key, value)
        is Long -> editor.putLong(key, value)
        is Float -> editor.putFloat(key, value)
    }
    editor.apply()
}

fun getData(key : String, default: Any, context : Context) : Any {
    val preferences =
        context.getSharedPreferences(SharedPreferencesConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    return preferences.all[key] ?: default
}

object SharedPreferencesConstants {
    const val SHARED_PREFERENCES_NAME = "shared_prefs"
    const val DAYS_BEFORE_BIRTHDAY_TO_NOFITY = "days_before_to_notify"
    const val NOTIFICATION_TIME_HOUR = "notification_time_hour"
    const val NOTIFICATION_TIME_MINUTE = "notification_time_minute"
    const val SORT_TYPE = "sort_type"

    const val DEFAULT_DAYS_BEFORE_BIRTHDAY_VALUE = 14
    const val DEFAULT_NOTIFICATION_HOUR = 9
    const val DEFAULT_NOTIFICATION_MINUTE = 0
    const val DEFAULT_SORT_TYPE = 0
}