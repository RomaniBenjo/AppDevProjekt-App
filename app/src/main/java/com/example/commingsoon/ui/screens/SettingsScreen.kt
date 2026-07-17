package com.example.commingsoon.ui.screens


import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.commingsoon.viewmodels.SettingsViewModel
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commingsoon.R
import com.example.commingsoon.language.AppLanguage
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.language.appString
import com.example.commingsoon.notifications.NotificationsHelper
import com.example.commingsoon.ui.theme.AppThemeType
import com.example.commingsoon.ui.theme.AppThemeViewModel
import java.time.LocalTime
import kotlin.collections.forEach

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun SettingsScreen(
    themeViewModel: AppThemeViewModel,
    languageViewModel: AppLanguageViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current

    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        item {
            Text(
                text = appString(R.string.choose_language),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(10.dp))

            LanguageSelection(
                selected = languageViewModel.currentLanguage,
                onSelected = languageViewModel::updateLanguage,
                allLanguages = languageViewModel.getLanguages()
            )
        }

        item {
            LightDarkSwitch(
                isDarkMode = themeViewModel.isDarkMode(),
                onChanged = {
                    themeViewModel.updateMode(it)
                }
            )
        }

        item {
            Text(
                text = appString(R.string.choose_theme),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(10.dp))

            ThemeGrid(
                selected = themeViewModel.currentTheme,
                onSelected = themeViewModel::updateTheme,
                allThemes = themeViewModel.getAllThemes(),
                viewModel = themeViewModel
            )
        }

        item {
            Text(
                text = appString(R.string.choose_notification),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(10.dp))

            JourneyNotificationSetting(
                enabled = settingsViewModel.isJourneyReminderEnabled(),
                reminderTime = settingsViewModel.getReminderTime(),
                onEnabledChanged = settingsViewModel::updateJourneyReminderEnabled,
                onTimeClicked = {
                    showTimePicker = true
                }
//                onConfirm = { time ->
//                    viewModel.updateReminderTime(time)
//                    showTimePicker = false
//                }
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(58.dp),
                    onClick = { NotificationsHelper(context).showTestNotification() }
                ) {
                    Text(appString(R.string.test_notification))
                }
            }
        }
    }

    if (showTimePicker) {
        ReminderTimePicker(
            initialTime = settingsViewModel.getReminderTime(),
            onDismiss = {
                showTimePicker = false
            },
            onConfirm = { time ->
                settingsViewModel.updateReminderTime(time)
                showTimePicker = false
            }
        )
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

                Text(language.displayName)
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
            Text(appString(R.string.light_mode))
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
            Text(appString(R.string.dark_mode))
        }
    }
}

@Composable
fun ThemeGrid(
    selected: AppThemeType,
    onSelected: (AppThemeType) -> Unit,
    allThemes: List<AppThemeType>,
    viewModel: AppThemeViewModel
) {
    val rows = allThemes.chunked(2)

    val configuration = LocalConfiguration.current
    val fontSize = (configuration.screenWidthDp * 0.035f).sp

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
                        },
                        fontSize = fontSize
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
    viewModel: AppThemeViewModel,
    selected: Boolean,
    onClick: () -> Unit,
    fontSize: TextUnit
) {
    val definition = viewModel.getThemeDefinition(theme)

    val colorScheme =
        if (viewModel.isDarkMode())
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
                    text = appString(viewModel.getThemeName(theme)),
                    fontSize = fontSize,
                    color = colorScheme.background
//                    maxLines = 2,
//                    softWrap = false,
//                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun JourneyNotificationSetting(
    enabled: Boolean,
    reminderTime: LocalTime,
    onEnabledChanged: (Boolean) -> Unit,
    onTimeClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Linke Seite
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = appString(R.string.journey_notification),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = appString(R.string.journey_notification_description),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(Modifier.width(16.dp))

        // Rechte Seite
        Row(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(50.dp)
                .clip(RoundedCornerShape(50))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(50)
                ),
            horizontalArrangement = Arrangement.Center
        ) {
            val selectedColor = MaterialTheme.colorScheme.secondary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (!enabled) selectedColor
                        else Color.Transparent
                    )
                    .clickable {
                        onEnabledChanged(false)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(appString(R.string.never))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (enabled) selectedColor
                        else Color.Transparent
                    )
                    .clickable {
                        onEnabledChanged(true)
                        onTimeClicked()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%02d:%02d", reminderTime.hour, reminderTime.minute)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePicker(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {

    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = appString(R.string.choose_notification_time),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(24.dp))

                TimePicker(state = state)

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(appString(R.string.cancel)) }

                    Spacer(Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            onConfirm(LocalTime.of( state.hour, state.minute))
                        }
                    ) {
                        Text(appString(R.string.ok))
                    }
                }
            }
        }
    }
}