package com.adgvcxz.stateflow.simple

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.adgvcxz.stateflow.*
import com.adgvcxz.stateflow.simple.databinding.FragmentTimerBinding
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import reactivecircus.flowbinding.android.view.clicks

/**
 * Created by zhaowei on 2022/4/11.
 */

enum class TimerStatus {
    Normal, Timing, Pause
}

data class TimerModel(
    var status: TimerStatus = TimerStatus.Normal,
    var seconds: Int = 0
)

object Start : IEvent
object Pause : IEvent
object Stop : IEvent

data class SetSeconds(val seconds: Int) : IMutation
data class SetStatus(val status: TimerStatus) : IMutation

class TimerViewModel(seconds: Int) : AFViewModel<TimerModel>() {
    override val initState: TimerModel = TimerModel(seconds = seconds)


    override suspend fun mutate(event: IEvent): Flow<IMutation> {
        when (event) {
            Start -> if (currentState.status != TimerStatus.Timing) {
                emit(SetStatus(TimerStatus.Timing))
                return flow<IMutation> {
                    while (true) {
                        emit(SetSeconds(currentState.seconds + 1))
                        delay(1000)
                    }
                }.withIndex().takeWhile {
                    it.index == 0 || currentState.status == TimerStatus.Timing
                }.map { it.value }
            }
            Pause -> return flowOf(SetStatus(TimerStatus.Pause))
            Stop -> return flowOf(SetStatus(TimerStatus.Normal), SetSeconds(0))
        }
        return super.mutate(event)
    }

    override fun reduce(state: TimerModel, mutation: IMutation): TimerModel {
        when (mutation) {
            is SetSeconds -> return state.copy(seconds = mutation.seconds)
            is SetStatus -> return state.copy(status = mutation.status)
        }
        return state
    }
}

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null

    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by lazy {
        ViewModelProvider(this, TimerFactory(50))[TimerViewModel::class.java]
    }

    @FlowPreview
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        viewModel.bind(this) {
            add({ seconds }, { binding.textView.text = "$this" })
        }

        viewModel.bindEvent(this) {
            add(binding.start.clicks(), Start)
            add(binding.pause.clicks(), { Pause })
            add(binding.stop.clicks(), { Stop })
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}

class TimerFactory(private val index: Int) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == TimerViewModel::class.java) {
            return TimerViewModel(index) as T
        }
        return super.create(modelClass)
    }
}