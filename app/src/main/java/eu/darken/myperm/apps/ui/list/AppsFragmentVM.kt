package eu.darken.myperm.apps.ui.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.list.apps.NormalAppVH
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.livedata.SingleLiveEvent
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.main.ui.main.MainFragmentDirections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class AppsFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    packageRepo: AppRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val searchTerm = MutableStateFlow<String?>(null)
    private val filterOptions = MutableStateFlow(FilterOptions())
    private val sortOptions = MutableStateFlow(SortOptions())

    val events = SingleLiveEvent<AppsEvents>()

    val listData: LiveData<List<AppsAdapter.Item>> = combine(
        packageRepo.apps,
        searchTerm,
        filterOptions,
        sortOptions
    ) { apps, searchTerm, filterOptions, sortOptions ->
        val filtered = apps
            .filter { app -> filterOptions.keys.all { it.matches(app) } }
            .filter {
                if (searchTerm == null) return@filter true
                if (it.id.toString().contains(searchTerm)) return@filter true
                if (it is NormalApp && it.label?.contains(searchTerm) == true) return@filter true

                return@filter false
            }
            .sortedWith(sortOptions.mainSort.comparator)

        filtered.map { app ->
            when (app) {
                is NormalApp -> NormalAppVH.Item(
                    app = app,
                    onClickAction = {
                        log(TAG) { "Navigating to $app" }
                        MainFragmentDirections.actionMainFragmentToAppDetailsFragment(appId = app.id).navigate()
                    },
                    onShowPermission = { events.postValue(AppsEvents.ShowPermissionSnackbar(it)) }
                )
                else -> throw IllegalArgumentException()
            }
        }
    }
        .asLiveData2()

    fun onSearchInputChanged(term: String?) {
        log { "onSearchInputChanged(term=$term)" }
        searchTerm.value = term
    }

    fun updateFilterOptions(action: (FilterOptions) -> FilterOptions) {
        val old = filterOptions.value
        val new = action(old)
        log { "updateFilterOptions($old) -> $new" }
        filterOptions.value = new
    }

    fun updateSortOptions(action: (SortOptions) -> SortOptions) {
        val old = sortOptions.value
        val new = action(old)
        log { "updateFilterOptions($old) -> $new" }
        sortOptions.value = new
    }

    fun showFilterDialog() {
        log { "showFilterDialog" }
        events.postValue(AppsEvents.ShowFilterDialog(filterOptions.value))
    }

    fun showSortDialog() {
        log { "showSortDialog" }
        events.postValue(AppsEvents.ShowSortDialog(sortOptions.value))
    }
}