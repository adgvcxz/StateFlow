package com.adgvcxz.stateflow.simple

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.adgvcxz.stateflow.AFViewModel
import com.adgvcxz.stateflow.IEvent
import com.adgvcxz.stateflow.IMutation
import com.adgvcxz.stateflow.simple.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import reactivecircus.flowbinding.android.view.clicks


class MainViewModel(index: Int) : AFViewModel<Int>() {
    override val initState: Int = index

    override suspend fun mutate(event: IEvent): IMutation? {
        if (event is Add) {
            return withContext(Dispatchers.IO) {
                delay(event.time.toLong())
                SetValue(currentState * 2)
            }
        }
        return super.mutate(event)
    }

    override fun scan(state: Int, mutation: IMutation): Int {
        if (mutation is SetValue) return mutation.value
        return super.scan(state, mutation)
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, MainFactory(1))[MainViewModel::class.java]
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launchWhenCreated {
            viewModel.state.collect {
                Log.e("zhaow", "${System.currentTimeMillis()}")
                binding.textView.text = "$it"
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.state.distinctUntilChanged().collect {
                binding.textView1.text = "$it"
            }
        }
        binding.button1.clicks().onEach {
            viewModel.event.send(Add(1000))
        }.launchIn(lifecycleScope)
        binding.button2.clicks().onEach {
            viewModel.event.send(Add(2000))
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