package com.edson.ghwatch

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Dark = darkColorScheme(
    primary = Color(0xFF58A6FF),
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3)
)

private val INTERVALS = listOf(15 to "15 min", 30 to "30 min", 60 to "1 h", 120 to "2 h", 180 to "3 h", 360 to "6 h")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Checker.schedule(this, Store.intervalMinutes(this))
        setContent { MaterialTheme(colorScheme = Dark) { Screen() } }
    }
}

@Composable
fun Screen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var repos by remember { mutableStateOf(Store.repos(ctx)) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var interval by remember { mutableStateOf(Store.intervalMinutes(ctx)) }
    var busy by remember { mutableStateOf(false) }
    val version = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    }
    var token by remember { mutableStateOf(Store.token(ctx)) }
    var tokenSaved by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Repo?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val msg = try {
                Backup.export(ctx, uri); "Backup salvo"
            } catch (e: Exception) {
                "Erro ao salvar o backup"
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val msg = try {
                val n = Backup.import(ctx, uri)
                repos = Store.repos(ctx)
                interval = Store.intervalMinutes(ctx)
                "Backup restaurado: $n repositório(s) adicionado(s)"
            } catch (e: Exception) {
                "Arquivo de backup inválido"
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun add() {
        val slug = GitHub.parseSlug(input)
        when {
            slug == null -> error = "Use owner/repo ou a URL completa do GitHub"
            repos.any { it.slug.equals(slug, true) } -> error = "Repositório já cadastrado"
            else -> {
                repos = repos + Repo(slug)
                Store.save(ctx, repos)
                input = ""; error = null
                scope.launch { Checker.run(ctx); repos = Store.repos(ctx) }
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Text("Atualização GitHub", style = MaterialTheme.typography.headlineMedium)
            Text("Versão $version", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = input, onValueChange = { input = it },
                label = { Text("owner/repo ou URL") },
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = ::add, modifier = Modifier.fillMaxWidth()) { Text("Adicionar repositório") }

            Spacer(Modifier.height(16.dp))
            Text("Checar a cada", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                INTERVALS.forEach { (m, label) ->
                    FilterChip(
                        selected = interval == m,
                        onClick = { interval = m; Store.setInterval(ctx, m); Checker.schedule(ctx, m) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { scope.launch { busy = true; Checker.run(ctx); repos = Store.repos(ctx); busy = false } },
                enabled = !busy, modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Verificando..." else "Verificar agora") }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it; tokenSaved = false },
                label = { Text("Token do GitHub (opcional)") },
                supportingText = { Text(if (tokenSaved) "Token salvo" else "Aumenta o limite da API de 60 para 5.000 req/h") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { Store.setToken(ctx, token); tokenSaved = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (token.isBlank()) "Remover token" else "Salvar token") }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val name = SimpleDateFormat("yy-MM-dd", Locale.getDefault()).format(Date()) + ".json"
                        exportLauncher.launch(name)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Fazer backup") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f)
                ) { Text("Restaurar") }
            }

            toDelete?.let { r ->
                AlertDialog(
                    onDismissRequest = { toDelete = null },
                    title = { Text("Excluir repositório?") },
                    text = { Text("Você não será mais notificado sobre ${r.slug}.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                repos = repos.filterNot { it.slug == r.slug }
                                Store.save(ctx, repos)
                                toDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF85149))
                        ) { Text("Excluir") }
                    },
                    dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } }
                )
            }

            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(repos, key = { it.slug }) { r ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.slug, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Última versão: ${r.lastTag ?: "—"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (r.lastChecked > 0)
                                    Text(
                                        "Checado: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                            .format(Date(r.lastChecked)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                            }
                            TextButton(
                                onClick = { toDelete = r },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF85149))
                            ) { Text("Excluir") }
                        }
                    }
                }
            }
        }
    }
}
