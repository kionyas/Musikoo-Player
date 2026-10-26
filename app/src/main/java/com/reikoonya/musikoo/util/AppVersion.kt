package com.reikoonya.musikoo.util

/**
 * Части версии: "v1.10.0-beta" -> [1, 10, 0].
 * Всё, что идёт после цифр и точек, отбрасывается.
 */
internal fun versionParts(version: String): List<Int> =
    version.trim().removePrefix("v").takeWhile { it.isDigit() || it == '.' }
        .split('.').mapNotNull { it.toIntOrNull() }

/** 1.10.0 > 1.9.2, missing parts count as 0 (1.4 == 1.4.0). */
fun isNewerVersion(remote: String, local: String): Boolean {
    val r = versionParts(remote)
    val l = versionParts(local)
    for (i in 0 until maxOf(r.size, l.size)) {
        val a = r.getOrElse(i) { 0 }
        val b = l.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}
