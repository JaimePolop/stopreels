package com.stopreels.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelDetectionTest {
    private val screen = ScreenRect(0, 0, 1080, 2400)

    @Test
    fun reelInFeedIsBlocked() {
        val root = node(
            children = listOf(
                node(contentDescription = "Reel de @ana", bounds = ScreenRect(40, 400, 1040, 1_200)),
                node(text = "Me gusta"),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertTrue(result.shouldBlock)
        assertTrue(result.targets.any { it.kind == BlockTargetKind.FEED_REEL })
    }

    @Test
    fun reelsTabWithVideoIsBlockedAsSurface() {
        val root = node(
            children = listOf(
                node(text = "Reels", bounds = ScreenRect(40, 80, 230, 150)),
                node(
                    contentDescription = "Reel en reproducción",
                    className = "android.view.SurfaceView",
                    bounds = ScreenRect(0, 200, 1080, 2_200),
                ),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertTrue(result.shouldBlock)
        assertTrue(result.targets.single().kind == BlockTargetKind.REELS_SURFACE)
        assertTrue(result.screen == InstagramScreen.REELS)
    }

    @Test
    fun reelsNavigationLabelAloneIsNotBlocked() {
        val root = node(
            children = listOf(
                node(text = "Reels", bounds = ScreenRect(900, 2_200, 1_020, 2_360)),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertFalse(result.shouldBlock)
    }

    @Test
    fun sharedReelIsNotBlockedEvenWhenViewerIsFullScreen() {
        val root = node(
            children = listOf(
                node(
                    contentDescription = "Reel compartido por Ana",
                    className = "android.view.SurfaceView",
                    bounds = ScreenRect(0, 200, 1080, 2_200),
                ),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertFalse(result.shouldBlock)
        assertTrue(result.isPrivateContext)
    }

    @Test
    fun sharedPhotoInPrivateConversationIsNotBlocked() {
        val root = node(
            children = listOf(
                node(text = "Direct"),
                node(text = "Conversación con Ana"),
                node(className = "android.widget.EditText", contentDescription = "Escribe un mensaje"),
                node(contentDescription = "Foto compartida por Ana", bounds = ScreenRect(80, 500, 1_000, 1_500)),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertFalse(result.shouldBlock)
        assertTrue(result.isPrivateContext)
    }

    @Test
    fun normalPhotoInFeedIsNotBlocked() {
        val root = node(
            children = listOf(
                node(
                    contentDescription = "Foto de Ana",
                    className = "android.widget.ImageView",
                    bounds = ScreenRect(40, 400, 1_040, 1_400),
                ),
            ),
        )

        assertFalse(ReelDetectionEngine.detect(root, DetectionPreferences()).shouldBlock)
    }

    @Test
    fun videoSharedInPrivateMessageIsNotBlocked() {
        val root = node(
            children = listOf(
                node(text = "Direct"),
                node(text = "Conversación con Ana"),
                node(className = "android.widget.EditText", contentDescription = "Escribe un mensaje"),
                node(contentDescription = "Reel compartido por Ana", bounds = ScreenRect(80, 500, 1_000, 1_500)),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertFalse(result.shouldBlock)
        assertTrue(result.isPrivateContext)
        assertTrue(result.screen == InstagramScreen.DIRECT)
    }

    @Test
    fun sharedReelWithPartialDirectHierarchyIsNotBlocked() {
        // Some Instagram versions expose only the message container id and the
        // media node when a shared Reel is opened from a conversation.
        val root = node(
            children = listOf(
                node(viewId = "com.instagram.android:id/thread_message_container"),
                node(
                    viewId = "com.instagram.android:id/shared_media",
                    contentDescription = "Reel de Ana",
                    className = "android.view.SurfaceView",
                    bounds = ScreenRect(0, 200, 1080, 2_200),
                ),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertFalse(result.shouldBlock)
        assertTrue(result.isPrivateContext)
    }

    @Test
    fun sharedPhotoWithMessageResourceIdIsNotBlocked() {
        val root = node(
            children = listOf(
                node(viewId = "com.instagram.android:id/message_thread"),
                node(
                    viewId = "com.instagram.android:id/message_media",
                    contentDescription = "Photo de Ana",
                    className = "android.widget.ImageView",
                    bounds = ScreenRect(80, 500, 1_000, 1_500),
                ),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertFalse(result.shouldBlock)
        assertTrue(result.isPrivateContext)
    }

    @Test
    fun sharedEnglishMediaLabelIsNotBlockedWithoutConversationTitle() {
        val root = node(
            children = listOf(
                node(contentDescription = "Shared Reel by Ana", bounds = ScreenRect(80, 500, 1_000, 1_500)),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertFalse(result.shouldBlock)
        assertTrue(result.isPrivateContext)
    }

    @Test
    fun globalDirectNavigationLabelsDoNotDisableFeedBlocking() {
        val root = node(
            children = listOf(
                node(text = "Direct"),
                node(text = "Mensajes"),
                node(contentDescription = "Reel de @ana", bounds = ScreenRect(40, 400, 1040, 1_200)),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertTrue(result.shouldBlock)
        assertFalse(result.isPrivateContext)
    }

    @Test
    fun unrelatedEditTextDoesNotMakeFeedLookLikePrivateChat() {
        val root = node(
            children = listOf(
                node(text = "Mensajes"),
                node(className = "android.widget.EditText", hintText = "Buscar"),
                node(contentDescription = "Reel de @ana", bounds = ScreenRect(40, 400, 1040, 1_200)),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertTrue(result.shouldBlock)
        assertFalse(result.isPrivateContext)
    }

    @Test
    fun searchResultsWithReelsAreNotBlocked() {
        val root = node(
            children = listOf(
                node(
                    className = "android.widget.EditText",
                    hintText = "Buscar",
                    viewId = "com.instagram.android:id/search_bar",
                    text = "Ana",
                ),
                node(text = "Personas"),
                node(contentDescription = "Reel de Ana", bounds = ScreenRect(40, 400, 1_040, 1_200)),
            ),
        )

        assertFalse(ReelDetectionEngine.detect(root, DetectionPreferences()).shouldBlock)
    }

    @Test
    fun searchScreenWithSearchInputAndIdIsNotBlocked() {
        val root = node(
            children = listOf(
                node(
                    className = "android.widget.EditText",
                    viewId = "com.instagram.android:id/search_bar",
                    text = "Ana",
                ),
                node(contentDescription = "Reel de Ana", bounds = ScreenRect(40, 400, 1_040, 1_200)),
            ),
        )

        assertFalse(ReelDetectionEngine.detect(root, DetectionPreferences()).shouldBlock)
    }

    @Test
    fun searchScreenWithGenericSearchBarIsNotBlocked() {
        val root = node(
            children = listOf(
                node(viewId = "com.instagram.android:id/search_bar"),
                node(contentDescription = "Reel de Ana", bounds = ScreenRect(40, 400, 1_040, 1_200)),
            ),
        )

        assertFalse(ReelDetectionEngine.detect(root, DetectionPreferences()).shouldBlock)
    }

    @Test
    fun feedReelUsesFullScreenOverlayBounds() {
        val card = ScreenRect(40, 360, 1040, 1_480)
        val root = node(
            children = listOf(
                node(
                    bounds = card,
                    children = listOf(
                        node(contentDescription = "Reel de @ana", bounds = ScreenRect(80, 420, 240, 500)),
                    ),
                ),
            ),
        )

        val result = ReelDetectionEngine.detect(root, DetectionPreferences())

        assertTrue(result.shouldBlock)
        assertTrue(result.targets.first().bounds == screen)
    }

    @Test
    fun normalPostIsNotBlocked() {
        val root = node(
            children = listOf(
                node(text = "Me gusta"),
                node(text = "Comentar"),
                node(contentDescription = "Vídeo de @ana", className = "android.view.SurfaceView"),
            ),
        )

        assertFalse(ReelDetectionEngine.detect(root, DetectionPreferences()).shouldBlock)
    }

    @Test
    fun storyIsNotBlockedByDefaultButCanBeEnabled() {
        val root = node(children = listOf(node(text = "Historias"), node(contentDescription = "Historia de Ana")))

        assertFalse(ReelDetectionEngine.detect(root, DetectionPreferences()).shouldBlock)
        assertTrue(
            ReelDetectionEngine.detect(
                root,
                DetectionPreferences(blockStories = true),
            ).shouldBlock,
        )
    }

    @Test
    fun changingFeedReelKeepsStableOverlayKey() {
        val first = ReelDetectionEngine.detect(
            node(children = listOf(node(contentDescription = "Reel de Ana", bounds = ScreenRect(80, 300, 800, 900)))),
            DetectionPreferences(),
        )
        val second = ReelDetectionEngine.detect(
            node(children = listOf(node(contentDescription = "Reel de Bruno", bounds = ScreenRect(80, 500, 800, 1_100)))),
            DetectionPreferences(),
        )

        assertTrue(first.stableKey == second.stableKey)
    }

    @Test
    fun changingReelLabelInSameRegionKeepsStableOverlayKey() {
        val first = ReelDetectionEngine.detect(
            node(children = listOf(node(contentDescription = "Reel de Ana", bounds = ScreenRect(0, 300, 1080, 1_200)))),
            DetectionPreferences(),
        )
        val second = ReelDetectionEngine.detect(
            node(children = listOf(node(contentDescription = "Reel de Bruno", bounds = ScreenRect(0, 300, 1080, 1_200)))),
            DetectionPreferences(),
        )

        assertTrue(first.shouldBlock)
        assertTrue(second.shouldBlock)
        assertTrue(first.stableKey == second.stableKey)
    }

    @Test
    fun disablingBlockingReturnsNoTargets() {
        val root = node(children = listOf(node(contentDescription = "Reel de Ana", bounds = screen)))

        val result = ReelDetectionEngine.detect(root, DetectionPreferences(blockingEnabled = false))

        assertFalse(result.shouldBlock)
    }

    @Test
    fun reelsNavigationDoesNotCreateShortcutOverlay() {
        val shortcut = node(text = "Reels", bounds = ScreenRect(700, 2_200, 900, 2_400))

        assertFalse(
            ReelDetectionEngine.detect(
                node(children = listOf(shortcut)),
                DetectionPreferences(blockShortcuts = true),
            ).shouldBlock,
        )
    }

    private fun node(
        text: String? = null,
        contentDescription: String? = null,
        viewId: String? = null,
        className: String? = null,
        bounds: ScreenRect = screen,
        children: List<AccessibilityNodeSnapshot> = emptyList(),
        hintText: String? = null,
    ) = AccessibilityNodeSnapshot(text, contentDescription, viewId, className, bounds, true, children, hintText)
}
