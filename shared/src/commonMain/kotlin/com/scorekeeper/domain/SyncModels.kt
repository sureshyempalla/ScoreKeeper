package com.scorekeeper.domain

import kotlinx.serialization.Serializable

/**
 * Everything about one Community Event, bundled for sync: the event itself
 * plus every game within it (each already carrying its own entrants/matches/
 * sets -- see [EventGame]). This is the unit [com.scorekeeper.data.EventSyncRepository]
 * pushes to and pulls from Firestore, one document per bundle.
 */
@Serializable
data class EventBundle(
    val event: CommunityEvent,
    val games: List<EventGame>
)
