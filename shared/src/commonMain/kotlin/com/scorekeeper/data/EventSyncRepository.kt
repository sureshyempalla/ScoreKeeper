package com.scorekeeper.data

import com.scorekeeper.domain.EventBundle
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * Phase 1 sync: on-demand push/pull of Community Events to Firestore, one
 * document per event under `users/{uid}/events/{eventId}`. Deliberately not
 * realtime (no `.snapshots` listener) -- see the note on [GameRepository
 * .importEventBundle] for the merge rule: a pull only ever imports an event
 * this device doesn't already have; it never overwrites a local event with
 * server data, so there's no risk of a stale pull clobbering an in-progress
 * edit. Pushing, on the other hand, always overwrites the server copy with
 * this device's local copy -- this device is always the source of truth for
 * events it already knows about.
 */
class EventSyncRepository {
    private fun eventsCollection(uid: String) =
        Firebase.firestore.collection("users").document(uid).collection("events")

    suspend fun push(uid: String, bundle: EventBundle) {
        eventsCollection(uid).document(bundle.event.id).set(bundle)
    }

    /** Ids of every event this account has stored on the server (across all of its devices). */
    suspend fun remoteEventIds(uid: String): List<String> =
        eventsCollection(uid).get().documents.map { it.id }

    suspend fun pull(uid: String, eventId: String): EventBundle? =
        eventsCollection(uid).document(eventId).get().takeIf { it.exists }?.data<EventBundle>()
}
