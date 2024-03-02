package com.jacobstechnologies.birthdayreminders

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Locale

class RecyclerViewAdapter(private val dataAccess : DataAccess, private val context : Context): RecyclerView.Adapter<RecyclerViewAdapter.Holder>(){

    private var reminders = ArrayList<Reminder>()
    private val birthdayFormatter: SimpleDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) : Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.reminder_item, parent, false)
        return Holder(view)
    }

    override fun getItemCount() : Int {
        return reminders.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder : Holder, position : Int) {
        val reminder = reminders[position]
        holder.name.text = reminder.contactInfo.name
        holder.birthday.text = birthdayFormatter.format(reminder.contactInfo.birthday.time)
        holder.reminder.text = birthdayFormatter.format(getReminderTime(reminder, context))
        holder.daysBeforeReminderEditText.setText(reminder.daysBeforeToRemind.toString())
        holder.switch.isChecked = reminders[position].on==1
        holder.switch.setOnClickListener {
            vibrate(context)
            reminders[position].on = if (holder.switch.isChecked) 1 else 0
            dataAccess.updateReminder(reminders[position])
        }
        holder.doneButton.setOnClickListener {
            if (holder.daysBeforeReminderEditText.text.toString() != ""){
                reminders[position].daysBeforeToRemind = holder.daysBeforeReminderEditText.text.toString().toInt()
                dataAccess.updateReminder(reminders[position])
                notifyItemChanged(position)
            }
            holder.editLinearLayout.visibility = View.VISIBLE
        }
        holder.editLinearLayout.visibility = View.GONE
        holder.itemView.setOnLongClickListener {
            if (holder.editLinearLayout.visibility == View.VISIBLE){
                holder.editLinearLayout.visibility = View.GONE
            }
            else{
                holder.editLinearLayout.visibility = View.VISIBLE
            }
            true
        }
        holder.deleteButton.setOnClickListener {
            vibrate(context)
            dataAccess.deleteReminder(reminder)
            reminders.remove(reminder)
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun sortList(sortType: SortTypes){
        when (sortType) {
            SortTypes.BIRTHDAY_YTD -> reminders.sortBy { it.contactInfo.birthday }
            SortTypes.BIRTHDAY_ONWARD -> reminders.sortBy { it.contactInfo.birthday  }
            SortTypes.REMINDER_ONWARD -> reminders.sortBy { it.daysBeforeToRemind }
            SortTypes.NAME -> reminders.sortBy { it.contactInfo.name }
        }
        notifyDataSetChanged()
    }

    fun returnReminders() : ArrayList<Reminder> {
        return reminders
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setChecked(on: Int){
        for (reminder in reminders) {
            reminder.on = on
            dataAccess.updateReminder(reminder)
        }
        notifyDataSetChanged()
    }

    fun addReminder(reminder : Reminder){
        if (!reminders.contains(reminder)){
            reminders.add(reminder)
            notifyItemChanged(reminders.size - 1)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filteredList: ArrayList<Reminder>){
        reminders = filteredList
        notifyDataSetChanged()
    }

    inner class Holder(v: View) : RecyclerView.ViewHolder(v){
        val name: TextView = v.findViewById(R.id.reminderListItemNameTextView)
        val birthday: TextView = v.findViewById(R.id.reminderListItemBirthdayTextView)
        val reminder: TextView = v.findViewById(R.id.reminderListItemReminderDateTextView)
        val daysBeforeReminderEditText: TextInputEditText = v.findViewById(R.id.reminderListItemDaysBeforeTextView)
        val doneButton: Button = v.findViewById(R.id.reminderListItemDoneButton)
        val editLinearLayout: LinearLayout = v.findViewById(R.id.reminderListItemEditLinearLayout)
        val switch: SwitchMaterial = v.findViewById(R.id.reminderListItemSwitch)
        val deleteButton: Button = v.findViewById(R.id.reminderListItemDeleteButton)
    }

    enum class SortTypes{
        NAME, BIRTHDAY_YTD, BIRTHDAY_ONWARD, REMINDER_ONWARD
    }
}
