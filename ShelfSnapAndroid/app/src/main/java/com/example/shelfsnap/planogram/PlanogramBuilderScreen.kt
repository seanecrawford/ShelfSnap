package com.example.shelfsnap.planogram

import android.graphics.RectF
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.shelfsnap.data.SupabaseRepository
import com.example.shelfsnap.models.Planogram
import com.example.shelfsnap.models.PlanogramItem
import com.example.shelfsnap.models.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A simple planogram builder that allows users to assign products to a grid representing shelves.
 * The grid dimensions are derived from the provided [planogram] if editing an existing layout,
 * or from the [shelvesCount] and [columns] parameters when creating a new planogram. Each cell
 * contains a button that opens a dropdown menu of available products. Upon selection, the
 * chosen product is assigned to that cell. The user can save the planogram, which invokes
 * [onSave] with the assembled data. This implementation does not persist data to Supabase;
 * you should extend [SupabaseRepository] to insert the planogram and its items.
 */
@Composable
fun PlanogramBuilderScreen(
    repository: SupabaseRepository,
    planogram: Planogram? = null,
    shelvesCount: Int = planogram?.shelvesCount ?: 3,
    columns: Int = 4,
    onSave: (Planogram, List<PlanogramItem>) -> Unit,
    onCancel: () -> Unit
) {
    // Load products from Supabase. If none are returned, fall back to a static list for demo purposes.
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        // Load products in IO context
        val result = withContext(Dispatchers.IO) { repository.getProducts() }
        products = if (result.isNotEmpty()) result else listOf(
            Product(id = "prod1", sku = "MILK500", name = "Milk 500ml", upc = null, brand = "DairyCo", category = "Dairy", widthMm = 100, heightMm = 200, depthMm = 100, imageUrl = null),
            Product(id = "prod2", sku = "BREAD", name = "Bread Loaf", upc = null, brand = "Bakery", category = "Bakery", widthMm = 120, heightMm = 150, depthMm = 100, imageUrl = null),
            Product(id = "prod3", sku = "CEREAL", name = "Cereal 400g", upc = null, brand = "Cereal Co", category = "Breakfast", widthMm = 80, heightMm = 250, depthMm = 50, imageUrl = null),
            Product(id = "prod4", sku = "OJ1000", name = "Orange Juice 1L", upc = null, brand = "JuiceCo", category = "Beverages", widthMm = 90, heightMm = 220, depthMm = 90, imageUrl = null)
        )
    }

    // Data structure representing a single grid cell assignment
    data class Cell(var productId: String?)

    // Initialize grid cells for the given number of shelves and columns. If editing an existing
    // planogram, pre-populate the cells based on its items; otherwise start empty.
    var cells by remember { mutableStateOf<List<Cell>>(emptyList()) }
    LaunchedEffect(products) {
        if (cells.isEmpty() && products.isNotEmpty()) {
            val initial = MutableList(shelvesCount * columns) { Cell(null) }
            // If editing an existing planogram, fill the cells according to planogram items
            planogram?.let {
                val items = withContext(Dispatchers.IO) { repository.getPlanogramItems(it.id) }
                items.forEach { item ->
                    val row = item.shelfIndex
                    // Compute column based on x coordinate proportionally. Here we assume equal width cells.
                    val col = ((item.xMm.toFloat() / it.shelfWidthMm.toFloat()) * columns).toInt().coerceIn(0, columns - 1)
                    val index = row * columns + col
                    if (index in initial.indices) {
                        initial[index].productId = item.productId
                    }
                }
            }
            cells = initial
        }
    }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Render each shelf as a row
        for (row in 0 until shelvesCount) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    val cell = cells.getOrNull(index) ?: Cell(null)
                    var expanded by remember { mutableStateOf(false) }
                    // Find the currently selected product for this cell
                    val selectedProduct = products.firstOrNull { it.id == cell.productId }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(1.dp, Color.Gray)
                            .padding(4.dp)
                    ) {
                        // Display current selection or placeholder
                        Button(onClick = { expanded = true }) {
                            Text(text = selectedProduct?.name ?: "Select")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            products.forEach { product ->
                                DropdownMenuItem(
                                    text = { Text(text = product.name) },
                                    onClick = {
                                        // Update selected product for this cell
                                        expanded = false
                                        val newCells = cells.toMutableList()
                                        newCells[index] = Cell(product.id)
                                        cells = newCells
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
            Button(onClick = {
                // Assemble a new planogram to save. For new planograms we generate a temporary ID.
                val planogramId = planogram?.id ?: "new-${System.currentTimeMillis()}"
                val newPlanogram = planogram ?: Planogram(
                    id = planogramId,
                    name = "Planogram ${planogramId.takeLast(5)}",
                    section = null,
                    storeId = null,
                    shelvesCount = shelvesCount,
                    shelfWidthMm = 1000,
                    shelfHeightMm = null,
                    imageUrl = null,
                    createdAt = null
                )
                // Build planogram items from cell assignments. Each cell corresponds to a segment of the shelf.
                val items = mutableListOf<PlanogramItem>()
                for (r in 0 until shelvesCount) {
                    for (c in 0 until columns) {
                        val idx = r * columns + c
                        val prodId = cells[idx].productId ?: continue
                        // Convert column index to approximate x offset in millimetres
                        val xMm = (c.toFloat() / columns * newPlanogram.shelfWidthMm).toInt()
                        val widthMm = (1f / columns * newPlanogram.shelfWidthMm).toInt()
                        items.add(
                            PlanogramItem(
                                id = "item-$idx-${System.currentTimeMillis()}",
                                planogramId = planogramId,
                                productId = prodId,
                                shelfIndex = r,
                                xMm = xMm,
                                widthMm = widthMm,
                                facings = 1,
                                notes = null
                            )
                        )
                    }
                }
                // Invoke save callback with the constructed planogram and items
                onSave(newPlanogram, items)
            }) {
                Text(text = "Save Planogram")
            }
            Button(onClick = { onCancel() }) {
                Text(text = "Cancel")
            }
        }
    }
}