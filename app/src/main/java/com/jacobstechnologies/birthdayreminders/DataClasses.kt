package com.jacobstechnologies.birthdayreminders

import android.content.Context
import java.util.Date
import java.util.UUID

data class ContactInfo(val name: String, val birthday: Date)
data class Reminder(val contactInfo: ContactInfo, var daysBeforeToRemind: Int, var on: Int, val id: String)

fun createReminder(context: Context, contactInfo : ContactInfo, daysBefore : Int = 14): Reminder{
    val daysBeforeToRemind = getData(SharedPreferencesConstants.DAYS_BEFORE_BIRTHDAY_TO_NOFITY, daysBefore, context).toString().toInt()
    return Reminder(contactInfo, daysBeforeToRemind, 1, UUID.randomUUID().toString())
}