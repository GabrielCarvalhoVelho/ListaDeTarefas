package com.example.listadetarefas

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.listadetarefas.ui.theme.ListaDeTarefasTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚨 Solicita permissão para exibir notificações no Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }

        // ✅ Cria o canal de notificação
        createNotificationChannel()

        enableEdgeToEdge()
        setContent {
            ListaDeTarefasTheme {
                var user by remember { mutableStateOf(auth.currentUser) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (user == null) {
                        AuthScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLoginSuccess = { user = auth.currentUser }
                        )
                    } else {
                        TaskScreen(
                            modifier = Modifier.padding(innerPadding),
                            userId = user!!.uid,
                            onLogout = {
                                auth.signOut()
                                user = null
                            }
                        )
                    }
                }
            }
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Channel"
            val descriptionText = "Canal para lembretes de tarefas"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("task_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun AuthScreen(modifier: Modifier = Modifier, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errorMessage = "Formato de e-mail inválido"
                    return@Button
                }
                if (password.length < 6) {
                    errorMessage = "A senha deve ter pelo menos 6 caracteres"
                    return@Button
                }
                errorMessage = ""

                if (isLogin) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            val user = auth.currentUser
                            if (user?.isEmailVerified == true) {
                                onLoginSuccess()
                            } else {
                                errorMessage = "Por favor, verifique seu e-mail antes de fazer login"
                                auth.signOut()
                            }
                        }
                        .addOnFailureListener { errorMessage = "Erro no login: ${it.localizedMessage}" }
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            val user = auth.currentUser
                            user?.sendEmailVerification()?.addOnSuccessListener {
                                errorMessage = "Verifique seu e-mail para ativar a conta"
                            }?.addOnFailureListener {
                                errorMessage = "Erro ao enviar e-mail de verificação: ${it.localizedMessage}"
                            }
                        }
                        .addOnFailureListener { errorMessage = "Erro no cadastro: ${it.localizedMessage}" }
                }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLogin) "Entrar" else "Cadastrar")
        }
        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Criar conta" else "Já tem uma conta? Entrar")
        }
        TextButton(onClick = {
            val user = auth.currentUser
            user?.sendEmailVerification()?.addOnSuccessListener {
                errorMessage = "E-mail de verificação reenviado!"
            }?.addOnFailureListener {
                errorMessage = "Erro ao reenviar e-mail: ${it.localizedMessage}"
            }
        }) {
            Text("Reenviar e-mail de verificação")
        }
        TextButton(onClick = {
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        errorMessage = "E-mail para redefinição de senha enviado!"
                    }
                    .addOnFailureListener {
                        errorMessage = "Erro ao enviar e-mail: ${it.localizedMessage}"
                    }
            } else {
                errorMessage = "Digite um e-mail válido para redefinição de senha"
            }
        }) {
            Text("Esqueci minha senha")
        }
    }
}

@Composable
fun TaskScreen(modifier: Modifier = Modifier, userId: String, onLogout: () -> Unit) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(emptyList<Map<String, Any>>()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Map<String, Any>?>(null) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(userId) {
        db.collection("tasks").whereEqualTo("userId", userId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Firestore", "Erro ao buscar tarefas", e)
                return@addSnapshotListener
            }
            tasks = snapshot?.documents?.map { it.data!! + ("id" to it.id) } ?: emptyList()
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sair", color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título da Tarefa") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descrição") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Horário (HH:mm)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (title.isNotBlank() && description.isNotBlank() && time.isNotBlank()) {
                    val task = hashMapOf(
                        "title" to title,
                        "description" to description,
                        "completed" to false,
                        "userId" to userId,
                        "time" to time
                    )
                    db.collection("tasks").add(task)
                    scheduleNotification(context, title, description, time)
                    title = ""
                    description = ""
                    time = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Adicionar Tarefa", color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(tasks) { task ->
                val isCompleted = task["completed"] as? Boolean ?: false
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = task["title"].toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        )
                        Text(
                            text = task["description"].toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    taskToEdit = task
                                    showEditDialog = true
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Editar")
                            }
                            Button(
                                onClick = {
                                    val taskId = task["id"].toString()
                                    db.collection("tasks").document(taskId).delete()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Excluir", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    val taskId = task["id"].toString()
                                    db.collection("tasks").document(taskId)
                                        .update("completed", true)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("✔ Completar")
                            }
                        }
                    }
                }
            }
        }
    }
    if (showEditDialog && taskToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Tarefa") },
            text = {
                Column {
                    OutlinedTextField(
                        value = taskToEdit!!["title"].toString(),
                        onValueChange = {
                            taskToEdit = taskToEdit!!.toMutableMap().apply { this["title"] = it }
                        },
                        label = { Text("Título") }
                    )
                    OutlinedTextField(
                        value = taskToEdit!!["description"].toString(),
                        onValueChange = {
                            taskToEdit =
                                taskToEdit!!.toMutableMap().apply { this["description"] = it }
                        },
                        label = { Text("Descrição") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val taskId = taskToEdit!!["id"].toString()
                    db.collection("tasks").document(taskId).update(taskToEdit!!)
                    showEditDialog = false
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

fun scheduleNotification(context: Context, title: String, description: String, time: String) {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val calendar = Calendar.getInstance()

    try {
        val date = formatter.parse(time)
        if (date != null) {
            val now = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, now.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

            // Se a hora já passou hoje, agenda para o próximo dia
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("description", description)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), // ID único
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // ✅ Verifica permissão e solicita se necessário (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w("NotificationUtils", "Permissão SCHEDULE_EXACT_ALARM não concedida. Solicitando ao usuário.")
                    val settingsIntent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return // Sai da função, espera o usuário aceitar
                }
            }

            Log.d("NotificationUtils", "Agendando notificação para: ${calendar.time}")

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("NotificationUtils", "Erro ao agendar notificação: ${e.localizedMessage}")
    }
}



