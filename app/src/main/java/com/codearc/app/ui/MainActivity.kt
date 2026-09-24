package com.codearc.app.ui
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.codearc.app.R
import com.codearc.app.data.Preferences
import com.codearc.app.data.CodeArcDatabase
import com.codearc.app.data.AppMetadata
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.fragment.app.Fragment
import androidx.lifecycle.withResumed
class MainActivity : AppCompatActivity() {
 private var ready = false
 private var destination = "Home"
 override fun onCreate(savedInstanceState: Bundle?) {
 val splash = installSplashScreen()
 super.onCreate(savedInstanceState)
 splash.setKeepOnScreenCondition { !ready }
 setContentView(R.layout.activity_main)
 destination = savedInstanceState?.getString("destination") ?: "Home"
 findViewById<BottomNavigationView>(R.id.navigation).setOnItemSelectedListener {
 when(it.itemId) {
 R.id.nav_add -> { CreationSheet().show(supportFragmentManager, "create"); false }
 else -> { navigate(when(it.itemId) { R.id.nav_projects -> "Projects"; R.id.nav_learn -> "Learn"; R.id.nav_settings -> "Settings"; else -> "Home" }); true }
 }
 }
 findViewById<View>(R.id.create).setOnClickListener { CreationSheet().show(supportFragmentManager, "create") }
 onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
 override fun handleOnBackPressed() {
 val current = supportFragmentManager.findFragmentById(R.id.content)
 if (current is OnboardingFragment && current.previous()) return
 if ((current is PageFragment || current is ProjectsFragment) && destination != "Home") select("Home")
 else { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
 }
 })
 lifecycleScope.launch {
 val completed = Preferences(applicationContext).onboardingCompleted.first()
 if (savedInstanceState == null || supportFragmentManager.findFragmentById(R.id.content) is BrandFragment) {
 findViewById<View>(R.id.navigation_container).visibility = View.GONE
 supportFragmentManager.beginTransaction().replace(R.id.content, BrandFragment()).commitNow()
 ready = true
 delay(650)
 lifecycle.withResumed {
 if(completed) navigate("Home") else supportFragmentManager.beginTransaction().replace(R.id.content, OnboardingFragment()).commit()
 }
 }
 findViewById<View>(R.id.navigation_container).visibility = if(completed) View.VISIBLE else View.GONE
 ready = true
 CodeArcDatabase.get(applicationContext).metadata().put(AppMetadata("foundation_version", "1"))
 }
 }
 fun startCoding() {
 lifecycleScope.launch {
 Preferences(applicationContext).completeOnboarding()
 findViewById<View>(R.id.navigation_container).visibility = View.VISIBLE
 select("Home")
 }
 }
 fun select(name: String) {
 findViewById<BottomNavigationView>(R.id.navigation).selectedItemId = when(name) { "Projects" -> R.id.nav_projects; "Learn" -> R.id.nav_learn; "Settings" -> R.id.nav_settings; else -> R.id.nav_home }
 }
 private fun navigate(name: String) {
 destination = name
 supportFragmentManager.beginTransaction().replace(R.id.content, (if(name == "Projects") ProjectsFragment() else PageFragment.newInstance(name))).commit()
 }
 override fun onSaveInstanceState(outState: Bundle) { outState.putString("destination", destination); super.onSaveInstanceState(outState) }
}

class BrandFragment : Fragment(R.layout.fragment_brand)
