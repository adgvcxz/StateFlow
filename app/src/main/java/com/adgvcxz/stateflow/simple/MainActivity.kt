package com.adgvcxz.stateflow.simple

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.adgvcxz.stateflow.*
import com.adgvcxz.stateflow.simple.databinding.ActivityMainBinding
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import reactivecircus.flowbinding.android.view.clicks

enum class MainStatus {
    Normal, Timing, Pause
}

data class MainModel(
    var status: MainStatus = MainStatus.Normal,
    var seconds: Int = 0
)

object Start : IEvent
object Pause : IEvent
object Stop : IEvent

data class SetSeconds(val seconds: Int) : IMutation
data class SetStatus(val status: MainStatus) : IMutation

class MainViewModel(seconds: Int) : AFViewModel<MainModel>() {
    override val initState: MainModel = MainModel(seconds = seconds)


    override suspend fun mutate(event: IEvent): Flow<IMutation> {
        when (event) {
            Start -> if (currentState.status != MainStatus.Timing) {
                return flow<IMutation> {
                    while (true) {
                        emit(SetSeconds(currentState.seconds + 1))
                        delay(1000)
                    }
                }.withIndex().takeWhile {
                    it.index == 0 || currentState.status == MainStatus.Timing
                }.map { it.value }.onStart { emit(SetStatus(MainStatus.Timing)) }
            }
            Pause -> return flowOf(SetStatus(MainStatus.Pause))
            Stop -> return flowOf(SetStatus(MainStatus.Normal), SetSeconds(0))
        }
        return super.mutate(event)
    }

    override fun reduce(state: MainModel, mutation: IMutation): MainModel {
        when (mutation) {
            is SetSeconds -> return state.copy(seconds = mutation.seconds)
            is SetStatus -> return state.copy(status = mutation.status)
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
        viewModel = ViewModelProvider(this, MainFactory(50))[MainViewModel::class.java]
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.bindModelWhenCreated(this) {
            add({ seconds }, { binding.textView.text = "$this" })
        }

        viewModel.bindEvent(this) {
            add(binding.start.clicks(), Start)
            add(binding.pause.clicks(), { Pause })
            add(binding.stop.clicks(), { Stop })
        }
    }
}

class MainFactory(private val index: Int) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == MainViewModel::class.java) {
            return MainViewModel(index) as T
        }
        return super.create(modelClass)
    }
}