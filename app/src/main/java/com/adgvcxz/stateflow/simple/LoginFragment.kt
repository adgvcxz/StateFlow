package com.adgvcxz.stateflow.simple

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.adgvcxz.stateflow.*
import com.adgvcxz.stateflow.simple.databinding.FragmentLoginBinding
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.android.widget.textChanges
import kotlin.random.Random

/**
 * Created by zhaowei on 2022/4/11.
 */

data class LoginModel(
    var email: String,
    var password: String,
    val success: Boolean
)

data class SetPassword(val password: String) : IEvent, IMutation
data class SetEmail(val email: String) : IEvent, IMutation
object Login : IEvent
data class SetSuccess(val success: Boolean) : IMutation

class LoginViewModel : AFViewModel<LoginModel>() {
    override val initState: LoginModel = LoginModel("", "", false)

    private suspend fun login(): Boolean {
        delay(2000)
        val result = Random.nextBoolean()
        if (!result) {
            throw Throwable("登陆错误")
        } else return result
    }

    override suspend fun mutate(event: IEvent): Flow<IMutation> {
        when (event) {
            Login -> return flow { emit(SetSuccess(login())) }.loading().catch()
        }
        return super.mutate(event)
    }

    override fun reduce(state: LoginModel, mutation: IMutation): LoginModel {
        when (mutation) {
            is SetPassword -> return state.copy(password = mutation.password)
            is SetEmail -> return state.copy(email = mutation.email)
        }
        return super.reduce(state, mutation)
    }
}

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null

    private val viewModel: LoginViewModel by lazy {
        ViewModelProvider(this)[LoginViewModel::class.java]
    }

    private val binding get() = _binding!!

    @OptIn(FlowPreview::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
//        binding.email.setText(viewModel.currentState.email)
//        binding.password.setText(viewModel.currentState.password)

        viewModel.bind(this) {
            add({ "邮箱: $email\n密码: $password" }, { binding.content.text = this })
        }
        viewModel.bindEvent(this) {
            add(binding.email.textChanges(), { SetEmail(this.toString()) })
            add(binding.password.textChanges(), { SetPassword(this.toString()) })
            add(binding.login.clicks(), Login)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun <T> Flow<T>.loading(): Flow<T> {
    return onStart {
        MainViewModel.uiEvent.emit(ShowLoadingDialog)
    }.onCompletion {
        MainViewModel.uiEvent.emit(HideLoadingDialog)
    }
}

fun <T> Flow<T>.catch(): Flow<T> {
    return this.catch { e ->
        MainViewModel.uiEvent.emit(ShowFailedMessage(e.localizedMessage ?: "error"))
        emptyFlow<T>()
    }
}