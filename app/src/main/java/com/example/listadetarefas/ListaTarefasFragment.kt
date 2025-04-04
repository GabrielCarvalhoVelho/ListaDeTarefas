package com.example.listadetarefas

import TarefaAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.listadetarefas.model.Tarefa
import com.example.listadetarefas.viewmodel.TarefaViewModel

class ListaTarefasFragment : Fragment() {
    private lateinit var viewModel: TarefaViewModel
    private lateinit var adapter: TarefaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_lista_tarefas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[TarefaViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTarefas)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = TarefaAdapter(
            tarefas = emptyList(),
            onEditar = { tarefa, novoTitulo, novaDescricao ->
                Log.d("ListaTarefasFragment", "Editando tarefa: ${tarefa.id}")
                viewModel.editarTarefa(tarefa, novoTitulo, novaDescricao)
            },
            onExcluir = { tarefa ->
                Log.d("ListaTarefasFragment", "Excluindo tarefa: ${tarefa.id}")
                viewModel.excluirTarefa(tarefa)
            },
            onCompletar = { tarefa ->
                Log.d("ListaTarefasFragment", "Completando tarefa: ${tarefa.id}")
                viewModel.marcarComoCompleta(tarefa)
            }
        )

        recyclerView.adapter = adapter

        viewModel.tarefas.observe(viewLifecycleOwner) { tarefas ->
            Log.d("ListaTarefasFragment", "Tarefas recebidas: ${tarefas.size}")
            tarefas.forEach { tarefa ->
                Log.d("ListaTarefasFragment", " - ${tarefa.titulo} (concluída: ${tarefa.concluida})")
            }
            adapter.atualizarLista(tarefas)
        }

        viewModel.carregarTarefas()
    }
}