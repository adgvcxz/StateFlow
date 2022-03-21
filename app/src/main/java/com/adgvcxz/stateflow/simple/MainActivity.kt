package com.adgvcxz.stateflow.simple

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.adgvcxz.stateflow.*
import com.adgvcxz.stateflow.simple.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks


data class MainModel(
    var index1: Int,
    var index2: Int,
    var current: Boolean
)

class MainViewModel(index: Int) : AFViewModel<MainModel>() {
    override val initState: MainModel = MainModel(index, index, true)

    override suspend fun mutate(event: IEvent): IMutation? {
        if (event is Add) {
            return withContext(Dispatchers.IO) {
                delay(event.time.toLong())
                SetValue(if (currentState.current) currentState.index1 * 2 else currentState.index2 * 2)
            }
        }
        return super.mutate(event)
    }

    override fun scan(state: MainModel, mutation: IMutation): MainModel {
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
            add({ index2 }, {
                binding.textView1.text = "$this"
            })
        }
        binding.button1.clicks().onEach {
            viewModel.event.emit(Add(100))
        }.launchIn(lifecycleScope)
        binding.button2.clicks().onEach {
            viewModel.event.emit(Add(200))
        }.launchIn(lifecycleScope)
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