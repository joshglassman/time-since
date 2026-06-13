package com.scribbles.timesince.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scribbles.timesince.data.sync.SyncCoordinator
import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.usecase.CreateCategoryUseCase
import com.scribbles.timesince.domain.usecase.DeleteCategoryUseCase
import com.scribbles.timesince.domain.usecase.GetCategoriesUseCase
import com.scribbles.timesince.domain.usecase.UpdateCategoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    getCategories: GetCategoriesUseCase,
    private val createCategory: CreateCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
    private val syncCoordinator: SyncCoordinator? = null,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onCreate(name: String, colorHex: String, icon: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            createCategory(name, colorHex, icon)
            syncCoordinator?.requestSync()
        }
    }

    fun onUpdate(id: String, name: String, colorHex: String, icon: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            updateCategory(id, name, colorHex, icon)
            syncCoordinator?.requestSync()
        }
    }

    fun onDelete(id: String) {
        viewModelScope.launch {
            deleteCategory(id)
            syncCoordinator?.requestSync()
        }
    }
}
