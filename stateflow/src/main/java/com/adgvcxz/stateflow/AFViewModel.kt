package com.adgvcxz.stateflow

import androidx.lifecycle.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by zhaowei on 2021/10/28.
 */
abstract class AFViewModel<M> : ViewModel() {

    val action: MutableSharedFlow<IEvent> = MutableSharedFlow()

    private val mutation: MutableSharedFlow<IMutation> = MutableSharedFlow()

    private lateinit var _uiState: MutableStateFlow<M>

    abstract val initState: M

    @FlowPreview
    @Suppress("DeferredResultUnused")
    val state: SharedFlow<M> by lazy {
        _uiState = MutableStateFlow(initState)
        viewModelScope.launch {
            val event = transformEvent(action)
            val afterAction = event.flatMapMerge { mutate(it) }
            val mutation = transformMutation(merge(afterAction, mutation))
            mutation.collect {
                reduce(_uiState.value, it)?.run { _uiState.value = this }
            }
        }
        _uiState.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    }

    val currentState: M get() = if (::_uiState.isInitialized) _uiState.value else initState


    open suspend fun transformEvent(event: Flow<IEvent>): Flow<IEvent> = event
    open suspend fun transformMutation(mutation: Flow<IMutation>): Flow<IMutation> = mutation

    open suspend fun mutate(event: IEvent): Flow<IMutation> {
        if (event is IMutation) {
            return flow { emit(event) }
        }
        return emptyFlow()
    }

    open fun reduce(state: M, mutation: IMutation): M = state

    suspend fun emit(mutation: IMutation) {
        this.mutation.emit(mutation)
    }

    fun <T> merge(vararg flows: Flow<T>): Flow<T> = flowOf(*flows).flattenMerge()
}