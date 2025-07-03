package com.mars.ultimatecleaner.domain.usecase.file

import com.mars.ultimatecleaner.domain.model.OrganizationPlan
import com.mars.ultimatecleaner.domain.model.OrganizationProgress
import com.mars.ultimatecleaner.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OrganizeFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {

    suspend operator fun invoke(organizationPlan: OrganizationPlan): Flow<OrganizationProgress> {
        return fileRepository.organizeFiles(organizationPlan)
    }
}