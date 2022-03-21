package com.adgvcxz.stateflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by zhaowei on 2021/10/28.
 */
abstract class AFViewModel<M> : ViewModel() {

    val action: MutableSharedFlow<IEvent> = MutableSharedFlow()

    private lateinit var _uiState: MutableStateFlow<M>

    abstract val initState: M

    @Suppress("DeferredResultUnused")
    val state: SharedFlow<M> by lazy {
        _uiState = MutableStateFlow(initState)
        viewModelScope.launch {
            action.collect {
                val event = transformEvent(it)
                val mutation = mutate(event)?.let { value -> transformMutation(value) }
                mutation?.let { value -> scan(_uiState.value, value) }?.let { value ->
                    _uiState.value = value
                }
            }
        }
        _uiState.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    }

    val currentState: M get() = if (::_uiState.isInitialized) _uiState.value else initState


    open suspend fun transformEvent(event: IEvent): IEvent = event
    open suspend fun transformMutation(mutation: IMutation): IMutation = mutation

    open suspend fun mutate(event: IEvent): IMutation? {
        return event as? IMutation
    }

    open fun scan(state: M, mutation: IMutation): M = state


}