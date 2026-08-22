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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

fun GloveboxRoute.getVehicleId(): Long? {
    return when (this) {
        is GloveboxRoute.History -> vehicleId
        is GloveboxRoute.MyParts -> vehicleId
        is GloveboxRoute.Reminders -> vehicleId
        is GloveboxRoute.Insights -> vehicleId
        is GloveboxRoute.DigitalGlovebox -> vehicleId
        is GloveboxRoute.VehicleProfile -> vehicleId.takeIf { it != 0L }
        is GloveboxRoute.AddServiceLog -> vehicleId
        is GloveboxRoute.AddFuelLog -> vehicleId
        is GloveboxRoute.AddDocument -> vehicleId
        else -> null
    }
}

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
    val contextVehicleId = currentRoute.getVehicleId() ?: 0L
    val activeVehicle = remember(contextVehicleId, vehicles) {
        vehicles.find { it.id == contextVehicleId }
    }
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
        if (hasVehicles && contextVehicleId != 0L && activeVehicle != null) GloveboxNavItem(
            label = activeVehicle.nickname ?: "${activeVehicle.make} ${activeVehicle.model}",
            icon = Icons.Rounded.DirectionsCar,
            route = GloveboxRoute.History(contextVehicleId), // Context parent
            isSubItem = true,
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.History(contextVehicleId))
            }
        ) else null,
        if (hasVehicles && contextVehicleId != 0L) GloveboxNavItem(
            label = stringResource(R.string.history),
            icon = Icons.Rounded.History,
            route = GloveboxRoute.History(contextVehicleId),
            isDeepSubItem = true,
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.History(contextVehicleId))
            }
        ) else null,
        if (hasVehicles && contextVehicleId != 0L) GloveboxNavItem(
            label = stringResource(R.string.nav_parts),
            icon = Icons.Rounded.Build,
            route = GloveboxRoute.MyParts(contextVehicleId),
            isDeepSubItem = true,
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.MyParts(contextVehicleId))
            }
        ) else null,
        if (hasVehicles && contextVehicleId != 0L) GloveboxNavItem(
            label = stringResource(R.string.nav_reminders),
            icon = Icons.Rounded.Notifications,
            route = GloveboxRoute.Reminders(contextVehicleId),
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.Reminders(contextVehicleId))
            }
        ) else null,
        GloveboxNavItem(
            label = stringResource(R.string.nav_insights),
            icon = Icons.Rounded.BarChart,
            route = GloveboxRoute.Insights(contextVehicleId),
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.Insights(contextVehicleId))
            }
        ),
        if (hasVehicles && contextVehicleId != 0L) GloveboxNavItem(
            label = stringResource(R.string.nav_glovebox),
            icon = Icons.Rounded.Folder,
            route = GloveboxRoute.DigitalGlovebox(contextVehicleId),
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.DigitalGlovebox(contextVehicleId))
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

                    is GloveboxRoute.MyParts -> NavEntry(key) {
                        MyPartsScreen(
                            vehicleId = key.vehicleId,
                            onOpenDrawer = onOpenDrawer
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
    val primaryItems = navigationItems.filter { it.route !is GloveboxRoute.Settings }
    val secondaryItems = navigationItems.filter { it.route is GloveboxRoute.Settings }

    val drawerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
        ) {
            DrawerHeader()
            
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                primaryItems.forEach { item ->
                    val isSelected = item.isRouteSelected(currentRoute)
                    val indentation = when {
                        item.isDeepSubItem -> 44.dp
                        item.isSubItem -> 24.dp
                        else -> 8.dp
                    }
                    val iconIndentation = when {
                        item.isDeepSubItem -> 32.dp
                        item.isSubItem -> 16.dp
                        else -> 0.dp
                    }

                    NavigationDrawerItem(
                        label = { 
                            Text(
                                item.label, 
                                modifier = Modifier.padding(start = indentation),
                                style = when {
                                    item.isDeepSubItem -> MaterialTheme.typography.bodySmall
                                    item.isSubItem -> MaterialTheme.typography.bodyMedium
                                    else -> MaterialTheme.typography.labelLarge
                                }
                            ) 
                        },
                        selected = isSelected,
                        onClick = item.onClick,
                        icon = { 
                            Icon(
                                item.icon, 
                                contentDescription = null, 
                                modifier = Modifier
                                    .padding(start = iconIndentation)
                                    .size(if (item.isSubItem || item.isDeepSubItem) 18.dp else 22.dp)
                            ) 
                        },
                        modifier = Modifier.padding(
                            horizontal = 12.dp, 
                            vertical = if (item.isSubItem || item.isDeepSubItem) 1.dp else 2.dp
                        ),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = when {
                                item.isDeepSubItem -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                item.isSubItem -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            selectedIconColor = when {
                                item.isDeepSubItem -> MaterialTheme.colorScheme.onTertiaryContainer
                                item.isSubItem -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            selectedTextColor = when {
                                item.isDeepSubItem -> MaterialTheme.colorScheme.onTertiaryContainer
                                item.isSubItem -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (item.isSubItem || item.isDeepSubItem) 0.6f else 1f
                            ),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (item.isSubItem || item.isDeepSubItem) 0.6f else 1f
                            )
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                }
                
                if (secondaryItems.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    secondaryItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { 
                                Text(
                                    item.label, 
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.labelLarge
                                ) 
                            },
                            selected = item.isRouteSelected(currentRoute),
                            onClick = item.onClick,
                            icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
            
            DrawerFooter()
        }
    }

    if (isExpanded) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier
                        .width(260.dp)
                        .padding(end = 1.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    drawerContent()
                }
            },
            content = content
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerShape = MaterialTheme.shapes.large
                ) {
                    drawerContent()
                }
            },
            content = content
        )
    }
}

@Composable
fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
            .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    letterSpacing = (-0.5).sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = "AUTOMOTIVE HUB",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun DrawerFooter() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Version $versionName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "Help & Feedback",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { 
                    uriHandler.openUri("https://github.com/nhaskaris/GloveBox/issues")
                }
            )
        }
    }
}

data class GloveboxNavItem(
    val label: String,
    val icon: ImageVector,
    val route: GloveboxRoute,
    val isSubItem: Boolean = false,
    val isDeepSubItem: Boolean = false,
    val onClick: () -> Unit
) {
    fun isRouteSelected(currentRoute: GloveboxRoute): Boolean {
        return when (route) {
            is GloveboxRoute.VehicleList -> currentRoute is GloveboxRoute.VehicleList || currentRoute is GloveboxRoute.VehicleProfile
            is GloveboxRoute.History -> currentRoute is GloveboxRoute.History || currentRoute is GloveboxRoute.AddServiceLog || currentRoute is GloveboxRoute.AddFuelLog
            is GloveboxRoute.Reminders -> currentRoute is GloveboxRoute.Reminders
            is GloveboxRoute.Insights -> currentRoute is GloveboxRoute.Insights
            is GloveboxRoute.DigitalGlovebox -> currentRoute is GloveboxRoute.DigitalGlovebox || currentRoute is GloveboxRoute.AddDocument
            is GloveboxRoute.MyParts -> currentRoute is GloveboxRoute.MyParts
            is GloveboxRoute.BuyChecklist -> currentRoute is GloveboxRoute.BuyChecklist || currentRoute is GloveboxRoute.ProspectForm || currentRoute is GloveboxRoute.ProspectComparison
            is GloveboxRoute.Settings -> currentRoute is GloveboxRoute.Settings
            else -> false
        }
    }
}
