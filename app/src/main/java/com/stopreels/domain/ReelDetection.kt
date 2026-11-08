package com.stopreels.domain

import java.util.Locale

const val INSTAGRAM_PACKAGE = "com.instagram.android"

data class ScreenRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val area: Long get() = width.toLong() * height.toLong()

    fun contains(other: ScreenRect): Boolean =
        left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom

    fun inset(amount: Int): ScreenRect = ScreenRect(
        left + amount,
        top + amount,
        (right - amount).coerceAtLeast(left + 1),
        (bottom - amount).coerceAtLeast(top + 1),
    )

    fun expandTo(minWidth: Int, minHeight: Int, screen: ScreenRect): ScreenRect {
        if (screen.width <= 0 || screen.height <= 0) return this
        val newWidth = width.coerceAtLeast(minWidth)
        val newHeight = height.coerceAtLeast(minHeight)
        val centerX = left + width / 2
        val centerY = top + height / 2
        val boundedWidth = newWidth.coerceAtMost(screen.width)
        val boundedHeight = newHeight.coerceAtMost(screen.height)
        val boundedLeft = (centerX - boundedWidth / 2).coerceIn(screen.left, screen.right - boundedWidth)
        val boundedTop = (centerY - boundedHeight / 2).coerceIn(screen.top, screen.bottom - boundedHeight)
        return ScreenRect(
            boundedLeft,
            boundedTop,
            boundedLeft + boundedWidth,
            boundedTop + boundedHeight,
        )
    }
}

data class AccessibilityNodeSnapshot(
    val text: String? = null,
    val contentDescription: String? = null,
    val viewId: String? = null,
    val className: String? = null,
    val bounds: ScreenRect = ScreenRect(0, 0, 0, 0),
    val visibleToUser: Boolean = true,
    val children: List<AccessibilityNodeSnapshot> = emptyList(),
    val hintText: String? = null,
) {
    fun flattened(): List<AccessibilityNodeSnapshot> = buildList {
        val root = this@AccessibilityNodeSnapshot
        fun visit(node: AccessibilityNodeSnapshot) {
            add(node)
            node.children.forEach(::visit)
        }
        visit(root)
    }
}

enum class InstagramScreen {
    FEED,
    REELS,
    DIRECT,
    STORY,
    UNKNOWN,
}

enum class BlockTargetKind {
    FEED_REEL,
    REELS_SURFACE,
    NORMAL_VIDEO,
    SHORTCUT,
    STORY,
}

data class BlockTarget(
    val bounds: ScreenRect,
    val kind: BlockTargetKind,
    val sourceKey: String,
)

data class DetectionPreferences(
    val blockingEnabled: Boolean = true,
    val blockFeedReels: Boolean = true,
    val blockReelsTab: Boolean = true,
    val blockShortcuts: Boolean = true,
    val blockStories: Boolean = false,
    val blockNormalVideos: Boolean = false,
    val diagnosticMode: Boolean = false,
)

data class DetectionResult(
    val screen: InstagramScreen,
    val isPrivateContext: Boolean,
    val targets: List<BlockTarget>,
) {
    val shouldBlock: Boolean get() = targets.isNotEmpty()
    // Instagram changes labels and accessibility IDs while a Reel is playing. The
    // overlay must stay mounted when the blocked region is still the same region.
    val stableKey: String get() = targets.joinToString("|") { target ->
        val bounds = target.bounds
        val geometryKey = listOf(
            bounds.left / 48,
            bounds.top / 48,
            bounds.right / 48,
            bounds.bottom / 48,
        ).joinToString(",")
        "${target.kind}:$geometryKey"
    }
}

/**
 * Conservative, pure detection logic. It never infers a Reel from a generic video alone.
 * A Reel signal must come from an accessible label, resource id, or a visible route label.
 */
object ReelDetectionEngine {
    private val reelTokens = listOf(
        "reel",
        "reels",
        "reel_viewer",
        "reel_item",
        "reel_video",
        "clips",
        "clips_tab",
        "short video",
        "short videos",
        "video corto",
        "videos cortos",
    )
    private val directRouteTokens = setOf("direct", "mensajes", "messages", "message", "inbox", "chats")
    private val sharedMediaTokens = listOf(
        "reel compartido",
        "video compartido",
        "foto compartida",
        "imagen compartida",
        "te ha enviado un reel",
        "te envio un reel",
        "te ha enviado una foto",
        "te ha enviado una imagen",
        "sent you a reel",
        "sent you a photo",
        "sent you an image",
        "shared reel",
        "shared video",
        "shared photo",
        "shared image",
        "compartido por",
        "enviado por",
        "shared by",
        "sent by",
        "reel sent",
        "video sent",
        "photo sent",
        "image sent",
        "reel enviado",
        "video enviado",
        "foto enviada",
        "imagen enviada",
    )
    private val privateConversationTokens = listOf(
        "conversacion con",
        "conversation with",
        "chat con",
        "chat with",
        "escribe un mensaje",
        "escribir un mensaje",
        "write a message",
        "enviar mensaje",
        "send message",
        "te ha enviado",
        "enviado por",
        "sent you",
        "reel compartido",
        "video compartido",
        "shared reel",
        "shared video",
        "compartido por",
        "shared by",
    )
    private val storyTokens = listOf("historia", "historias", "story", "stories", "tu historia", "your story")
    private val reelsRouteTokens = setOf(
        "reels",
        "clips",
        "reels tab",
        "clips tab",
        "reels_tab",
        "clips_tab",
    )
    private val normalVideoTokens = listOf(
        "video",
        "videos",
        "vídeo",
        "vídeos",
        "video en reproducción",
        "playing video",
    )
    private val searchResultTokens = listOf(
        "resultados de busqueda",
        "resultados de búsqueda",
        "search results",
        "personas",
        "people",
        "cuentas",
        "accounts",
        "usuarios",
        "users",
    )
    private val privateContainerIdTokens = listOf(
        "direct",
        "inbox",
        "thread",
        "conversation",
        "chat",
        "shared_media",
        "sharedmedia",
        "media_message",
        "reel_shared",
        "shared_reel",
        "sent_media",
    )

    fun detect(root: AccessibilityNodeSnapshot, preferences: DetectionPreferences): DetectionResult {
        if (!preferences.blockingEnabled) {
            return DetectionResult(InstagramScreen.UNKNOWN, false, emptyList())
        }

        val nodes = root.flattened().filter { it.visibleToUser }
        val allText = nodes.flatMap { nodeSignals(it) }
        val hasSearchInputControl = nodes.any { node ->
            val className = node.className.orEmpty()
            val normalizedId = node.viewId.orEmpty().lowercase()
            className.contains("SearchView", ignoreCase = true) ||
                className.contains("EditText", ignoreCase = true) ||
                normalizedId.contains("search_bar") ||
                normalizedId.contains("search_field") ||
                normalizedId.contains("search_input") ||
                normalizedId.contains("search_edit") ||
                normalizedId.contains("buscar_bar") ||
                normalizedId.contains("buscar_input")
        }
        val hasSearchResultSignal = allText.any { signal -> searchResultTokens.any(signal::contains) }
        val hasSearchIdSignal = nodes.any { node ->
            node.viewId?.let { id ->
                val normalizedId = id.lowercase()
                normalizedId.contains("search") || normalizedId.contains("buscar")
            } == true
        }
        if (hasSearchInputControl && (hasSearchResultSignal || hasSearchIdSignal)) {
            // Search results may contain Reels while the user is looking for a
            // person. Search is not the Reels viewer, so leave these results alone.
            return DetectionResult(InstagramScreen.UNKNOWN, false, emptyList())
        }
        val hasDirectRouteSignal = allText.any(::containsDirectRouteSignal)
        val hasEditableMessageField = nodes.any {
            it.className?.contains("EditText", ignoreCase = true) == true &&
                nodeSignals(it).any(::containsMessageFieldSignal)
        }
        val hasConversationSignal = allText.any { signal ->
            privateConversationTokens.any(signal::contains)
        }
        val hasSharedMediaSignal = allText.any { signal ->
            sharedMediaTokens.any(signal::contains)
        }
        val hasPrivateContainerSignal = nodes.any { node ->
            !isBottomNavigationNode(node, root.bounds) &&
                node.viewId.orEmpty().lowercase(Locale.ROOT).let { id ->
                    privateContainerIdTokens.any(id::contains)
                }
        }
        // Instagram often exposes "Direct" or "Mensajes" in the global navigation.
        // Those labels alone must never make the whole feed look like a private chat.
        val detectedPrivateContext = hasSharedMediaSignal ||
            hasConversationSignal ||
            hasPrivateContainerSignal && (hasDirectRouteSignal || hasReelLikeContent(allText)) ||
            hasEditableMessageField && hasDirectRouteSignal
        // Direct and media shared by another person are always exempt. This is
        // intentionally not configurable: a partial accessibility hierarchy
        // must never turn a private message into a Reel block.
        val isPrivateContext = detectedPrivateContext

        val hasStorySignal = allText.any { signal -> storyTokens.any(signal::contains) }
        if (hasStorySignal && !preferences.blockStories) {
            return DetectionResult(InstagramScreen.STORY, isPrivateContext, emptyList())
        }

        if (hasStorySignal && preferences.blockStories && !isPrivateContext) {
            val storyNode = nodes.firstOrNull { node ->
                nodeSignals(node).any { signal -> storyTokens.any(signal::contains) }
            }
            if (storyNode != null) {
                return DetectionResult(
                    screen = InstagramScreen.STORY,
                    isPrivateContext = false,
                    targets = listOf(
                        BlockTarget(
                            bounds = storyNode.bounds.expandTo(180, 120, root.bounds),
                            kind = BlockTargetKind.STORY,
                            sourceKey = sourceKey(storyNode),
                        ),
                    ),
                )
            }
        }

        val routeSignalPresent = allText.any(::isReelsRouteSignal)
        val videoSurfaceNodes = nodes.filter(::isVideoSurface)
        val reelNodes = nodes
            .filter(::containsReelSignal)
            .filterNot(::isNavigationOnlyReelNode)
            .ifEmpty {
                // A loading Reels viewer may expose only a large video surface and
                // the route label. The route label alone is navigation, not content.
                if (routeSignalPresent && preferences.blockReelsTab && root.bounds.area > 0) {
                    videoSurfaceNodes.filter { it.bounds.area * 100 >= root.bounds.area * 35 }
                } else {
                    emptyList()
                }
            }
        val normalVideoNodes = if (preferences.blockNormalVideos) {
            nodes.filter(::containsNormalVideoSignal).filterNot(::containsReelSignal)
        } else {
            emptyList()
        }
        val contentNodes = (reelNodes + normalVideoNodes).distinct()
        if (contentNodes.isEmpty() || isPrivateContext) {
            return DetectionResult(
                screen = if (isPrivateContext) InstagramScreen.DIRECT else InstagramScreen.UNKNOWN,
                isPrivateContext = isPrivateContext,
                targets = emptyList(),
            )
        }

        val screen = classifyScreen(root, contentNodes)
        if (screen == InstagramScreen.REELS && !preferences.blockReelsTab) {
            return DetectionResult(screen, isPrivateContext, emptyList())
        }
        if (screen == InstagramScreen.FEED && reelNodes.isNotEmpty() && !preferences.blockFeedReels) {
            return DetectionResult(screen, isPrivateContext, emptyList())
        }
        val screenBounds = root.bounds
        val fullSurface = contentNodes.firstOrNull { node ->
            screenBounds.area > 0 && node.bounds.area >= (screenBounds.area * 0.35)
        }

        if (fullSurface != null && screenBounds.area > 0) {
            return DetectionResult(
                screen = InstagramScreen.REELS,
                isPrivateContext = false,
                targets = listOf(
                    BlockTarget(screenBounds, BlockTargetKind.REELS_SURFACE, sourceKey(fullSurface)),
                ),
            )
        }

        // Once a real Reel is identified in the feed, cover the complete Instagram
        // surface. This prevents the user from seeing or interacting with the Reel
        // around a small label/card overlay.
        if (screen == InstagramScreen.FEED && screenBounds.area > 0) {
            val targetKind = if (reelNodes.isNotEmpty()) BlockTargetKind.FEED_REEL else BlockTargetKind.NORMAL_VIDEO
            return DetectionResult(
                screen = screen,
                isPrivateContext = false,
                targets = listOf(
                    BlockTarget(
                        bounds = screenBounds,
                        kind = targetKind,
                        sourceKey = sourceKey(contentNodes.first()),
                    ),
                ),
            )
        }

        val targets = contentNodes.mapNotNull { node ->
            val isShortcut = isBottomShortcut(node, screenBounds)
            if (isShortcut && !preferences.blockShortcuts) return@mapNotNull null
            val kind = if (isShortcut) BlockTargetKind.SHORTCUT else if (hasStorySignal) BlockTargetKind.STORY else if (containsReelSignal(node)) BlockTargetKind.FEED_REEL else BlockTargetKind.NORMAL_VIDEO
            val coverageNode = if (isShortcut) node else nearestContainingNode(node, nodes, screenBounds) ?: node
            BlockTarget(
                bounds = coverageNode.bounds.expandTo(
                    minWidth = if (isShortcut) 96 else 180,
                    minHeight = if (isShortcut) 72 else 120,
                    screen = screenBounds,
                ),
                kind = kind,
                sourceKey = "${sourceKey(node)}|coverage=${sourceKey(coverageNode)}",
            )
        }
            .distinctBy { "${it.kind}:${it.bounds}" }
            .take(3)

        return DetectionResult(screen, false, targets)
    }

    private fun classifyScreen(root: AccessibilityNodeSnapshot, reelNodes: List<AccessibilityNodeSnapshot>): InstagramScreen {
        val rootSignals = root.flattened().flatMap(::nodeSignals)
        val hasRouteLabel = rootSignals.any { normalize(it) == "reels" || normalize(it) == "clips" }
        val surfaceLike = reelNodes.any(::isVideoSurface)
        return if (hasRouteLabel && surfaceLike) InstagramScreen.REELS else InstagramScreen.FEED
    }

    private fun containsReelSignal(node: AccessibilityNodeSnapshot): Boolean {
        val signals = nodeSignals(node)
        return signals.any { value -> reelTokens.any(value::contains) }
    }

    private fun containsNormalVideoSignal(node: AccessibilityNodeSnapshot): Boolean {
        if (containsReelSignal(node)) return false
        val signals = nodeSignals(node)
        return isVideoSurface(node) || signals.any { signal -> normalVideoTokens.any(signal::contains) }
    }

    private fun isNavigationOnlyReelNode(node: AccessibilityNodeSnapshot): Boolean {
        val signals = nodeSignals(node)
        return signals.isNotEmpty() && signals.all(::isReelsRouteSignal) && !isVideoSurface(node)
    }

    private fun isReelsRouteSignal(signal: String): Boolean = normalize(signal) in reelsRouteTokens

    private fun isVideoSurface(node: AccessibilityNodeSnapshot): Boolean =
        node.className?.let { name ->
            name.contains("SurfaceView") ||
                name.contains("TextureView") ||
                name.contains("VideoView")
        } == true

    private fun isBottomShortcut(node: AccessibilityNodeSnapshot, screen: ScreenRect): Boolean {
        if (screen.height <= 0) return false
        val normalizedSignals = nodeSignals(node).map(::normalize)
        val routeLabel = normalizedSignals.any { it == "reels" || it == "clips" }
        return routeLabel && node.bounds.top >= screen.top + (screen.height * 0.65).toInt() && node.bounds.height <= 220
    }

    private fun isBottomNavigationNode(node: AccessibilityNodeSnapshot, screen: ScreenRect): Boolean {
        if (screen.height <= 0) return false
        val isSmall = node.bounds.height <= 220 && node.bounds.width <= (screen.width * 0.45f).toInt()
        return isSmall && node.bounds.top >= screen.top + (screen.height * 0.72f).toInt()
    }

    private fun hasReelLikeContent(signals: List<String>): Boolean = signals.any { signal ->
        reelTokens.any(signal::contains) || normalVideoTokens.any(signal::contains)
    }

    private fun nearestContainingNode(
        node: AccessibilityNodeSnapshot,
        nodes: List<AccessibilityNodeSnapshot>,
        screen: ScreenRect,
    ): AccessibilityNodeSnapshot? {
        val nodeArea = node.bounds.area.coerceAtLeast(1L)
        val maximumArea = if (screen.area > 0) (screen.area * 0.95).toLong() else Long.MAX_VALUE
        return nodes.asSequence()
            .filter { candidate ->
                candidate !== node &&
                    candidate.bounds.contains(node.bounds) &&
                    candidate.bounds.area >= nodeArea * 3 &&
                    candidate.bounds.area <= maximumArea
            }
            .minByOrNull { it.bounds.area }
    }

    private fun nodeSignals(node: AccessibilityNodeSnapshot): List<String> = listOfNotNull(
        node.text,
        node.contentDescription,
        node.viewId,
        node.hintText,
    ).map(::normalize).filter(String::isNotBlank)

    private fun containsDirectRouteSignal(signal: String): Boolean {
        val normalized = normalize(signal)
        return directRouteTokens.any { normalized == it || normalized.contains(it) }
    }

    private fun containsMessageFieldSignal(signal: String): Boolean {
        val normalized = normalize(signal)
        return listOf("escribe", "mensaje", "message", "write", "send", "enviar")
            .any(normalized::contains)
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace("ü", "u")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun sourceKey(node: AccessibilityNodeSnapshot): String = listOfNotNull(
        node.text,
        node.contentDescription,
        node.viewId,
        quantizedBounds(node.bounds),
    ).joinToString("/")

    private fun quantizedBounds(bounds: ScreenRect): String = listOf(
        bounds.left / 32,
        bounds.top / 32,
        bounds.right / 32,
        bounds.bottom / 32,
    ).joinToString(",")
}
