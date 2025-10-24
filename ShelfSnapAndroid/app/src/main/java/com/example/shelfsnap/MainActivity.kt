package com.example.shelfsnap

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.example.shelfsnap.ui.theme.ShelfSnapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShelfSnapTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShelfSnapApp()
                }
            }
        }
    }
}

@Composable
fun ShelfSnapApp() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Greeting()
        }
    }
}

@Composable
fun Greeting() {
    // Maintain state to determine whether the camera/detection screen should be shown
    val showCamera = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    // Maintain state to determine whether the planogram list should be shown
    val showPlanogramList = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    // Maintain state to determine whether the planogram builder should be shown
    val showPlanogramBuilder = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    // Hold the planogram selected from the list for editing; null indicates creating a new planogram
    val selectedPlanogram = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.shelfsnap.models.Planogram?>(null) }
    // Create the ScanViewModel once for this composable
    val scanViewModel = androidx.compose.runtime.remember { com.example.shelfsnap.scan.ScanViewModel() }

    // Create a Supabase repository for fetching planograms
    val supabaseRepository = androidx.compose.runtime.remember { com.example.shelfsnap.data.SupabaseRepository() }

    val context = LocalContext.current
    // Request camera permission when needed
    fun ensureCameraPermission() {
        val permission = Manifest.permission.CAMERA
        val isGranted = ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!isGranted && context is ComponentActivity) {
            ActivityCompat.requestPermissions(context, arrayOf(permission), 0)
        }
    }

    when {
        showCamera.value -> {
            // Display the ScanScreen which hosts the camera preview and detection overlay
            com.example.shelfsnap.scan.ScanScreen(scanViewModel = scanViewModel)
        }
        showPlanogramBuilder.value -> {
            // Show the planogram builder
            com.example.shelfsnap.planogram.PlanogramBuilderScreen(
                repository = supabaseRepository,
                planogram = selectedPlanogram.value,
                onSave = { plan, items ->
                    // TODO: persist the planogram and items to Supabase via repository (not yet implemented)
                    // Hide builder and return to list
                    showPlanogramBuilder.value = false
                    showPlanogramList.value = false
                },
                onCancel = {
                    // Cancel editing, return to list
                    showPlanogramBuilder.value = false
                    // stay on list
                }
            )
        }
        showPlanogramList.value -> {
            // Display list of planograms from Supabase
            com.example.shelfsnap.planogram.PlanogramListScreen(
                repository = supabaseRepository,
                onPlanogramSelected = { planogram ->
                    // When a planogram is selected, open it in the builder
                    selectedPlanogram.value = planogram
                    showPlanogramList.value = false
                    showPlanogramBuilder.value = true
                }
            )
        }
        else -> {
            // Home screen with actions
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    ensureCameraPermission()
                    showCamera.value = true
                }, modifier = Modifier.padding(8.dp)) {
                    Text(text = "Capture Shelf")
                }
                Button(onClick = {
                    // Show planogram list
                    showPlanogramList.value = true
                }, modifier = Modifier.padding(8.dp)) {
                    Text(text = "View Planograms")
                }
                Button(onClick = {
                    // Create a new planogram using the builder
                    selectedPlanogram.value = null
                    showPlanogramBuilder.value = true
                }, modifier = Modifier.padding(8.dp)) {
                    Text(text = "New Planogram")
                }
            }
        }
    }
}