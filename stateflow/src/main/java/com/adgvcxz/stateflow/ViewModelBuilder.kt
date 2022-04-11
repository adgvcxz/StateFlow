package com.adgvcxz.stateflow

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by zhaowei on 2021/10/29.
 */
class ViewModelBuilder<M> {

    private val items = mutableListOf<ViewModelItem<M, Any, Any>>()

    @FlowPreview
    fun build(scope: CoroutineScope, viewModel: AFViewModel<M>) {
        items.forEach { item ->
            scope.launch {
                viewModel.state.map { item.value.invoke(it) }
                    .flatMapMerge { value ->
                        val result = flow { emit(value) }
                        item.filter?.invoke(result) ?: result
                    }.map {
                        item.map?.invoke(it) ?: it
                    }.distinctUntilChanged()
                    .collect { item.behavior.invoke(it) }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <S> addItem(init: ViewModelItem<M, S, S>.() -> Unit) {
        val item = ViewModelItem<M, S, S>()
        item.init()
        items.add(item as ViewModelItem<M, Any, Any>)
    }

}

class ViewModelItem<M, S, R> {

    lateinit var value: (M.() -> S)
    lateinit var behavior: R.() -> Unit
    var filter: (Flow<S>.() -> Flow<S>)? = null
    var map: (S.() -> R)? = null

    fun behavior(init: R.() -> Unit) {
        behavior = init
    }

    fun value(init: M.() -> S) {
        value = init
    }

    fun map(init: S.() -> R) {
        map = init
    }

    fun filter(init: Flow<S>.() -> Flow<S>) {
        filter = init
    }
}

@FlowPreview
fun <M> AFViewModel<M>.bindModelWhenCreated(
    owner: LifecycleOwner,
    init: ViewModelBuilder<M>.() -> Unit
) {
    val builder = ViewModelBuilder<M>()
    builder.init()
    owner.lifecycleScope.launchWhenCreated {
        builder.build(this, this@bindModelWhenCreated)
    }
}

@FlowPreview
fun <M> AFViewModel<M>.bind(
    owner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    init: ViewModelBuilder<M>.() -> Unit
) {
    val builder = ViewModelBuilder<M>()
    builder.init()
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(state) {
            builder.build(this, this@bind)
        }
    }
}

fun <M, T> ViewModelBuilder<M>.add(
    data: M.() -> T,
    action: T.() -> Unit,
    filter: (Flow<T>.() -> Flow<T>)? = null
) {
    addItem<T> {
        value { data(this) }
        filter { filter?.invoke(this) ?: this }
        behavior { action(this) }
    }
}