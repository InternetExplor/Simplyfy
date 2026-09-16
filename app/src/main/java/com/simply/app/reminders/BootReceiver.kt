package com.simply.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simply.app.data.Repository

/** После перезагрузки будильники стираются — расставляем их заново. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val data = Repository.get(context).data.value
        // Даже с выключенными напоминаниями зовём планировщик: он подчистит
        // список будильников, который после перезагрузки всё равно пуст.
        ReminderScheduler.reschedule(context, data)
    }
}
