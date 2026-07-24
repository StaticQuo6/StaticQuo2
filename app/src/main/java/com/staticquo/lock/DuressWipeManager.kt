package com.staticquo.lock

import com.staticquo.data.db.AppLockDao
import com.staticquo.heatmap.HeatmapRepository
import com.staticquo.vault.VaultRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuressWipeManager @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val heatmapRepository: HeatmapRepository,
    private val appLockDao: AppLockDao
) {

    suspend fun performWipe() {
        vaultRepository.wipeAll()
        heatmapRepository.clearAll()
        appLockDao.clearAll()
    }
}
