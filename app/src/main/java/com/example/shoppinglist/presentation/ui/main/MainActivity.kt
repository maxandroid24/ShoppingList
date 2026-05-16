package com.example.shoppinglist.presentation.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.shoppinglist.R
import com.example.shoppinglist.databinding.ActivityMainBinding
import com.example.shoppinglist.domain.utils.Resource
import com.example.shoppinglist.presentation.ui.auth.AuthViewModel
import com.example.shoppinglist.presentation.ui.group.GroupListFragment
import com.example.shoppinglist.presentation.ui.shoppinglist.ShoppingListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.currentUser.collect { user ->
                        if (user == null) {
                            android.util.Log.d("MainActivity", "User is null, signing in anonymously...")
                            authViewModel.signInAnonymously()
                        } else {
                            android.util.Log.d("MainActivity", "User found: ${user.id}")
                            
                            val hasGroupList = supportFragmentManager.findFragmentByTag("group_list_tag") != null
                            val hasShoppingList = supportFragmentManager.findFragmentByTag("shopping_list_tag") != null
                            
                            if (!hasGroupList && !hasShoppingList) {
                                android.util.Log.i("MainActivity", "No screen visible, redirecting to GroupListFragment")
                                showGroupListFragment()
                            }
                        }
                    }
                }

                launch {
                    authViewModel.authState.collect { state ->
                        if (state is Resource.Error) {
                            Toast.makeText(this@MainActivity, "Sign-in failed: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showGroupListFragment() {
        supportActionBar?.title = "My Groups"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.nav_host_fragment, GroupListFragment(), "group_list_tag")
        }
    }

    fun showShoppingListFragment(groupId: String) {
        val user = authViewModel.currentUser.value
        android.util.Log.i("MainActivity", "showShoppingListFragment triggered. Group: $groupId, User: ${user?.id}")
        
        if (user == null) {
            android.util.Log.e("MainActivity", "User context missing during navigation")
            Toast.makeText(this, "Login session expired, retrying...", Toast.LENGTH_SHORT).show()
            authViewModel.signInAnonymously()
            return
        }

        Toast.makeText(this, "Loading products...", Toast.LENGTH_SHORT).show()
        authViewModel.setActiveGroup(groupId)
        
        val fragment = ShoppingListFragment.newInstance(groupId, user.id, user.displayName ?: user.id)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        android.util.Log.i("MainActivity", "Starting fragment transaction for $groupId")
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.nav_host_fragment, fragment, "shopping_list_tag")
            addToBackStack("shopping_list")
        }
        android.util.Log.i("MainActivity", "Fragment transaction committed. Backstack count: ${supportFragmentManager.backStackEntryCount}")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
