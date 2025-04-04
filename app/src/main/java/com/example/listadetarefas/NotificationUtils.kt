package com.example.listadetarefas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object NotificationUtils {

    fun scheduleNotification(context: Context, title: String, description: String, time: String) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("description", description)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(), // ID único
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val now = Calendar.getInstance()
            val calendar = Calendar.getInstance()
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = formatter.parse(time)

            if (date != null) {
                calendar.set(Calendar.HOUR_OF_DAY, date.hours)
                calendar.set(Calendar.MINUTE, date.minutes)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                // Se o horário já passou hoje, agenda para o próximo dia
                if (calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                // Verifica permissão para alarmes exatos (API 31+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val canSchedule = alarmManager.canScheduleExactAlarms()
                    if (!canSchedule) {
                        Log.w("NotificationUtils", "Permissão SCHEDULE_EXACT_ALARM não concedida.")
                        return
                    }
                }

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )

                Log.d("NotificationUtils", "Notificação agendada para: ${calendar.time}")
            }
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Erro ao agendar notificação: ${e.message}", e)
        }
    }
}
