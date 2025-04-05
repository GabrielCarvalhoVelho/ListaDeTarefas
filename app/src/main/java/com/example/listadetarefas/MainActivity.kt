package com.example.listadetarefas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier



import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.listadetarefas.NotificationUtils.scheduleNotification
import com.example.listadetarefas.ui.theme.ListaDeTarefasTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

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

    // Gradiente de fundo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Cinza claro elegante
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally // Centraliza horizontalmente
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo), // nome do arquivo do logo
                contentDescription = "Logo do Aplicativo",
                modifier = Modifier
                    .size(240.dp) // você pode ajustar o tamanho
                    .padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color(0xFF212121)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF212121)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33FFFFFF), shape = RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF212121),
                    unfocusedBorderColor = Color(0xFF212121),
                    cursorColor = Color(0xFF212121),
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha", color = Color(0xFF212121)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF212121)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33FFFFFF), shape = RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF212121),
                    unfocusedBorderColor = Color(0xFF212121),
                    cursorColor = Color(0xFF212121),
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red, modifier = Modifier.padding(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            .addOnFailureListener {
                                errorMessage = "Erro no login: ${it.localizedMessage}"
                            }
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val user = auth.currentUser
                                user?.sendEmailVerification()
                                    ?.addOnSuccessListener {
                                        errorMessage = "Verifique seu e-mail para ativar a conta"
                                    }
                                    ?.addOnFailureListener {
                                        errorMessage = "Erro ao enviar e-mail de verificação: ${it.localizedMessage}"
                                    }
                            }
                            .addOnFailureListener {
                                errorMessage = "Erro no cadastro: ${it.localizedMessage}"
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                ),
                border = BorderStroke(2.dp, Color(0xFF2E7D32)) // Borda verde elegante
            ) {
                Text(
                    text = if (isLogin) "Entrar" else "Cadastrar",
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { isLogin = !isLogin }) {
                Text(
                    if (isLogin) "Criar conta" else "Já tem uma conta? Entrar",
                    color = Color(0xFF212121)
                )
            }

            TextButton(onClick = {
                val user = auth.currentUser
                user?.sendEmailVerification()?.addOnSuccessListener {
                    errorMessage = "E-mail de verificação reenviado!"
                }?.addOnFailureListener {
                    errorMessage = "Erro ao reenviar e-mail: ${it.localizedMessage}"
                }
            }) {
                Text("Reenviar e-mail de verificação", color = Color(0xFF212121))
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
                Text("Esqueci minha senha", color = Color(0xFF212121))
            }
        }
    }
}


@Composable
fun TaskScreen(modifier: Modifier = Modifier, userId: String, onLogout: () -> Unit) {
    val context = LocalContext.current

    var time by remember { mutableStateOf("") }
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    var editedTime by remember { mutableStateOf("") }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, selectedHour: Int, selectedMinute: Int ->
                val formatted = String.format("%02d:%02d", selectedHour, selectedMinute)
                time = formatted
            },
            hour,
            minute,
            true // 24h
        )
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Cinza claro elegante
            .padding(16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Minhas Tarefas",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White // Fundo branco
                    ),
                    border = BorderStroke(2.dp, Color(0xFFD32F2F)), // Borda vermelha
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Sair",
                        color = Color(0xFFD32F2F) // Texto da mesma cor da borda
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título da Tarefa") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33FFFFFF), shape = RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF212121),
                    unfocusedBorderColor = Color(0xFF212121),
                    cursorColor = Color(0xFF212121),
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121),
                    focusedLabelColor = Color(0xFF212121),    // Cor do label com foco
                    unfocusedLabelColor = Color(0xFF212121)   // Cor do label sem foco
                )

            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF212121),
                    unfocusedBorderColor = Color(0xFF212121),
                    cursorColor = Color(0xFF212121),
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121),
                    focusedLabelColor = Color(0xFF212121),    // Cor do label com foco
                    unfocusedLabelColor = Color(0xFF212121)   // Cor do label sem foco
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            TimeInputField(time = time, onTimeChange = { time = it })
            Spacer(modifier = Modifier.height(12.dp))
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(2.dp, Color(0xFF2E7D32)) // Adicionando a borda aqui
            ) {
                Text("Adicionar Tarefa", color = Color(0xFF2E7D32))
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tasks) { task ->
                    val isCompleted = task["completed"] as? Boolean ?: false
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = task["title"].toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212121)
                                )
                            )
                            Text(
                                text = task["description"].toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = Color(0xFF212121)
                                )
                            )
                            Text(
                                text = task["time"].toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = Color(0xFF212121)
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botão Editar - Azul
                                OutlinedButton(
                                    onClick = {
                                        taskToEdit = task
                                        editedTime = task["time"].toString() // carrega horário atual
                                        showEditDialog = true
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF1976D2)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF1976D2)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Editar")
                                }

// Botão Excluir - Vermelho
                                OutlinedButton(
                                    onClick = {
                                        val taskId = task["id"].toString()
                                        db.collection("tasks").document(taskId).delete()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFFD32F2F)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Excluir")
                                }

// Botão Completar - Verde
                                OutlinedButton(
                                    onClick = {
                                        val taskId = task["id"].toString()
                                        val isCompleted = task["completed"] as? Boolean ?: false

                                        db.collection("tasks").document(taskId)
                                            .update("completed", !isCompleted)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF388E3C)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if ((task["completed"] as? Boolean) == true) "❌ Desmarcar" else "✔ Completar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && taskToEdit != null) {
        var editedTime by remember { mutableStateOf(taskToEdit!!["time"].toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Editar Tarefa",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = taskToEdit!!["title"].toString(),
                        onValueChange = {
                            taskToEdit = taskToEdit!!.toMutableMap().apply { this["title"] = it }
                        },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF212121),
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            cursorColor = Color(0xFF212121),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121),
                            focusedLabelColor = Color(0xFF212121),
                            unfocusedLabelColor = Color(0xFF757575)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = taskToEdit!!["description"].toString(),
                        onValueChange = {
                            taskToEdit = taskToEdit!!.toMutableMap().apply { this["description"] = it }
                        },
                        label = { Text("Descrição") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF212121),
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            cursorColor = Color(0xFF212121),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121),
                            focusedLabelColor = Color(0xFF212121),
                            unfocusedLabelColor = Color(0xFF757575)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TimeInputField(
                        time = editedTime,
                        onTimeChange = {
                            editedTime = it
                            taskToEdit = taskToEdit!!.toMutableMap().apply { this["time"] = it }
                        }
                    )
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        val taskId = taskToEdit!!["id"].toString()
                        db.collection("tasks").document(taskId).update(taskToEdit!!)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1976D2)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF616161)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

}

@Composable
fun TimeInputField(
    time: String,
    onTimeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, selectedHour: Int, selectedMinute: Int ->
                val formatted = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeChange(formatted)
            },
            hour,
            minute,
            true
        )
    }

    OutlinedTextField(
        value = time,
        onValueChange = {},
        label = { Text("Horário") },
        readOnly = true,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Selecionar horário",
                modifier = Modifier.clickable { timePickerDialog.show() }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { timePickerDialog.show() },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF212121),
            unfocusedBorderColor = Color(0xFF212121),
            cursorColor = Color(0xFF212121),
            focusedTextColor = Color(0xFF212121),
            unfocusedTextColor = Color(0xFF212121),
            focusedLabelColor = Color(0xFF212121),
            unfocusedLabelColor = Color(0xFF212121)
        )
    )
}


