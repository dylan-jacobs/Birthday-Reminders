package com.jacobstechnologies.birthdayreminders

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        var notificationTimeHour = getData(SharedPreferencesConstants.NOTIFICATION_TIME_HOUR, SharedPreferencesConstants.DEFAULT_NOTIFICATION_HOUR, applicationContext) as Int
        var notificationTimeMinute = getData(SharedPreferencesConstants.NOTIFICATION_TIME_MINUTE, SharedPreferencesConstants.DEFAULT_NOTIFICATION_MINUTE, applicationContext) as Int

        val toolbar: MaterialToolbar = findViewById(R.id.settingsToolbar)
        val daysBeforeToNotifyTextView: TextInputEditText = findViewById(R.id.daysBeforeTextView)
        val notificationTimeTextView: TextView = findViewById(R.id.settingsNotificationTimeTextView)
        val editNotificationTimeButton: Button = findViewById(R.id.settingsEditTimeButton)
        val doneButton: Button = findViewById(R.id.settingsDoneButton)

        // textviews
        daysBeforeToNotifyTextView.setText(getData(SharedPreferencesConstants.DAYS_BEFORE_BIRTHDAY_TO_NOFITY, SharedPreferencesConstants.DEFAULT_DAYS_BEFORE_BIRTHDAY_VALUE, applicationContext).toString())
        notificationTimeTextView.text = setTimeText(notificationTimeHour, notificationTimeMinute)

        val dialogListener: TimePickerDialog.OnTimeSetListener =
            TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                saveData(SharedPreferencesConstants.NOTIFICATION_TIME_HOUR, hourOfDay, applicationContext)
                saveData(SharedPreferencesConstants.NOTIFICATION_TIME_MINUTE, minute, applicationContext)
                notificationTimeHour = hourOfDay
                notificationTimeMinute = minute
                notificationTimeTextView.text = setTimeText(notificationTimeHour, notificationTimeMinute)
            }
        editNotificationTimeButton.setOnClickListener {
            val dialog = TimePickerDialog(
                this,
                dialogListener,
                notificationTimeHour,
                notificationTimeMinute,
                false
            )
            dialog.show()
        }
        doneButton.setOnClickListener {
            if (daysBeforeToNotifyTextView.text.toString() != "") {

                val bottomSheetDialog = BottomSheetDialog(this)
                val bottomSheetDialogView = layoutInflater.inflate(R.layout.new_default_days_before_bottom_dialog, null)

                val updateAll: Button = bottomSheetDialogView.findViewById(R.id.updateDefaultDialogUpdateAllButton)
                val updateExisting: Button = bottomSheetDialogView.findViewById(R.id.updateDefaultDialogUpdateExistingButton)
                val updateFuture: Button = bottomSheetDialogView.findViewById(R.id.updateDefaultDialogUpdateFutureButton)

                val dataAccess = DataAccess(applicationContext)

                updateAll.setOnClickListener {
                    saveData(SharedPreferencesConstants.DAYS_BEFORE_BIRTHDAY_TO_NOFITY, daysBeforeToNotifyTextView.text.toString(), applicationContext)

                    val reminders = dataAccess.retrieveReminders()
                    reminders.forEach {
                        it.daysBeforeToRemind = daysBeforeToNotifyTextView.text.toString().toInt()
                        dataAccess.updateReminder(it)
                    }
                    bottomSheetDialog.dismiss()
                }
                updateExisting.setOnClickListener {
                    val previousDaysBeforeDefaultValue = getData(SharedPreferencesConstants.DAYS_BEFORE_BIRTHDAY_TO_NOFITY, SharedPreferencesConstants.DEFAULT_DAYS_BEFORE_BIRTHDAY_VALUE, applicationContext).toString().toInt()
                    saveData(SharedPreferencesConstants.DAYS_BEFORE_BIRTHDAY_TO_NOFITY, daysBeforeToNotifyTextView.text.toString(), applicationContext)
                    val reminders = dataAccess.retrieveReminders()
                    reminders.forEach {
                        if (it.daysBeforeToRemind == previousDaysBeforeDefaultValue){ // only update non-modified default reminders
                            it.daysBeforeToRemind = daysBeforeToNotifyTextView.text.toString().toInt()
                            dataAccess.updateReminder(it)
                        }
                    }
                    bottomSheetDialog.dismiss()
                }
                updateFuture.setOnClickListener {
                    saveData(SharedPreferencesConstants.DAYS_BEFORE_BIRTHDAY_TO_NOFITY, daysBeforeToNotifyTextView.text.toString(), applicationContext)
                    bottomSheetDialog.dismiss()
                }

                bottomSheetDialog.setContentView(bottomSheetDialogView)
                bottomSheetDialog.show()
            }
        }

        toolbar.setNavigationOnClickListener {
            it.contentDescription = getString(R.string.back)
            onBackPressedDispatcher.onBackPressed()
        }

    }

    private fun setTimeText(hourVal: Int, minuteVal: Int) : String{
        var hour = hourVal
        var amOrPm = "AM"
        if (hour > 12){
            amOrPm = "PM"
            hour -= 12
        }
        else if (hour == 12){
            amOrPm = "PM"
            hour = 12
        }
        val minute =
            if (minuteVal < 10){ "0${minuteVal}" } else {minuteVal}
        return getString(R.string.notification_time, hour, minute, amOrPm)
    }
}