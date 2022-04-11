package com.adgvcxz.stateflow.simple

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.adgvcxz.stateflow.AFViewModel
import com.adgvcxz.stateflow.IEvent
import com.adgvcxz.stateflow.simple.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.MutableSharedFlow


object MainModel

object ShowLoadingDialog : IEvent
object HideLoadingDialog : IEvent
data class ShowFailedMessage(val message: String) : IEvent

object MainViewModel : AFViewModel<MainModel>() {
    override val initState: MainModel = MainModel

    val uiEvent = MutableSharedFlow<IEvent>()

}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel by lazy { MainViewModel }

    private val loading by lazy {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(false) // if you want user to wait for some process to finish,

        builder.setView(R.layout.dialog_loading)
        builder.create()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.fragment_timer, R.id.fragment_login
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        lifecycleScope.launchWhenCreated {
            viewModel.uiEvent.collect {
                when(it) {
                    ShowLoadingDialog -> {
                        loading.show()
                    }
                    HideLoadingDialog -> {
                        loading.dismiss()
                    }
                    is ShowFailedMessage -> {
                        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


    }
}
