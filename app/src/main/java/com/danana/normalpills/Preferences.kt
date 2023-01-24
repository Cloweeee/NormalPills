package com.danana.normalpills

import com.tencent.mmkv.MMKV

const val CREATURES = "creatures"
const val CONFETTI = "confetti"

private val BooleanPreferenceDefaults =
    mapOf(
        CREATURES to false,
        CONFETTI to true,
    )

object Preferences {
    private val kv = MMKV.defaultMMKV()

    private fun String.getBoolean(default: Boolean = BooleanPreferenceDefaults.getOrElse(this) { false }): Boolean =
        kv.decodeBool(this, default)

    private fun String.updateBoolean(newValue: Boolean) = kv.encode(this, newValue)
    fun updateValue(key: String, b: Boolean) = key.updateBoolean(b)


    fun creaturesEnabled() = CREATURES.getBoolean()
    fun confettiEnabled() = CONFETTI.getBoolean()
}