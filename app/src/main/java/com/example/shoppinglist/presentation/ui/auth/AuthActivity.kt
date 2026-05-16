package com.example.shoppinglist.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.shoppinglist.databinding.ActivityAuthBinding
import com.example.shoppinglist.domain.utils.Resource
import com.example.shoppinglist.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeState()
        viewModel.signInAnonymously()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                            finish()
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this@AuthActivity, state.message, Toast.LENGTH_LONG).show()
                            // Maybe retry?
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}
