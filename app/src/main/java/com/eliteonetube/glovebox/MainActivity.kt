package com.eliteonetube.glovebox

import android.Manifest
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.eliteonetube.glovebox.navigation.GloveboxRoute
import com.eliteonetube.glovebox.ui.screens.*
import com.eliteonetube.glovebox.ui.theme.GloveboxTheme
import com.eliteonetube.glovebox.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.eliteonetube.glovebox.navigation.ListDetailScene
import com.eliteonetube.glovebox.navigation.rememberListDetailSceneStrategy

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
            val context = LocalContext.current
            
            // Apply language change using AppCompatDelegate
            LaunchedEffect(appLanguage) {
                val appLocales = if (appLanguage == null) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(appLanguage)
                }
                
                if (AppCompatDelegate.getApplicationLocales() != appLocales) {
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }

            // Create a localized context for string extraction
            val configuration = LocalConfiguration.current
            val localizedContext = remember(context, appLanguage) {
                val locale = if (appLanguage != null) Locale.forLanguageTag(appLanguage!!) else Locale.getDefault()
                val config = Configuration(configuration)
                config.setLocale(locale)
                val configContext = context.createConfigurationContext(config)

                object : ContextWrapper(context) {
                    override fun getResources() = configContext.resources
                    override fun getAssets() = configContext.assets
                }
            }

            val backStack = rememberNavBackStack(GloveboxRoute.VehicleList)

            // CRITICAL: We MUST provide all owners from the Activity to the Localized Context
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLifecycleOwner provides (context as LifecycleOwner),
                LocalViewModelStoreOwner provides (context as ViewModelStoreOwner),
                LocalSavedStateRegistryOwner provides (context as SavedStateRegistryOwner),
                LocalActivityResultRegistryOwner provides (context as ActivityResultRegistryOwner),
                LocalOnBackPressedDispatcherOwner provides (context as androidx.activity.OnBackPressedDispatcherOwner),
                LocalNavigationEventDispatcherOwner provides (context as NavigationEventDispatcherOwner)
            ) {
                GloveboxTheme(
                    themePreference = themePreference,
                    dynamicColor = false
                ) {
                    MainContent(viewModel, appLanguage, backStack)
                }
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MainViewModel, appLanguage: String?, backStack: NavBackStack<NavKey>) {
    val activeVehicleId by viewModel.activeVehicleId.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    )
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(isOnboardingCompleted) {
        if (isOnboardingCompleted == false) {
            backStack.add(GloveboxRoute.Onboarding)
        }
    }

    if (isOnboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentRoute = backStack.lastOrNull() as? GloveboxRoute ?: GloveboxRoute.VehicleList
    val effectiveVehicleId = activeVehicleId ?: 0L
    val hasVehicles = vehicles.isNotEmpty()

    val navigationItems = listOfNotNull(
        GloveboxNavItem(
            label = stringResource(R.string.nav_garage),
            icon = Icons.Rounded.DirectionsCar,
            route = GloveboxRoute.VehicleList,
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.VehicleList)
            }
        ),
        if (hasVehicles) GloveboxNavItem(
            label = stringResource(R.string.nav_reminders),
            icon = Icons.Rounded.Notifications,
            route = GloveboxRoute.Reminders(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    scope.launch { drawerState.close() }
                    backStack.clear()
                    backStack.add(GloveboxRoute.Reminders(effectiveVehicleId))
                }
            }
        ) else null,
        if (hasVehicles) GloveboxNavItem(
            label = stringResource(R.string.nav_insights),
            icon = Icons.Rounded.BarChart,
            route = GloveboxRoute.Insights(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    scope.launch { drawerState.close() }
                    backStack.clear()
                    backStack.add(GloveboxRoute.Insights(effectiveVehicleId))
                }
            }
        ) else null,
        if (hasVehicles) GloveboxNavItem(
            label = stringResource(R.string.nav_glovebox),
            icon = Icons.Rounded.Folder,
            route = GloveboxRoute.DigitalGlovebox(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    scope.launch { drawerState.close() }
                    backStack.clear()
                    backStack.add(GloveboxRoute.DigitalGlovebox(effectiveVehicleId))
                }
            }
        ) else null,
        GloveboxNavItem(
            label = stringResource(R.string.nav_buying_guide),
            icon = Icons.Rounded.Checklist,
            route = GloveboxRoute.BuyChecklist,
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.BuyChecklist)
            }
        ),
        GloveboxNavItem(
            label = stringResource(R.string.nav_settings),
            icon = Icons.Rounded.Settings,
            route = GloveboxRoute.Settings,
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.Settings)
            }
        )
    )

    val onOpenDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val onNavigateBack: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    NavigationWrapper(
        isExpanded = isExpanded,
        drawerState = drawerState,
        navigationItems = navigationItems,
        currentRoute = currentRoute
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = onNavigateBack,
                sceneStrategy = listDetailStrategy,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                )
            ) { key ->
                when (key) {
                    is GloveboxRoute.Onboarding -> NavEntry(key) {
                        OnboardingScreen(
                            onComplete = {
                                viewModel.setOnboardingCompleted()
                                backStack.removeAt(backStack.size - 1)
                            },
                            currentLanguage = appLanguage,
                            onLanguageChange = { lang: String? -> viewModel.setAppLanguage(lang) },
                            unitSystem = viewModel.unitSystem.collectAsStateWithLifecycle().value,
                            onUnitChange = viewModel::setUnitSystem
                        )
                    }

                    is GloveboxRoute.VehicleList -> NavEntry(
                        key = key,
                        metadata = ListDetailScene.listPane()
                    ) {
                        VehicleListScreen(
                            onSelectVehicle = { id ->
                                viewModel.setActiveVehicleId(id)
                                // If already at history for this vehicle, don't add again
                                if (backStack.lastOrNull() !is GloveboxRoute.History || (backStack.lastOrNull() as? GloveboxRoute.History)?.vehicleId != id) {
                                    backStack.add(GloveboxRoute.History(id))
                                }
                            },
                            onAddVehicle = { backStack.add(GloveboxRoute.VehicleProfile()) },
                            onEditVehicle = { id -> backStack.add(GloveboxRoute.VehicleProfile(id)) },
                            onOpenDrawer = onOpenDrawer
                        )
                    }

                    is GloveboxRoute.VehicleProfile -> NavEntry(key) {
                        VehicleProfileScreen(
                            vehicleId = key.vehicleId,
                            onNavigateBack = onNavigateBack
                        )
                    }

                    is GloveboxRoute.History -> NavEntry(
                        key = key,
                        metadata = ListDetailScene.listPane() + ListDetailScene.detailPane()
                    ) {
                        HistoryScreen(
                            vehicleId = key.vehicleId,
                            onAddRecord = { backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId)) },
                            onEditRecord = { id -> backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId, id)) },
                            onAddFuel = { backStack.add(GloveboxRoute.AddFuelLog(key.vehicleId)) },
                            onEditFuel = { id -> backStack.add(GloveboxRoute.AddFuelLog(key.vehicleId, id)) },
                            onOpenDrawer = onOpenDrawer
                        )
                    }

                    is GloveboxRoute.AddServiceLog -> NavEntry(
                        key = key,
                        metadata = ListDetailScene.detailPane()
                    ) {
                        AddServiceLogScreen(
                            vehicleId = key.vehicleId,
                            recordId = key.recordId,
                            prefilledType = key.prefilledType,
                            onNavigateBack = onNavigateBack
                        )
                    }

                    is GloveboxRoute.Reminders -> NavEntry(key) {
                        RemindersScreen(
                            vehicleId = key.vehicleId,
                            onLogService = { type ->
                                backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId, prefilledType = type))
                            },
                            onOpenDrawer = onOpenDrawer
                        )
                    }

                    is GloveboxRoute.Insights -> NavEntry(key) {
                        InsightsScreen(
                            vehicleId = key.vehicleId,
                            onOpenDrawer = onOpenDrawer
                        )
                    }

                    is GloveboxRoute.AddFuelLog -> NavEntry(
                        key = key,
                        metadata = ListDetailScene.detailPane()
                    ) {
                        AddFuelLogScreen(
                            vehicleId = key.vehicleId,
                            logId = key.logId,
                            onNavigateBack = onNavigateBack
                        )
                    }

                    is GloveboxRoute.DigitalGlovebox -> NavEntry(key) {
                        DigitalGloveboxScreen(
                            vehicleId = key.vehicleId,
                            onAddDocument = { backStack.add(GloveboxRoute.AddDocument(key.vehicleId)) },
                            onViewDocument = { id -> backStack.add(GloveboxRoute.AddDocument(key.vehicleId, id)) },
                            onOpenDrawer = onOpenDrawer
                        )
                    }

                    is GloveboxRoute.AddDocument -> NavEntry(key) {
                        AddDocumentScreen(
                            vehicleId = key.vehicleId,
                            docId = key.docId,
                            onNavigateBack = onNavigateBack
                        )
                    }

                    is GloveboxRoute.BuyChecklist -> NavEntry(key) {
                        ProspectListScreen(
                            onAddProspect = { backStack.add(GloveboxRoute.ProspectForm()) },
                            onViewProspect = { id -> backStack.add(GloveboxRoute.ProspectForm(id)) },
                            onCompareProspects = { ids -> backStack.add(GloveboxRoute.ProspectComparison(ids)) },
                            onOpenDrawer = onOpenDrawer
                        )
                    }

                    is GloveboxRoute.ProspectComparison -> NavEntry(key) {
                        ProspectComparisonScreen(
                            prospectIds = key.prospectIds,
                            onNavigateBack = onNavigateBack
                        )
                    }

                    is GloveboxRoute.ProspectForm -> NavEntry(key) {
                        ProspectFormScreen(
                            prospectId = key.prospectId,
                            onNavigateBack = onNavigateBack,
                            onPromoted = { newId ->
                                viewModel.setActiveVehicleId(newId)
                                backStack.clear()
                                backStack.add(GloveboxRoute.History(newId))
                            }
                        )
                    }

                    is GloveboxRoute.Settings -> NavEntry(key) {
                        SettingsScreen(onOpenDrawer = onOpenDrawer)
                    }

                    else -> error("Unknown route")
                }
            }
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.size - 1)
    }
}

@Composable
fun NavigationWrapper(
    isExpanded: Boolean,
    drawerState: DrawerState,
    navigationItems: List<GloveboxNavItem>,
    currentRoute: GloveboxRoute,
    content: @Composable () -> Unit
) {
    if (isExpanded) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier
                        .width(240.dp)
                        .statusBarsPadding()
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(12.dp))
                        navigationItems.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                selected = item.isRouteSelected(currentRoute),
                                onClick = item.onClick,
                                icon = { Icon(item.icon, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            },
            content = content
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.statusBarsPadding()
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(12.dp))
                        navigationItems.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                selected = item.isRouteSelected(currentRoute),
                                onClick = item.onClick,
                                icon = { Icon(item.icon, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            },
            content = content
        )
    }
}

data class GloveboxNavItem(
    val label: String,
    val icon: ImageVector,
    val route: GloveboxRoute,
    val onClick: () -> Unit
) {
    fun isRouteSelected(currentRoute: GloveboxRoute): Boolean {
        return when (route) {
            is GloveboxRoute.VehicleList -> currentRoute is GloveboxRoute.VehicleList || currentRoute is GloveboxRoute.VehicleProfile
            is GloveboxRoute.History -> currentRoute is GloveboxRoute.History || currentRoute is GloveboxRoute.AddServiceLog || currentRoute is GloveboxRoute.AddFuelLog
            is GloveboxRoute.Reminders -> currentRoute is GloveboxRoute.Reminders
            is GloveboxRoute.Insights -> currentRoute is GloveboxRoute.Insights
            is GloveboxRoute.DigitalGlovebox -> currentRoute is GloveboxRoute.DigitalGlovebox || currentRoute is GloveboxRoute.AddDocument
            is GloveboxRoute.BuyChecklist -> currentRoute is GloveboxRoute.BuyChecklist || currentRoute is GloveboxRoute.ProspectForm || currentRoute is GloveboxRoute.ProspectComparison
            is GloveboxRoute.Settings -> currentRoute is GloveboxRoute.Settings
            else -> false
        }
    }
}
