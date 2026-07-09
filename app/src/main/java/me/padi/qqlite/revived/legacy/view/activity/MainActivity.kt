package me.padi.qqlite.revived.legacy.view.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedService.OnScopeEventListener
import me.padi.qqlite.revived.App
import me.padi.qqlite.revived.databinding.ActivityMainBinding
import me.padi.qqlite.revived.di.RevivedKoin
import me.padi.qqlite.revived.shared.model.module.MainUiState
import me.padi.qqlite.revived.shared.viewmodel.module.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity(), App.ServiceStateListener {
    private var service: XposedService? = null
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModel()

    private val scopeCallback = object : OnScopeEventListener {
        override fun onScopeRequestApproved(approved: List<String>) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity, "onScopeRequestApproved: $approved", Toast.LENGTH_SHORT
                ).show()
                viewModel.updateScope(service?.scope)
            }
        }

        override fun onScopeRequestFailed(message: String) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity, "onScopeRequestFailed: $message", Toast.LENGTH_SHORT
                ).show()
                viewModel.updateScope(service?.scope)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        RevivedKoin.ensureStarted(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindActions()
        lifecycleScope.launch {
            viewModel.uiState.collectLatest(::render)
        }
    }

    override fun onStart() {
        super.onStart()
        App.addServiceStateListener(this, true)
    }

    override fun onStop() {
        App.removeServiceStateListener(this)
        super.onStop()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        this.service = service
        runOnUiThread {
            viewModel.updateService(service)
        }
    }

    private fun render(state: MainUiState) {
        binding.binder.text = state.binderText
        binding.api.text = state.apiText
        binding.framework.text = state.frameworkText
        binding.frameworkVersion.text = state.frameworkVersionText
        binding.frameworkVersionCode.text = state.frameworkVersionCodeText
        binding.frameworkProperties.text = state.frameworkPropertiesText
        binding.scope.text = state.scopeText
        binding.process.text = state.processText
        binding.reload.isEnabled = state.reloadEnabled
    }

    @SuppressLint("XposedNewApi")
    private fun bindActions() {
        binding.reload.setOnClickListener {
            if ((service?.apiVersion ?: 0) < 102) {
                Toast.makeText(this, "当前框架 API 不支持热重载", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val targets = service?.runningTargets.orEmpty()
            targets.forEach { target ->
                service?.hotReloadModule(target, null) { process, result ->
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Reload ${process.processName}, $result",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        binding.requestScope.setOnClickListener {
            service?.requestScope(listOf(TARGET_PACKAGE), scopeCallback)
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.tencent.qqlite"
    }
}
