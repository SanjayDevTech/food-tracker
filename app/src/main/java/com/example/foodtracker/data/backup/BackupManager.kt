package com.example.foodtracker.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.foodtracker.BuildConfig
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reads / writes [BackupPayload]s in two flavours — both produce a plain
 * JSON file in a location any other app on the phone can see:
 *
 *  - **Export / Import** — user-picked [Uri] via SAF
 *    (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`). Useful for handing
 *    a snapshot to a charting app, a cloud-sync drop folder, etc.
 *
 *  - **In-app backup** — automatic write to `Downloads/FoodTracker/`
 *    via MediaStore. Discoverable by every file manager, survives uninstall
 *    (Downloads is user storage), restorable from the in-app list.
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val profileStore: ProfileStore,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val backupCollection: Uri
        get() = MediaStore.Downloads.EXTERNAL_CONTENT_URI

    /** `Downloads/FoodTracker/` — where local backups land. */
    private val relativeDir = "${Environment.DIRECTORY_DOWNLOADS}/FoodTracker/"

    // ─── Snapshot construction ────────────────────────────────────────────

    suspend fun buildSnapshot(): BackupPayload = withContext(Dispatchers.IO) {
        val profile = profileStore.profile.first()
        val haptics = profileStore.hapticsEnabled.first()
        val theme = profileStore.themeMode.first()
        BackupPayload(
            createdAtMillis = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            profile = ProfileSnapshot.from(profile, haptics, theme),
            foods = database.foodDao().getAll().map(FoodSnapshot::from),
            foodLog = database.foodLogDao().getAll().map(FoodLogSnapshot::from),
            waterLog = database.waterLogDao().getAll().map(WaterLogSnapshot::from),
            activityLog = database.activityLogDao().getAll().map(ActivityLogSnapshot::from),
            weightLog = database.weightLogDao().getAll().map(WeightLogSnapshot::from),
        )
    }

    // ─── External export / import (Storage Access Framework) ──────────────

    /**
     * Serialise the current DB+profile to the user-picked [target] URI.
     * Used by the "Export data" button — the resulting file is a plain
     * JSON file the user can keep, share, or feed into another app.
     */
    suspend fun exportTo(target: Uri): Result<Unit> = runCatching {
        val payload = buildSnapshot()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(target, "w").use { stream ->
                requireNotNull(stream) { "Cannot open output stream for $target" }
                writePayload(stream, payload)
            }
        }
    }

    /**
     * Read a previously-exported JSON file from [source] and replace all
     * local data with it. Returns the restored payload's `createdAtMillis`
     * on success so the caller can confirm what just landed.
     */
    suspend fun importFrom(source: Uri): Result<Long> = runCatching {
        val payload = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(source).use { stream ->
                requireNotNull(stream) { "Cannot open input stream for $source" }
                readPayload(stream)
            }
        }
        restoreFromPayload(payload)
        payload.createdAtMillis
    }

    // ─── In-app local backup slots ────────────────────────────────────────
    //
    // Destination = the user's custom SAF tree URI when one is set,
    // otherwise `Downloads/FoodTracker/` via MediaStore. Both paths produce
    // a JSON file that any other app on the device can read.

    /**
     * Snapshot the database+profile and write a timestamped JSON file to
     * either the user-picked folder or `Downloads/FoodTracker/`. Returns
     * a [LocalBackup] describing what was saved so the caller can surface
     * the path in the UI.
     */
    suspend fun saveLocalBackup(): Result<LocalBackup> = runCatching {
        val payload = buildSnapshot()
        val customUri = profileStore.backupFolderUri.first()?.let(Uri::parse)
        withContext(Dispatchers.IO) {
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val name = "foodtracker-backup-$timestamp.json"
            if (customUri != null) writeToCustomFolder(customUri, name, payload)
            else writeToDownloads(name, payload)
        }
    }

    private fun writeToCustomFolder(treeUri: Uri, name: String, payload: BackupPayload): LocalBackup {
        val dir = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("Picked folder is no longer accessible. Pick again from Profile → Data.")
        val file = dir.createFile("application/json", name)
            ?: error("Couldn't create $name in the picked folder")
        try {
            context.contentResolver.openOutputStream(file.uri, "w").use { stream ->
                requireNotNull(stream) { "Cannot open output stream for ${file.uri}" }
                writePayload(stream, payload)
            }
        } catch (e: Exception) {
            runCatching { file.delete() }
            throw e
        }
        return LocalBackup(
            uri = file.uri,
            displayName = file.name ?: name,
            createdAtMillis = System.currentTimeMillis(),
            sizeBytes = file.length(),
            origin = LocalBackup.Origin.CustomFolder,
        )
    }

    private fun writeToDownloads(name: String, payload: BackupPayload): LocalBackup {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(backupCollection, values)
            ?: error("MediaStore refused to create backup entry")
        try {
            context.contentResolver.openOutputStream(uri, "w").use { stream ->
                requireNotNull(stream) { "Cannot open output stream for $uri" }
                writePayload(stream, payload)
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            throw e
        }
        return LocalBackup(
            uri = uri,
            displayName = name,
            createdAtMillis = System.currentTimeMillis(),
            sizeBytes = sizeOf(uri),
            origin = LocalBackup.Origin.Downloads,
        )
    }

    /**
     * List every JSON backup the app can find. If the user has picked a
     * custom folder we enumerate that; otherwise we fall back to
     * `Downloads/FoodTracker/` via MediaStore. Newest first.
     */
    suspend fun listBackups(): List<LocalBackup> = withContext(Dispatchers.IO) {
        val customUri = profileStore.backupFolderUri.first()?.let(Uri::parse)
        if (customUri != null) listFromCustomFolder(customUri) else listFromDownloads()
    }

    private fun listFromCustomFolder(treeUri: Uri): List<LocalBackup> {
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles()
            .asSequence()
            .filter { it.isFile && it.name?.endsWith(".json") == true }
            .map {
                LocalBackup(
                    uri = it.uri,
                    displayName = it.name ?: "backup.json",
                    createdAtMillis = it.lastModified(),
                    sizeBytes = it.length(),
                    origin = LocalBackup.Origin.CustomFolder,
                )
            }
            .sortedByDescending { it.createdAtMillis }
            .toList()
    }

    private fun listFromDownloads(): List<LocalBackup> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
            "${MediaStore.MediaColumns.MIME_TYPE} = ?"
        val args = arrayOf(relativeDir, "application/json")
        val sort = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val results = mutableListOf<LocalBackup>()
        context.contentResolver.query(backupCollection, projection, selection, args, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                results += LocalBackup(
                    uri = android.content.ContentUris.withAppendedId(backupCollection, id),
                    displayName = cursor.getString(nameCol),
                    // MediaStore stores DATE_MODIFIED in seconds since epoch.
                    createdAtMillis = cursor.getLong(dateCol) * 1000L,
                    sizeBytes = cursor.getLong(sizeCol),
                    origin = LocalBackup.Origin.Downloads,
                )
            }
        }
        return results
    }

    /** Restore the database+profile from a previously-saved local backup. */
    suspend fun restoreFrom(backup: LocalBackup): Result<Unit> = runCatching {
        val payload = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(backup.uri).use { stream ->
                requireNotNull(stream) { "Cannot read backup ${backup.displayName}" }
                readPayload(stream)
            }
        }
        restoreFromPayload(payload)
    }

    /** Delete a single local backup file. Works for both MediaStore and SAF entries. */
    suspend fun deleteLocalBackup(backup: LocalBackup): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val ok = when (backup.origin) {
                LocalBackup.Origin.Downloads ->
                    context.contentResolver.delete(backup.uri, null, null) > 0
                LocalBackup.Origin.CustomFolder ->
                    DocumentFile.fromSingleUri(context, backup.uri)?.delete() == true
            }
            if (!ok) error("Could not delete ${backup.displayName}")
        }
    }

    // ─── Custom backup folder management (SAF tree URI) ───────────────────

    /**
     * Persist the picked tree URI and take long-term read/write permission
     * so we can read and write inside it across app restarts.
     */
    suspend fun setCustomFolder(treeUri: Uri) {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(treeUri, flags) }
        profileStore.setBackupFolderUri(treeUri.toString())
    }

    /** Forget the custom folder — future backups go to Downloads/FoodTracker again. */
    suspend fun clearCustomFolder() {
        profileStore.backupFolderUri.first()?.let { uriString ->
            runCatching {
                val uri = Uri.parse(uriString)
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(uri, flags)
            }
        }
        profileStore.setBackupFolderUri(null)
    }

    /**
     * Human-friendly display path for the configured folder (Settings UI).
     * Returns null when no custom folder is set.
     */
    suspend fun currentFolderLabel(): String? {
        val uriString = profileStore.backupFolderUri.first() ?: return null
        val treeUri = Uri.parse(uriString)
        return treeUri.lastPathSegment?.substringAfter(":")?.takeIf { it.isNotBlank() }
            ?: DocumentsContract.getTreeDocumentId(treeUri)
    }

    // ─── Internal plumbing ────────────────────────────────────────────────

    private fun sizeOf(uri: Uri): Long {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { return it.statSize }
        return 0L
    }

    private fun writePayload(out: OutputStream, payload: BackupPayload) {
        out.writer(Charsets.UTF_8).use { writer ->
            writer.write(json.encodeToString(BackupPayload.serializer(), payload))
            writer.flush()
        }
    }

    private fun readPayload(input: InputStream): BackupPayload {
        val text = input.reader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString(BackupPayload.serializer(), text)
    }

    /**
     * Nuke-and-replace restore. We delete in FK-safe order (foods last, so
     * the CASCADE doesn't trip), then re-insert in dependency order so
     * food_log's foreign keys resolve.
     */
    private suspend fun restoreFromPayload(payload: BackupPayload) = withContext(Dispatchers.IO) {
        val foodDao = database.foodDao()
        val foodLogDao = database.foodLogDao()
        val waterDao = database.waterLogDao()
        val activityDao = database.activityLogDao()
        val weightDao = database.weightLogDao()

        // Wipe everything. Deleting foods CASCADEs food_log; we still clear
        // food_log explicitly so the order is obvious from the code.
        foodLogDao.deleteAll()
        waterDao.deleteAll()
        activityDao.deleteAll()
        weightDao.deleteAll()
        foodDao.deleteAll()

        // Re-insert. Foods first so food_log foreign keys can resolve.
        foodDao.upsertAll(payload.foods.map { it.toEntity() })
        foodLogDao.insertAll(payload.foodLog.map { it.toEntity() })
        waterDao.insertAll(payload.waterLog.map { it.toEntity() })
        activityDao.insertAll(payload.activityLog.map { it.toEntity() })
        weightDao.upsertAll(payload.weightLog.map { it.toEntity() })

        // Restore profile + settings. We mark onboarded so the user lands
        // on Home directly after a restore.
        profileStore.save(payload.profile.toUserProfile(), markOnboarded = true)
        profileStore.setHapticsEnabled(payload.profile.hapticsEnabled)
        profileStore.setThemeMode(
            com.example.foodtracker.ui.theme.ThemeMode.fromName(payload.profile.themeMode)
        )
    }
}

/**
 * Lightweight view of a backup file the app can read/delete. `uri` works
 * for both MediaStore-backed (`Downloads/FoodTracker/`) and SAF-backed
 * (user-picked folder) entries; `origin` tells us which delete API to call.
 */
data class LocalBackup(
    val uri: Uri,
    val displayName: String,
    val createdAtMillis: Long,
    val sizeBytes: Long,
    val origin: Origin = Origin.Downloads,
) {
    enum class Origin { Downloads, CustomFolder }
}
