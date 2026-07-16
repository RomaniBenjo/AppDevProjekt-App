package com.example.commingsoon.ui.screens


import android.R.attr.theme
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.commingsoon.viewmodels.SettingsViewModel
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commingsoon.R
import com.example.commingsoon.language.AppLanguage
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.ui.theme.AppThemeType
import com.example.commingsoon.ui.theme.AppThemeViewModel
import com.example.commingsoon.viewmodels.SettingsViewModelFactory
import kotlin.collections.forEach

@Composable
fun SettingsScreen(
    themeViewModel: AppThemeViewModel,
    languageViewModel: AppLanguageViewModel
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            languageViewModel,
            themeViewModel
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        item {
            Text(
                text = stringResource(R.string.choose_language),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(10.dp))

            LanguageSelection(
                selected = viewModel.getCurrentLanguage(),
                onSelected = viewModel::setLanguage,
                allLanguages = viewModel.getAllLanguages()
            )
        }

        item {
            LightDarkSwitch(
                isDarkMode = viewModel.getCurrentMode(),
                onChanged = {
                    viewModel.setDarkLightMode(it)
                }
            )
        }

        item {
            Text(
                text = stringResource(R.string.choose_theme),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(10.dp))

            ThemeGrid(
                selected = viewModel.getCurrentTheme(),
                onSelected = viewModel::setTheme,
                allThemes = viewModel.getAllThemes(),
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun LanguageSelection (
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    allLanguages: List<AppLanguage>
) {
    Column {
        allLanguages.forEach { language ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Log.d("Row", "Clicked: $language")
                        onSelected(language)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = language == selected,
                    onClick = { Log.d("RadioButton", "Clicked: $language")
                        onSelected(language) }
                )

                Text(language.name)
            }
        }
    }
}

@Composable
fun LightDarkSwitch(
    isDarkMode: Boolean,
    onChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(50))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(50)
            )
    ) {
        val selectedColor = MaterialTheme.colorScheme.secondary

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (!isDarkMode) selectedColor else Color.Transparent
                )
                .clickable {
                    onChanged(false)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.light_mode))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (isDarkMode) selectedColor else Color.Transparent
                )
                .clickable {
                    onChanged(true)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.dark_mode))
        }
    }
}

@Composable
fun ThemeGrid(
    selected: AppThemeType,
    onSelected: (AppThemeType) -> Unit,
    allThemes: List<AppThemeType>,
    viewModel: SettingsViewModel
) {
    val rows = allThemes.chunked(2)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { theme ->
                    ThemeCard(
                        modifier = Modifier.weight(1f),
                        theme = theme,
                        viewModel = viewModel,
                        selected = theme == selected,
                        onClick = {
                            onSelected(theme)
                        }
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ThemeCard(
    modifier: Modifier = Modifier,
    theme: AppThemeType,
    viewModel: SettingsViewModel,
    selected: Boolean,
    onClick: () -> Unit
) {
    val definition = viewModel.getThemeDefinition(theme)

    val colorScheme =
        if (viewModel.getCurrentMode())
            definition.colorScheme.light
        else
            definition.colorScheme.dark

    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.tertiary
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(definition.assets.headerShape),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = onClick
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(viewModel.getThemeName(theme)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.background
                )
            }
        }
    }
}