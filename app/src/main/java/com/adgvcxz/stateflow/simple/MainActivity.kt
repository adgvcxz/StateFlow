package com.adgvcxz.stateflow.simple

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.adgvcxz.stateflow.*
import com.adgvcxz.stateflow.simple.databinding.ActivityMainBinding
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import reactivecircus.flowbinding.android.view.clicks


data class MainModel(
    var index1: Int,
    var index2: Int,
    var current: Boolean
)

class MainViewModel(index: Int) : AFViewModel<MainModel>() {
    override val initState: MainModel = MainModel(index, index, true)

    override suspend fun mutate(event: IEvent): Flow<IMutation> {
        if (event is Add) {
            return flow {
                delay(event.time.toLong())
                emit(SetValue(if (currentState.current) currentState.index1 * 2 else currentState.index2 * 2))
                delay(event.time.toLong())
                emit(SetValue(if (currentState.current) currentState.index1 * 2 else currentState.index2 * 2))
            }
        }
        return super.mutate(event)
    }

    override fun reduce(state: MainModel, mutation: IMutation): MainModel {
        if (mutation is SetValue) {
            return if (state.current) {
                state.copy(index1 = mutation.value)
            } else {
                state.copy(index2 = mutation.value)
            }
        }
        return state
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel


    @FlowPreview
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, MainFactory(1))[MainViewModel::class.java]
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        viewModel.bindModelWhenCreated(this) {
            add({ index1 }, { binding.textView.text = "$this" })
            add({ index2 }, { binding.textView1.text = "$this" })
        }

        viewModel.bindEvent(this) {
            add(binding.button1.clicks(), Add(100))
            add(binding.button2.clicks(), { Add(100) })
        }
    }
}

data class Add(val time: Int) : IEvent
data class SetValue(val value: Int) : IMutation


class MainFactory(private val index: Int) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == MainViewModel::class.java) {
            return MainViewModel(index) as T
        }
        return super.create(modelClass)
    }
}