package com.miappmusica.player.data.repository

import com.miappmusica.player.data.local.dao.ModeDao
import com.miappmusica.player.data.local.entity.ModeEntity
import com.miappmusica.player.data.prefs.UserPreferences
import com.miappmusica.player.domain.model.AppMode
import com.miappmusica.player.domain.repository.ModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModeRepositoryImpl @Inject constructor(
    private val dao: ModeDao,
    private val prefs: UserPreferences
) : ModeRepository {

    /** Seeds the default modes the first time the app runs. Idempotent. */
    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.upsertAll(
                AppMode.defaults().mapIndexed { index, mode -> ModeEntity.from(mode, index) }
            )
        }
    }

    override fun observeModes(): Flow<List<AppMode>> =
        dao.observeModes().map { list -> list.map { it.toDomain() } }

    override fun observeActiveMode(): Flow<AppMode> =
        combine(observeModes(), prefs.activeModeId) { modes, activeId ->
            modes.firstOrNull { it.id == activeId } ?: AppMode.NORMAL
        }

    override suspend fun activate(modeId: String) = prefs.setActiveModeId(modeId)

    override suspend fun upsert(mode: AppMode) {
        // Preserve the existing sortOrder when updating so editing a mode (e.g. auto-assigning
        // its private playlist) does NOT reorder the list. New modes go to the end.
        val existing = dao.getById(mode.id)
        val sortOrder = existing?.sortOrder ?: dao.nextSortOrder()
        dao.upsert(ModeEntity.from(mode, sortOrder))
    }

    override suspend fun delete(modeId: String) {
        if (modeId == AppMode.NORMAL_ID) return // Normal is the mandatory escape mode.
        dao.delete(modeId)
    }
}
