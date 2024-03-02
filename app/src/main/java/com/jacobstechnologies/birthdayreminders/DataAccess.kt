package com.jacobstechnologies.birthdayreminders

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Date

class DataAccess(val context : Context) {
    private lateinit var db : SQLiteDatabase
    private var customSQLiteHelper: CustomSQLiteHelper = CustomSQLiteHelper(context)

    fun saveReminder(reminder : Reminder) {
        val db = customSQLiteHelper.writableDatabase
        val query = "INSERT INTO $DB_TABLE_NAME ($TABLE_ROW_REMINDER_ID, $TABLE_ROW_NAME, $TABLE_ROW_BIRTHDAY, $TABLE_ROW_REMINDER, $TABLE_ROW_ON) VALUES (?, ?, ?, ?, ?);"
        val values = arrayOf(
            reminder.id,
            reminder.contactInfo.name,
            reminder.contactInfo.birthday.time,
            reminder.daysBeforeToRemind,
            reminder.on.toString()
        )

        db.execSQL(query, values)
        db.close()
    }

    fun clearReminders(){
        val db = customSQLiteHelper.writableDatabase
        val query = "DELETE FROM $DB_TABLE_NAME"
        db.execSQL(query)
        db.close()
    }

    fun updateReminder(reminder: Reminder){
        db = customSQLiteHelper.writableDatabase
        val values = ContentValues()
        values.put(TABLE_ROW_REMINDER_ID, reminder.id)
        values.put(TABLE_ROW_NAME, reminder.contactInfo.name)
        values.put(TABLE_ROW_BIRTHDAY, reminder.contactInfo.birthday.time)
        values.put(TABLE_ROW_REMINDER, reminder.daysBeforeToRemind)
        values.put(TABLE_ROW_ON, reminder.on)
        db.update(DB_TABLE_NAME, values, "$TABLE_ROW_REMINDER_ID=?", arrayOf(reminder.id))
        db.close()
    }

    fun retrieveReminders() : ArrayList<Reminder>{
        db = customSQLiteHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * from $DB_TABLE_NAME", null)
        val reminders = ArrayList<Reminder>()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(TABLE_ROW_REMINDER_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(TABLE_ROW_NAME))
                val birthdayDate = Date(cursor.getString(cursor.getColumnIndexOrThrow(TABLE_ROW_BIRTHDAY)).toLong())
                val reminderDateInt = cursor.getInt(cursor.getColumnIndexOrThrow(TABLE_ROW_REMINDER))
                val on = cursor.getInt(cursor.getColumnIndexOrThrow(TABLE_ROW_ON))
                val contactInfo = ContactInfo(name, birthdayDate)
                reminders.add(Reminder(contactInfo, reminderDateInt, on, id))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return reminders
    }

    fun deleteReminder(reminder : Reminder){
        db = customSQLiteHelper.writableDatabase
        db.delete(DB_TABLE_NAME, "$TABLE_ROW_REMINDER_ID=?", arrayOf(reminder.id))
        db.close()
    }

    private class CustomSQLiteHelper constructor(context : Context?) : SQLiteOpenHelper(context, DB_TABLE_NAME, null, DB_VERSION) {
        override fun onCreate(db : SQLiteDatabase) {
            val  newRemindersTable = ("create table "
                    + DB_TABLE_NAME + " ("
                    + TABLE_ROW_ID
                    + " integer primary key autoincrement not null,"
                    + TABLE_ROW_REMINDER_ID
                    + " text not null,"
                    + TABLE_ROW_NAME
                    + " text not null,"
                    + TABLE_ROW_BIRTHDAY
                    + " integer not null,"
                    + TABLE_ROW_REMINDER
                    + " integer not null,"
                    + TABLE_ROW_ON
                    + " integer not null);"
                    )
            db.execSQL(newRemindersTable)
        }

        override fun onUpgrade(db : SQLiteDatabase, oldVersion : Int, newVersion : Int) {

        }
    }

    companion object {
        private const val DB_VERSION = 1
        const val DB_TABLE_NAME = "reminder_table"
        const val TABLE_ROW_ID = "reminder_id"
        const val TABLE_ROW_REMINDER_ID = "reminder_unique_id"
        const val TABLE_ROW_NAME = "reminder_contact_name"
        const val TABLE_ROW_BIRTHDAY = "reminder_birthday"
        const val TABLE_ROW_REMINDER = "reminder_date"
        const val TABLE_ROW_ON = "reminder_on"
    }
}