package com.eliteonetube.glovebox.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

/**
 * A [Scene] that displays a list and a detail [NavEntry] side-by-side.
 */
class ListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val listEntry: NavEntry<T>,
    val detailEntry: NavEntry<T>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(listEntry, detailEntry)
    override val content: @Composable (() -> Unit) = {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.4f)) {
                listEntry.Content()
            }

            // Let the detail entry know not to display a back button.
            CompositionLocalProvider(LocalBackButtonVisibility provides false) {
                Column(modifier = Modifier.weight(0.6f)) {
                    AnimatedContent(
                        targetState = detailEntry,
                        label = "detail_pane_transition",
                        contentKey = { entry -> entry.contentKey },
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it }
                            ) togetherWith
                                    slideOutHorizontally(targetOffsetX = { -it })
                        }
                    ) { entry ->
                        entry.Content()
                    }
                }
            }
        }
    }

    companion object {
        const val LIST_PANE_KEY = "is_list_pane"
        const val DETAIL_PANE_KEY = "is_detail_pane"

        fun listPane() = mapOf(LIST_PANE_KEY to true)
        fun detailPane() = mapOf(DETAIL_PANE_KEY to true)
    }
}

/**
 * This `CompositionLocal` can be used by a detail `NavEntry` to decide whether to display
 * a back button. Default is `true`. It is set to `false` for a detail `NavEntry` when being
 * displayed in a `ListDetailScene`.
 */
val LocalBackButtonVisibility = compositionLocalOf { true }

@Composable
fun <T : Any> rememberListDetailSceneStrategy(): ListDetailSceneStrategy<T> {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val windowSizeClass = adaptiveInfo.windowSizeClass

    return remember(windowSizeClass) {
        ListDetailSceneStrategy(windowSizeClass)
    }
}

/**
 * A [SceneStrategy] that returns a [ListDetailScene] if:
 *
 * - the window width is over 600dp (Medium/Expanded)
 * - A `Detail` entry is the last item in the back stack
 * - A `List` entry is in the back stack
 */
class ListDetailSceneStrategy<T : Any>(val windowSizeClass: WindowSizeClass) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (!windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            return null
        }

        val detailEntry =
            entries.lastOrNull()?.takeIf { it.metadata[ListDetailScene.DETAIL_PANE_KEY] == true }
                ?: return null
        val listEntry =
            entries.findLast { it.metadata[ListDetailScene.LIST_PANE_KEY] == true } ?: return null

        val sceneKey = listEntry.contentKey

        return ListDetailScene(
            key = sceneKey,
            previousEntries = entries.dropLast(1),
            listEntry = listEntry,
            detailEntry = detailEntry
        )
    }
}
