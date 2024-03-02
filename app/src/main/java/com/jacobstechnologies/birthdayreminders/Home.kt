package com.jacobstechnologies.birthdayreminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jacobstechnologies.birthdayreminders.databinding.FragmentFirstBinding
import java.text.SimpleDateFormat
import java.util.Locale

class Home : Fragment() {

    private var _binding : FragmentFirstBinding? = null
    private lateinit var dataAccess: DataAccess
    private lateinit var recyclerViewAdapter : RecyclerViewAdapter
    private lateinit var grantPermissionsButton: Button
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var contacts: ArrayList<ContactInfo>
    private var reminderList = ArrayList<Reminder>()

    private val binding get() = _binding!!
    private val permissions = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_CONTACTS) else arrayOf(Manifest.permission.READ_CONTACTS)
    private val notificationRequestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) outer@{ permissionsList ->
            permissionsList.forEach {
                if (!it.value){
                    showSettingsDialog()
                    return@outer
                }
                else{
                    swipeRefreshLayout.visibility = View.VISIBLE
                    grantPermissionsButton.visibility = View.GONE
                    refreshLayout(true)
                }
            }
        }

    override fun onCreateView(
        inflater : LayoutInflater, container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        dataAccess = DataAccess(requireContext())
        recyclerViewAdapter = RecyclerViewAdapter(dataAccess, requireContext())
        contacts = ArrayList()

        return binding.root

    }

    override fun onViewCreated(view : View, savedInstanceState : Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = binding.root.findViewById(R.id.firstFragmentRecyclerView)
        val searchView: SearchView = binding.root.findViewById(R.id.firstFragmentSearchView)
        val switch: SwitchMaterial = binding.root.findViewById(R.id.firstFragmentSelectAllSwitch)
        val sortButton: Button = binding.root.findViewById(R.id.firstFragmentSortButton)
        grantPermissionsButton = binding.root.findViewById(R.id.grantPermissionsButton)
        swipeRefreshLayout = binding.root.findViewById(R.id.swipeRefreshLayout)

        grantPermissionsButton.setOnClickListener { checkForPermissions() }
        grantPermissionsButton.visibility = View.GONE
        checkForPermissions()

        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = recyclerViewAdapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                context, layoutManager.orientation
            )
        )

        swipeRefreshLayout.setOnRefreshListener {
            refreshLayout(false)
            swipeRefreshLayout.isRefreshing = false
        }

        switch.isChecked = false
        recyclerViewAdapter.returnReminders().forEach {
            if (it.on == 1) {
                switch.isChecked = true
            }
        }
        switch.text = if (switch.isChecked) "Deselect All" else "Select All"
        switch.setOnClickListener {
            vibrate(requireContext())
            recyclerViewAdapter.setChecked(if (switch.isChecked) 1 else 0)
            switch.text = if (switch.isChecked) "Deselect All" else "Select All"
        }

        searchView.setOnQueryTextListener(
            object: SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query : String?) : Boolean {
                    return false
                }

                override fun onQueryTextChange(newText : String?) : Boolean {
                    if (newText != null) {
                        filterList(newText)
                    }
                    return false
                }

            }
        )

        sortButton.setOnClickListener {
            showSortDialog()
        }
    }


    private fun showSettingsDialog(){
        context?.let {
            MaterialAlertDialogBuilder(it, com.google.android.material.R.style.MaterialAlertDialog_Material3)
                .setTitle("Permissions Required")
                .setMessage("Contact and notification permissions are required. Please allow permissions from settings.")
                .setPositiveButton("Ok") {_, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${it.packageName}")
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showExactAlarmsPermissionDialog(){
        context?.let {
            MaterialAlertDialogBuilder(it, com.google.android.material.R.style.MaterialAlertDialog_Material3)
                .setTitle("Permissions Required")
                .setMessage("If you want to allow more precise alarms, please allow SCHEDULE_EXACT_ALARM permissions from settings.")
                .setPositiveButton("Ok") {_, _ ->
                    val intent = Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intent.data = Uri.parse("package:${it.packageName}")
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    @SuppressLint("InflateParams")
    private fun showSortDialog(){
        val sortDialog = BottomSheetDialog(requireContext())
        val sortDialogView = layoutInflater.inflate(R.layout.sort_reminders_dialog, null)
        sortDialog.setContentView(sortDialogView)
        val radioGroup: RadioGroup = sortDialogView.findViewById(R.id.sortDialogSortTypeRadioGroup)
        val doneButton: Button = sortDialogView.findViewById(R.id.sortDialogRadioButtonDoneButton)

        radioGroup.check(radioGroup.getChildAt(getData(SharedPreferencesConstants.SORT_TYPE, SharedPreferencesConstants.DEFAULT_SORT_TYPE, requireContext()) as Int).id)

        radioGroup.setOnCheckedChangeListener { _, _ ->
            saveData(SharedPreferencesConstants.SORT_TYPE, radioGroup.indexOfChild(sortDialogView.findViewById(radioGroup.checkedRadioButtonId)), requireContext())
        }

        doneButton.setOnClickListener {
            saveData(SharedPreferencesConstants.SORT_TYPE, radioGroup.indexOfChild(sortDialogView.findViewById(radioGroup.checkedRadioButtonId)), requireContext())
            refreshLayout(false)
            sortDialog.dismiss()
        }

        sortDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun filterList(message: String){
        val filteredList = ArrayList<Reminder>()
        val msg = message.lowercase()
        val birthdayFormatter = SimpleDateFormat("MMMM dd", Locale.getDefault())

        reminderList.forEach {
            if (it.contactInfo.name.lowercase().contains(msg) || birthdayFormatter.format(it.contactInfo.birthday).lowercase().contains(msg) || birthdayFormatter.format(it.contactInfo.birthday.time - (it.daysBeforeToRemind * 24 * 60 * 60 * 1000)).lowercase().contains(msg)){
                filteredList.add(it)
            }
        }
        if (filteredList.isEmpty()) {
            view?.let {
                Snackbar.make(it, "No results found", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
            }
        } else {
            recyclerViewAdapter.filterList(filteredList)
        }
    }

    private fun checkForPermissions(){
        if (context?.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            notificationRequestPermissionLauncher.launch(permissions)
            grantPermissionsButton.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.GONE
            return
        }
        grantPermissionsButton.visibility = View.GONE
        swipeRefreshLayout.visibility = View.VISIBLE
    }

    fun refreshLayout(syncWithContacts: Boolean){
        if (context?.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        contacts = loadContactsWithBirthdays(requireContext())
        val remindersDataList = dataAccess.retrieveReminders()

        if (syncWithContacts) {
            val missingContacts =
                checkIfAllContactsRepresentedByReminders(contacts.toTypedArray(), remindersDataList.toTypedArray())

            missingContacts.forEach { missingContact -> // contacts need to be represented by reminders
                val reminder = createReminder(requireContext(), missingContact)
                dataAccess.saveReminder(reminder)
                remindersDataList.add(reminder)
            }
        }

        val alarmManager = requireContext().getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 34 && !alarmManager.canScheduleExactAlarms()) {
            showExactAlarmsPermissionDialog()
        }

        remindersDataList.forEach {
            recyclerViewAdapter.addReminder(it)
            setTimer(requireContext(), it)
        }

        reminderList = remindersDataList
        recyclerViewAdapter.sortList(RecyclerViewAdapter.SortTypes.values()[getData(SharedPreferencesConstants.SORT_TYPE, SharedPreferencesConstants.DEFAULT_SORT_TYPE, requireContext()) as Int])
    }

    override fun onResume() {
        super.onResume()
        refreshLayout(false)
    }
}