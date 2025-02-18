package com.example.homework1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import com.example.homework1.ui.theme.Homework1Theme
import com.example.homework1.Message





val Context.dataStore by preferencesDataStore(name = "user_prefs")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            Homework1Theme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "Viestit") {
                    composable("Viestit") { HomeScreen(navController) }
                    composable("Asetukset") { SecondScreen(navController) }
                }
            }
        }
    }


    data class Message(val author: String, val body: String)
//kotinäkymä eli viestit osuus. Oletuksena ilman profiliikuvaa ja nimenä denho.
    @Composable
    fun HomeScreen(navController: NavController) {
        Log.d("NavigationDebug", "🔹 Avattiin: Viestit")
        val context = LocalContext.current
        val userName by remember { mutableStateOf(loadUserName(context)) }
        val imageUri by remember { mutableStateOf(loadImageUri(context)) }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ratas asetusten avaamiseen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { navController.navigate("Asetukset") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "Asetukset",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(text = "Viestit", style = MaterialTheme.typography.headlineMedium)

            // viestit
            LazyColumn {
                items(SampleData.conversationSample) { message ->
                    MessageCard(msg = Message(userName, message.body), imageUri)
                }
            }
        }
    }

// "asetukset" näkymä mahdollisuus vaihtaa nimeä sekä profiilikuvaa
    @Composable
    fun SecondScreen(navController: NavController) {
        Log.d("NavigationDebug", "🔹 Avattiin: Asetukset")
        val context = LocalContext.current

        // Ladataan tallennettu nimi ja profiilikuva
        val userName = remember { mutableStateOf(loadUserName(context)) }
        val imageUri = remember { mutableStateOf(loadImageUri(context)) }

        val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri.value = saveImageToInternalStorage(context, it)
                saveImageUri(context, imageUri.value!!)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Asetukset", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))

            // Profiilikuvan tai tyhjän ympyrän näyttäminen
            if (imageUri.value != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri.value),
                    contentDescription = "Profiilikuva",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePicker.launch("image/*") }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_camera),
                        contentDescription = "Valitse kuva",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            //  Käyttäjänimen muokkaus
            OutlinedTextField(
                value = userName.value,
                onValueChange = { userName.value = it },
                label = { Text("Käyttäjänimi") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    saveUserName(context, userName.value)  //  Tallennetaan nimi
                    navController.navigate("Viestit")     //  Palataan takaisin viesteihin
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Tallenna ja palaa")
            }
        }
    }


    @Composable
    fun MessageCard(msg: Message, imageUri: Uri?) {
        var isExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { isExpanded = !isExpanded }  // Klikkaamalla viesti aukeaa/sulkeutuu
        ) {

            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_camera),
                        contentDescription = "Tyhjä profiili",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = msg.author,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 1.dp,
                    modifier = Modifier.padding(1.dp)
                ) {
                    Text(
                        text = if (isExpanded) msg.body else msg.body.take(50) + "...",
                        modifier = Modifier
                            .padding(8.dp)
                            .animateContentSize(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }




fun saveUserName(context: Context, name: String) {
    runBlocking {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("user_name")] = name
        }
    }
}

fun loadUserName(context: Context): String {
    return runBlocking {
        context.dataStore.data.first()[stringPreferencesKey("user_name")] ?: "denho"
    }
}

fun saveImageUri(context: Context, uri: Uri) {
    runBlocking {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("image_uri")] = uri.toString()
        }
    }
}

fun loadImageUri(context: Context): Uri? {
    return runBlocking {
        context.dataStore.data.first()[stringPreferencesKey("image_uri")]?.let { Uri.parse(it) }
    }
}

fun saveImageToInternalStorage(context: Context, uri: Uri): Uri {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val file = File(context.filesDir, "selected_image.jpg")
    val outputStream = FileOutputStream(file)
    inputStream?.copyTo(outputStream)
    inputStream?.close()
    outputStream.close()
    return Uri.fromFile(file)
}
}

