package com.fixoom.core.model

object ContactPhone {
    /** Sri Lankan national numbers and international numbers are stored in E.164 form. */
    fun normalize(input: String): String? {
        val compact = input.trim().replace(Regex("[\\s().-]"), "")
        val international =
            when {
                compact.startsWith("00") -> "+" + compact.drop(2)
                compact.matches(Regex("0[1-9][0-9]{8}")) -> "+94" + compact.drop(1)
                compact.matches(Regex("94[1-9][0-9]{8}")) -> "+" + compact
                else -> compact
            }
        return international.takeIf { it.matches(Regex("[+][1-9][0-9]{7,14}")) }
    }
}
