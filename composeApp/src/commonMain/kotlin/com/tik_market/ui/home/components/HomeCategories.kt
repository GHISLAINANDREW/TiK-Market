package com.tik_market.ui.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tik_market.theme.CardWhite
import com.tik_market.theme.DividerGray
import com.tik_market.theme.TextSecondary
import com.tik_market.utils.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCategories(
    categories: List<String>,
    selectedCategory: String?,
    onCategoryClick: (String?) -> Unit
) {
    val s = LocalAppStrings.current
    val primary = MaterialTheme.colorScheme.primary

    if (categories.isEmpty()) return

    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val allCats = listOf(s.allCategories) + categories
            allCats.forEach { cat ->
                val isSelected = (cat == s.allCategories && selectedCategory == null) || cat == selectedCategory
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryClick(if (cat == s.allCategories) null else cat) },
                    label = { Text(cat, style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primary,
                        selectedLabelColor = Color.White,
                        containerColor = CardWhite,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) primary else DividerGray
                    )
                )
            }
        }
    }
}
