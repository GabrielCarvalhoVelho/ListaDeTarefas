import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.listadetarefas.R
import com.example.listadetarefas.model.Tarefa

class TarefaAdapter(
    private var tarefas: List<Tarefa>,
    private val onEditar: (Tarefa, String, String) -> Unit,
    private val onExcluir: (Tarefa) -> Unit,
    private val onCompletar: (Tarefa) -> Unit
) : RecyclerView.Adapter<TarefaAdapter.TarefaViewHolder>() {

    inner class TarefaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titulo: TextView = itemView.findViewById(R.id.txtTitulo)
        val descricao: TextView = itemView.findViewById(R.id.txtDescricao)
        val btnEditar: Button = itemView.findViewById(R.id.btnEditar)
        val btnExcluir: Button = itemView.findViewById(R.id.btnExcluir)
        val btnCompletar: Button = itemView.findViewById(R.id.btnCompletar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarefa, parent, false) // Certifique-se de que este é o layout correto!
        return TarefaViewHolder(view)
    }


    override fun onBindViewHolder(holder: TarefaViewHolder, position: Int) {
        val tarefa = tarefas[position]
        holder.titulo.text = tarefa.titulo
        holder.descricao.text = tarefa.descricao
        holder.btnCompletar.text = if (tarefa.concluida) "✅ Completa" else "✔ Completar"

        Log.d("RecyclerView", "Tarefa carregada: ${tarefa.titulo}")
        Log.d("TarefaAdapter", "ViewHolder - btnEditar visível: ${holder.btnEditar.visibility}")
        Log.d("TarefaAdapter", "ViewHolder - btnExcluir visível: ${holder.btnExcluir.visibility}")
        Log.d("TarefaAdapter", "ViewHolder - btnCompletar visível: ${holder.btnCompletar.visibility}")

        // Garante que os botões estejam visíveis
        holder.btnEditar.visibility = View.VISIBLE
        holder.btnExcluir.visibility = View.VISIBLE
        holder.btnCompletar.visibility = View.VISIBLE

        holder.btnEditar.setOnClickListener {
            val context = holder.itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_editar_tarefa, null)

            val inputTitulo = dialogView.findViewById<EditText>(R.id.editTitulo)
            val inputDescricao = dialogView.findViewById<EditText>(R.id.editDescricao)

            inputTitulo.setText(tarefa.titulo)
            inputDescricao.setText(tarefa.descricao)

            AlertDialog.Builder(context)
                .setTitle("Editar Tarefa")
                .setView(dialogView)
                .setPositiveButton("Salvar") { _, _ ->
                    val novoTitulo = inputTitulo.text.toString()
                    val novaDescricao = inputDescricao.text.toString()
                    onEditar(tarefa, novoTitulo, novaDescricao)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        holder.btnExcluir.setOnClickListener { onExcluir(tarefa) }
        holder.btnCompletar.setOnClickListener { onCompletar(tarefa) }
    }

    override fun getItemCount(): Int = tarefas.size

    fun atualizarLista(novasTarefas: List<Tarefa>) {
        this.tarefas = novasTarefas
        notifyDataSetChanged()
    }
}
