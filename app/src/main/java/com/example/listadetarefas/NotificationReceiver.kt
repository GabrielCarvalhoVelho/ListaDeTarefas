package com.example.listadetarefas

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "🔔 Alarme recebido!")

        val title = intent.getStringExtra("title") ?: "Lembrete de Tarefa"
        val taskDescription = intent.getStringExtra("description") ?: "Você tem uma tarefa pendente"

        Log.d("NotificationReceiver", "Título: $title")
        Log.d("NotificationReceiver", "Descrição: $taskDescription")

        // Criar canal de notificação se necessário
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "task_channel",
                "Task Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para lembretes de tarefas"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationReceiver", "📢 Canal de notificação criado")
        }

        // Intent para abrir o app ao clicar na notificação
        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(activityIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        // Criar notificação
        val notification = NotificationCompat.Builder(context, "task_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(taskDescription)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // ✅ Verificar permissão antes de exibir a notificação
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Log.d("NotificationReceiver", "✅ Permissão de notificação concedida. Exibindo...")
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), notification)
            }
        } else {
            Log.w("NotificationReceiver", "🚫 Permissão POST_NOTIFICATIONS não concedida")
        }
    }
}
