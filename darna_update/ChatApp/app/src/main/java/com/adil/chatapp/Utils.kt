package com.adil.chatapp

/**
 * Builds a deterministic, unique chat room id for two users so that both
 * users always resolve to the same conversation node in the database,
 * regardless of who opens the chat first.
 */
fun buildChatId(uid1: String, uid2: String): String {
    return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
}
