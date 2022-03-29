package com.adgvcxz.stateflow

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/**
 * Created by zhaowei on 2022/3/21.
 */
class EventBuilder {

    private val items = mutableListOf<EventItem<Any>>()

    @OptIn(FlowPreview::class)
    fun build(scope: CoroutineScope, viewModel: AFViewModel<*>) {
        items.forEach {
            it.flow().map { event -> it.action(event) }
                .filterIsInstance<IEvent>()
                .flatMapMerge { event ->
                    val result = flow { emit(event) }
                    it.transform?.invoke(result) ?: result
                }.onEach { event -> viewModel.action.emit(event) }
                .launchIn(scope)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> addItem(init: EventItem<T>.() -> Unit) {
        val item = EventItem<T>()
        item.init()
        items.add(item as EventItem<Any>)
    }
}

class EventItem<T> {

    var transform: (Flow<IEvent>.() -> Flow<IEvent>)? = null

    lateinit var action: (T) -> Any
    lateinit var flow: () -> Flow<T>

    fun flow(init: () -> Flow<T>) {
        flow = init
    }

    fun action(init: T.() -> Any) {
        action = init
    }

    fun transform(init: Flow<IEvent>.() -> Flow<IEvent>) {
        transform = init
    }
}


@FlowPreview
fun <M> AFViewModel<M>.bindEvent(
    owner: LifecycleOwner,
    init: EventBuilder.() -> Unit
) {
    val builder = EventBuilder()
    builder.init()
    builder.build(owner.lifecycleScope, this@bindEvent)
}

fun <T> EventBuilder.add(
    flow: () -> Flow<T>,
    action: T.() -> Any,
    transform: (Flow<IEvent>.() -> Flow<IEvent>)? = null
) {
    addItem<T> {
        this.transform = transform
        flow { flow() }
        action { action(this) }
    }
}


fun <T> EventBuilder.add(
    flow: Flow<T>,
    action: T.() -> Any,
    transform: (Flow<IEvent>.() -> Flow<IEvent>)? = null
) {
    addItem<T> {
        this.transform = transform
        flow { flow }
        action { action(this) }
    }
}

fun <T> EventBuilder.add(
    flow: Flow<T>,
    event: Any,
    transform: (Flow<IEvent>.() -> Flow<IEvent>)? = null
) {
    addItem<T> {
        this.transform = transform
        flow { flow }
        action { event }
    }
}