package guide.core.delivery

import java.io.File

/**
 * On-disk content store with lane-aware budgets — docs/ARCHITECTURE.md §7.
 *
 * Deliberately plain: a directory per lane, a TSV manifest, oldest-first
 * eviction. It is pure `java.io` so it can be tested against a temp directory
 * on the JVM, and small enough that the Android layer only has to supply a
 * root directory and a budget.
 */
class BundleStore(
    val root: File,
    var maxBytes: Long = 500L * 1024 * 1024,
) {

    data class Entry(
        val poiId: String,
        val hash: String,
        val lane: Lane,
        val language: String,
        val relativePath: String,
        val bytes: Long,
        var lastUsedMs: Long,
        val pinned: Boolean = false,
        val voice: String = "",
    ) {
        val key: String get() = "$lane|$language|$voice|$poiId|$hash"
    }

    private val entries = LinkedHashMap<String, Entry>()
    private val manifestFile = File(root, "manifest.tsv")

    init {
        root.mkdirs()
        load()
    }

    @Synchronized
    fun load() {
        entries.clear()
        if (!manifestFile.exists()) return
        manifestFile.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val f = line.split('\t')
            if (f.size < 8) return@forEachLine
            val lane = runCatching { Lane.valueOf(f[2]) }.getOrNull() ?: return@forEachLine
            val entry = Entry(
                poiId = f[0],
                hash = f[1],
                lane = lane,
                language = f[3],
                relativePath = f[4],
                bytes = f[5].toLongOrNull() ?: 0L,
                lastUsedMs = f[6].toLongOrNull() ?: 0L,
                pinned = f[7] == "1",
                voice = f.getOrNull(8) ?: "",
            )
            entries[entry.key] = entry
        }
    }

    @Synchronized
    fun save() {
        root.mkdirs()
        manifestFile.bufferedWriter().use { w ->
            for (e in entries.values) {
                w.write(
                    listOf(
                        e.poiId, e.hash, e.lane.name, e.language, e.relativePath,
                        e.bytes.toString(), e.lastUsedMs.toString(), if (e.pinned) "1" else "0", e.voice,
                    ).joinToString("\t"),
                )
                w.newLine()
            }
        }
    }

    fun has(poiId: String, hash: String, lane: Lane, language: String, voice: String = ""): Boolean =
        entries.containsKey(keyOf(poiId, hash, lane, language, voice))

    fun hasAnywhere(poiId: String, hash: String): Boolean =
        entries.values.any { it.poiId == poiId && it.hash == hash }

    /** `poiId/lane` → hash, for the `have` map in a prefetch request. */
    fun knownHashes(): Map<String, String> =
        entries.values.associate { "${it.poiId}/${it.lane}" to it.hash }

    fun fileFor(poiId: String, hash: String, lane: Lane, language: String, voice: String = ""): File {
        val ext = if (lane == Lane.AUDIO) "opus" else "json"
        val voicePart = if (voice.isBlank()) "" else "-$voice"
        val dir = File(root, "${lane.name.lowercase()}/$language")
        return File(dir, "$poiId-$hash$voicePart.$ext")
    }

    @Synchronized
    fun put(
        poiId: String,
        hash: String,
        lane: Lane,
        language: String,
        bytes: ByteArray,
        nowMs: Long = System.currentTimeMillis(),
        pinned: Boolean = false,
        voice: String = "",
    ): File {
        val file = fileFor(poiId, hash, lane, language, voice)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)

        val relative = file.relativeTo(root).path.replace('\\', '/')
        val entry = Entry(
            poiId = poiId,
            hash = hash,
            lane = lane,
            language = language,
            relativePath = relative,
            bytes = bytes.size.toLong(),
            lastUsedMs = nowMs,
            pinned = pinned,
            voice = voice,
        )
        entries[entry.key] = entry
        save()
        return file
    }

    fun read(poiId: String, hash: String, lane: Lane, language: String, voice: String = ""): ByteArray? {
        val file = fileFor(poiId, hash, lane, language, voice)
        return if (file.exists()) file.readBytes() else null
    }

    @Synchronized
    fun touch(poiId: String, language: String, nowMs: Long = System.currentTimeMillis()) {
        entries.values.filter { it.poiId == poiId && it.language == language }
            .forEach { it.lastUsedMs = nowMs }
        save()
    }

    fun totalBytes(): Long = entries.values.sumOf { it.bytes }

    /** Entries that would be deleted to get back under [maxBytes], oldest first. */
    fun evictionCandidates(): List<Entry> {
        val over = totalBytes() - maxBytes
        if (over <= 0) return emptyList()

        val ordered = entries.values
            .filter { !it.pinned }
            .sortedWith(compareBy<Entry> { it.lane != Lane.AUDIO }.thenBy { it.lastUsedMs })

        val victims = mutableListOf<Entry>()
        var freed = 0L
        for (e in ordered) {
            if (freed >= over) break
            victims += e
            freed += e.bytes
        }
        return victims
    }

    /** Delete until under budget. Text outlives audio by design. */
    @Synchronized
    fun evict(): List<Entry> {
        val victims = evictionCandidates()
        for (v in victims) {
            val file = File(root, v.relativePath)
            runCatching { file.delete() }
            entries.remove(v.key)
        }
        if (victims.isNotEmpty()) save()
        return victims
    }

    // ---- Cell index (lane A), kept out of the LRU lane accounting -------

    private fun cellFile(cell: Cell): File =
        File(root, "cells/${cell.z}/${cell.x}/${cell.y}.json")

    fun putCellIndex(cell: Cell, json: String) {
        val f = cellFile(cell)
        f.parentFile?.mkdirs()
        f.writeText(json)
    }

    fun readCellIndex(cell: Cell): String? {
        val f = cellFile(cell)
        return if (f.exists()) f.readText() else null
    }

    @Synchronized
    fun clear() {
        entries.clear()
        save()
        root.deleteRecursively()
        root.mkdirs()
    }

    private fun keyOf(poiId: String, hash: String, lane: Lane, language: String, voice: String): String =
        "$lane|$language|$voice|$poiId|$hash"
}
