package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.QuickAction
import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetQuickActionsUseCase @Inject constructor(
    private val cleaningRepository: CleaningRepository,
    private val storageRepository: StorageRepository
) {
    operator fun invoke(): Flow<List<QuickAction>> = flow {
        val actions = mutableListOf<QuickAction>()

        // Get storage info to determine quick actions
        storageRepository.getStorageInfo().collect { storageInfo ->
            actions.clear()

            // Add quick clean action if storage is low
            if (storageInfo.availableSpace < storageInfo.totalSpace * 0.2) {
                actions.add(
                    QuickAction.QuickClean(
                        title = "Quick Clean",
                        description = "Free up space instantly",
                        estimatedSpaceSaved = 500L * 1024 * 1024 // 500MB estimate
                    )
                )
            }

            // Add junk files scan action
            actions.add(
                QuickAction.ScanJunkFiles(
                    title = "Scan Junk Files",
                    description = "Find and remove junk files"
                )
            )

            // Add duplicate files scan action
            actions.add(
                QuickAction.FindDuplicates(
                    title = "Find Duplicates",
                    description = "Locate duplicate files"
                )
            )

            // Add cache clean action
            actions.add(
                QuickAction.ClearCache(
                    title = "Clear Cache",
                    description = "Clear app cache files"
                )
            )

            emit(actions)
        }
    }
}
