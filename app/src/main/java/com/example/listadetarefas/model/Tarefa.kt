package com.example.listadetarefas.model

data class Tarefa(
    val id: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val concluida: Boolean = false,
    val time: String = "" // <- Novo campo adicionado
)
