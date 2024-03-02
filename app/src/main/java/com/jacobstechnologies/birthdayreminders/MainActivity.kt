package com.jacobstechnologies.birthdayreminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.jacobstechnologies.birthdayreminders.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration : AppBarConfiguration
    private lateinit var binding : ActivityMainBinding
    private val permissions = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_CONTACTS) else arrayOf(Manifest.permission.READ_CONTACTS)
    private val notificationRequestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissionsList ->
            permissionsList.forEach {
                if (!it.value){
                    showSettingsDialog()
                }
            }
        }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setup notification channels
        setupChannels()

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                notificationRequestPermissionLauncher.launch(permissions)
            }
            else{
                showCreateNewReminderDialog()
            }
        }
    }

    override fun onCreateOptionsMenu(menu : Menu) : Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                true
            }
            R.id.action_sync_with_contacts -> {
                val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                navHost?.let { navFragment ->
                    navFragment.childFragmentManager.primaryNavigationFragment?.let {
                        val home = it as Home
                        home.refreshLayout(true)
                    }
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp() : Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    @SuppressLint("InflateParams")
    private fun showCreateNewReminderDialog(){
        val newReminderDialog = BottomSheetDialog(this)
        val newReminderDialogView = layoutInflater.inflate(R.layout.new_reminder_layout, null)
        val dataAccess = DataAccess(applicationContext)
        val contactSpinner: Spinner = newReminderDialogView.findViewById(R.id.newReminderContactsSpinner)
        val daysBeforeTextView: TextInputEditText = newReminderDialogView.findViewById(R.id.newReminderDaysBeforeEditText)
        val doneButton: Button = newReminderDialogView.findViewById(R.id.newReminderDoneButton)

        val contacts = loadContactsWithBirthdays(this)
        val contactNames = ArrayList<String>()
        contacts.forEach { contactNames.add(it.name) }

        daysBeforeTextView.setText(getData(SharedPreferencesConstants.DAYS_BEFORE_BIRTHDAY_TO_NOFITY, SharedPreferencesConstants.DEFAULT_DAYS_BEFORE_BIRTHDAY_VALUE, applicationContext).toString())
        contactSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, contactNames)
        doneButton.setOnClickListener {
            if (contacts.isNotEmpty()) {
                try {
                    val reminder = createReminder(
                        this, contacts[contactSpinner.selectedItemPosition], daysBeforeTextView.text.toString()
                            .toInt()
                    )
                    dataAccess.saveReminder(reminder)
                    newReminderDialog.dismiss()
                } catch (_ : IndexOutOfBoundsException){
                }
            }
        }
        newReminderDialog
            .setContentView(newReminderDialogView)
        newReminderDialog.show()
    }

    private fun setupChannels() {
        val adminChannelName : CharSequence = "New notification"
        val adminChannelDescription = "Device to device notification"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val adminChannel = NotificationChannel("10000", adminChannelName, NotificationManager.IMPORTANCE_HIGH)
        adminChannel.description = adminChannelDescription
        adminChannel.enableLights(true)
        adminChannel.lightColor = Color.RED
        adminChannel.enableVibration(true)
        notificationManager.createNotificationChannel(adminChannel)
    }

    private fun showSettingsDialog(){
        MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_Material3)
            .setTitle("Permissions Required")
            .setMessage("Contact and notification permissions are required. Please allow permissions from settings.")
            .setPositiveButton("Ok") {_, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${packageName}")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}