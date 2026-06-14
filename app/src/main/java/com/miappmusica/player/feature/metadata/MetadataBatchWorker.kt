package com.miappmusica.player.feature.metadata

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.domain.repository.MetadataRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Background batch metadata cleaner. Used for large jobs (e.g. "auto-clean all Downloads")
 * so the work survives navigation and process death. Dependencies are obtained via a Hilt
 * [EntryPoint] because WorkManager constructs workers itself.
 */
class MetadataBatchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun metadataRepository(): MetadataRepository
        fun libraryRepository(): LibraryRepository
    }

    override suspend fun doWork(): Result {
        val ids = inputData.getLongArray(KEY_TRACK_IDS)?.toList().orEmpty()
        if (ids.isEmpty()) return Result.success()

        val deps = EntryPointAccessors.fromApplication(applicationContext, WorkerDeps::class.java)
        val tracks = deps.libraryRepository().getTracks(ids)

        var processed = 0
        tracks.forEach { track ->
            runCatching {
                val diff = deps.metadataRepository().buildAutoProposal(track)
                deps.metadataRepository().apply(diff)
            }.onSuccess { processed++ }
        }
        deps.libraryRepository().refresh()

        return Result.success(Data.Builder().putInt(KEY_PROCESSED, processed).build())
    }

    companion object {
        const val KEY_TRACK_IDS = "track_ids"
        const val KEY_PROCESSED = "processed"

        fun request(trackIds: List<Long>): WorkRequest =
            OneTimeWorkRequestBuilder<MetadataBatchWorker>()
                .setInputData(
                    Data.Builder()
                        .putLongArray(KEY_TRACK_IDS, trackIds.toLongArray())
                        .build()
                )
                .build()
    }
}
