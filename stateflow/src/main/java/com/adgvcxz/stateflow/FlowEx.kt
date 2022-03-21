package com.adgvcxz.stateflow

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect

/**
 * Created by zhaowei on 2021/10/29.
 */
fun <M> AFViewModel<M>.launchWhenCreated(owner: LifecycleOwner, bind: M.() -> Unit) {
    owner.lifecycleScope.launchWhenCreated {
        state.collect { bind(it) }
    }
}