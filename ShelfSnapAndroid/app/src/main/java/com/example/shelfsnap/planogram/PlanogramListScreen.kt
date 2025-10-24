package com.example.shelfsnap.planogram

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shelfsnap.data.SupabaseRepository
import com.example.shelfsnap.models.Planogram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Displays a list of planograms fetched from Supabase. Each entry shows the planogram name
 * and can be clicked to perform an action (e.g. open a detail screen or start editing).
 * The list is loaded asynchronously on composition. If Supabase cannot be reached or returns
 * no planograms, an empty state is shown. This screen is a simple example of fetching
 * remote data in Compose and will need to be integrated with your navigation and
 * error-handling patterns.
 */
@Composable
fun PlanogramListScreen(
    repository: SupabaseRepository,
    onPlanogramSelected: (Planogram) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var planograms by remember { mutableStateOf<List<Planogram>>(emptyList()) }

    // Kick off loading when this composable first appears
    LaunchedEffect(Unit) {
        isLoading = true
        val result = withContext(Dispatchers.IO) { repository.getPlanograms() }
        planograms = result
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (planograms.isEmpty()) {
            Text(text = "No planograms found.", style = MaterialTheme.typography.bodyLarge)
        } else {
            planograms.forEach { planogram ->
                ListItem(
                    modifier = Modifier.clickable { onPlanogramSelected(planogram) },
                    headlineContent = { Text(text = planogram.name) },
                    supportingContent = { planogram.section?.let { Text(text = it) } }
                )
            }
        }
    }
}