package com.example.listadetarefas.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.listadetarefas.model.Tarefa
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TarefaViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _tarefas = MutableLiveData<List<Tarefa>>()
    val tarefas: LiveData<List<Tarefa>> get() = _tarefas

    fun carregarTarefas() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("tarefas")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val lista = snapshot.documents.map { doc ->
                        Tarefa(
                            id = doc.id,
                            titulo = doc.getString("titulo") ?: "",
                            descricao = doc.getString("descricao") ?: "",
                            concluida = doc.getBoolean("completa") ?: false
                        )
                    }
                    _tarefas.value = lista
                }
            }
    }

    fun editarTarefa(tarefa: Tarefa, novoTitulo: String, novaDescricao: String) {
        if (novoTitulo.isBlank() || novaDescricao.isBlank()) {
            Log.e("TarefaViewModel", "Título ou descrição vazios, edição cancelada.")
            return
        }

        val dadosAtualizados = mapOf(
            "titulo" to novoTitulo,
            "descricao" to novaDescricao
        )

        db.collection("tarefas").document(tarefa.id)
            .update(dadosAtualizados)
            .addOnSuccessListener {
                Log.d("TarefaViewModel", "Tarefa editada com sucesso!")
                carregarTarefas() // Atualiza a lista após edição
            }
            .addOnFailureListener { e ->
                Log.e("TarefaViewModel", "Erro ao editar tarefa", e)
            }
    }


    fun excluirTarefa(tarefa: Tarefa) {
        db.collection("tarefas").document(tarefa.id).delete()
    }

    fun marcarComoCompleta(tarefa: Tarefa) {
        db.collection("tarefas").document(tarefa.id)
            .update("completa", true)
    }
}