package com.jacobstechnologies.birthdayreminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.ContactsContract
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun setTimer(context : Context, reminder : Reminder) {
    if (reminder.on == 0) {
        return
    }
    val alarmManager = context.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
    val myIntent =
        Intent(context, DailyReceiver::class.java).putExtra("name", reminder.contactInfo.name)
            .putExtra("daysUntilBirthday", reminder.daysBeforeToRemind)
    val alarmId = reminder.contactInfo.birthday.time.toInt()
    val alarmNotSet =
        PendingIntent.getBroadcast(context, alarmId, myIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) == null
    val nextRemindCalendar = Calendar.getInstance()
    nextRemindCalendar.time = getReminderTime(reminder, context)
    nextRemindCalendar[Calendar.HOUR_OF_DAY] =
        getData(SharedPreferencesConstants.NOTIFICATION_TIME_HOUR, SharedPreferencesConstants.DEFAULT_NOTIFICATION_HOUR, context) as Int
    nextRemindCalendar[Calendar.MINUTE] =
        getData(SharedPreferencesConstants.NOTIFICATION_TIME_MINUTE, SharedPreferencesConstants.DEFAULT_NOTIFICATION_MINUTE, context) as Int
    nextRemindCalendar[Calendar.SECOND] = 0
    val pendingIntent = PendingIntent.getBroadcast(
        context, alarmId, myIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    if (Build.VERSION.SDK_INT < 34 || alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextRemindCalendar.timeInMillis, pendingIntent)
        return
    }

    alarmManager.set(AlarmManager.RTC_WAKEUP, nextRemindCalendar.timeInMillis, pendingIntent)

}

fun getReminderTime(reminder : Reminder, context : Context): Date{
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = reminder.contactInfo.birthday.time
    calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
    calendar.add(Calendar.DAY_OF_YEAR, -reminder.daysBeforeToRemind)
    calendar[Calendar.HOUR_OF_DAY] = getData(SharedPreferencesConstants.NOTIFICATION_TIME_HOUR, SharedPreferencesConstants.DEFAULT_NOTIFICATION_HOUR, context) as Int
    calendar[Calendar.MINUTE] = getData(SharedPreferencesConstants.NOTIFICATION_TIME_MINUTE, SharedPreferencesConstants.DEFAULT_NOTIFICATION_MINUTE, context) as Int
    if (calendar.timeInMillis < System.currentTimeMillis()){ // reminder time set in past ---> add one year
        calendar.add(Calendar.YEAR, 1)
    }
    return calendar.time
}

@SuppressLint("Range")
fun loadContactsWithBirthdays(context: Context) : ArrayList<ContactInfo>{
    val contacts = ArrayList<ContactInfo>()
    val resolver = context.contentResolver ?: return contacts

    // set up cursor
    val uri = ContactsContract.Data.CONTENT_URI
    val collectedDataTypes = arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Event.CONTACT_ID, ContactsContract.CommonDataKinds.Event.START_DATE)
    val criteria = ContactsContract.Data.MIMETYPE + "= ? AND " +
                    ContactsContract.CommonDataKinds.Event.TYPE + " = " +
                    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
    val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
    val cursor = resolver.query(uri, collectedDataTypes, criteria, selectionArgs, null) ?: return contacts

    if (cursor.count <= 0) {return contacts}
    while(cursor.moveToNext()){
        val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
        val birthday = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE))
        val contact = ContactInfo(name = name, birthday = parseDate(birthday))
        contacts.add(contact)
    }
    cursor.close()
    return contacts
}

fun parseDate(date: String): Date {
    val formats = arrayOf("--MM-dd", "yyyy-MM-dd")
    formats.forEach { format ->
        try{
            return (SimpleDateFormat(format, Locale.getDefault()).parse(date) as Date)
        } catch (e: ParseException){
            Log.v("Date Parsing Error", "Format $format was invalid -> attempted string: $date")
        }
    }
    Log.v("Date Parsing Error", "All preset formats invalid! -> attempted string: $date")
    return Date()
}

fun checkIfAllContactsRepresentedByReminders(contacts: Array<ContactInfo>, reminders: Array<Reminder>): ArrayList<ContactInfo>{
    val reminderContacts = reminders.map { it.contactInfo }
    val newContacts = ArrayList<ContactInfo>()
    contacts.forEach{
        if (!reminderContacts.contains(it)){
            newContacts.add(it)
        }
    }
    return newContacts
}

fun vibrate(context : Context){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        if (vibratorManager.defaultVibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)) {
            vibratorManager.vibrate(
                CombinedVibration.createParallel(
                    VibrationEffect
                        .startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
                        .compose()
                )
            )
        }
        else{
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(
                    50L,
                    VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    else{
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(500, 200))
    }
}