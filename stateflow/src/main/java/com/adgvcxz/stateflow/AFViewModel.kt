package com.adgvcxz.stateflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by zhaowei on 2021/10/28.
 */
abstract class AFViewModel<M> : ViewModel() {

    val event: Channel<IEvent> = Channel()

    abstract val initState: M

    val state: SharedFlow<M> by lazy {
        _uiState = MutableStateFlow(initState)
        _currentState = initState
        viewModelScope.launch {
            for (item in event) {
                val event = transformEvent(item)
                val mutation = mutate(event)?.let { transformMutation(it) }
                if (mutation != null) {
                    _currentState = scan(currentState, mutation)
                    _uiState.value = currentState
                }
            }
        }
        _uiState.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    }


    private lateinit var _uiState: MutableStateFlow<M>

    private var _currentState: M? = null

    val currentState: M get() = _currentState ?: initState


    open suspend fun transformEvent(event: IEvent): IEvent = event
    open suspend fun transformMutation(mutation: IMutation): IMutation = mutation

    open suspend fun mutate(event: IEvent): IMutation? {
        return event as? IMutation
    }

    open fun scan(state: M, mutation: IMutation): M = state

    override fun onCleared() {
        super.onCleared()
        event.close()
    }


}