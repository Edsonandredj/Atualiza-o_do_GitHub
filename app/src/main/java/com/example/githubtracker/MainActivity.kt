package com.example.githubtracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.githubtracker.api.RetrofitClient
import com.example.githubtracker.data.AppDatabase
import com.example.githubtracker.data.RepositoryEntity
import com.example.githubtracker.utils.NotificationHelper
import com.example.githubtracker.worker.UpdateCheckWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Agenda checagem inicial padrão (ex: 60 minutos)
        UpdateCheckWorker.schedulePeriodicWork(this, 60)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "github-tracker.db").build()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(db)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(db: AppDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputUrl by remember { mutableStateOf("") }
    var selectedInterval by remember { mutableFloatStateOf(60f) }
    
    val repositories by db.repoDao().getAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("GitHub Release Tracker") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Campo de Inserção de Repositório
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text("URL ou owner/repo (ex: google/gson)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val parsed = parseRepoInput(inputUrl)
                    if (parsed != null) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val release = RetrofitClient.service.getLatestRelease(parsed.first, parsed.second)
                                db.repoDao().insert(
                                    RepositoryEntity(
                                        id = "${parsed.first}/${parsed.second}",
                                        owner = parsed.first,
                                        repo = parsed.second,
                                        lastTag = release.tagName
                                    )
                                )
                                withContext(Dispatchers.Main) { inputUrl = "" }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Erro ao buscar repositório no GitHub", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Formato inválido!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Adicionar Repositório")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider de Intervalo (15 min até 360 min / 6 horas)
            Text("Frequência de Checagem: ${selectedInterval.toInt()} minutos")
            Slider(
                value = selectedInterval,
                onValueChange = { selectedInterval = it },
                valueRange = 15f..360f,
                steps = 6,
                onValueChangeFinished = {
                    UpdateCheckWorker.schedulePeriodicWork(context, selectedInterval.toLong())
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de Repositórios Cadastrados
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(repositories) { repo ->
                    RepositoryCard(
                        repo = repo,
                        onDelete = {
                            scope.launch(Dispatchers.IO) {
                                db.repoDao().deleteById(repo.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RepositoryCard(repo: RepositoryEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = repo.id, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Última versão: ${repo.lastTag ?: "Desconhecida"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("Excluir", color = Color.White)
            }
        }
    }
}

private fun parseRepoInput(input: String): Pair<String, String>? {
    val clean = input.trim().removePrefix("https://github.com/").removePrefix("http://github.com/").removeSuffix("/")
    val parts = clean.split("/")
    return if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        Pair(parts[0], parts[1])
    } else null
}
