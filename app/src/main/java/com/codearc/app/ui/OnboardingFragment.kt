package com.codearc.app.ui
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.codearc.app.R
import com.google.android.material.button.MaterialButton
class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {
 private var index = 0
 private val titles = listOf("CodeArc", "Code Offline", "Online Power", "Learn Step by Step")
 private val descriptions = listOf("Learn. Code. Compile. Build.", "Write and run supported programs without internet.", "Use cloud compilers and AI when connected.", "Learn from Beginner to Intermediate and later Advanced.")
 override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 index = savedInstanceState?.getInt("slide") ?: 0
 view.findViewById<MaterialButton>(R.id.next).setOnClickListener { if(index < 3) { index++; render() } else { it.isEnabled = false; (requireActivity() as MainActivity).startCoding() } }
 view.findViewById<View>(R.id.previous).setOnClickListener { previous() }
 render()
 }
 fun previous(): Boolean { if(index == 0) return false; index--; render(); return true }
 private fun render() {
 val v = view ?: return
 v.findViewById<TextView>(R.id.title).text = titles[index]
 v.findViewById<TextView>(R.id.description).text = descriptions[index]
 v.findViewById<TextView>(R.id.step).text = "0${index + 1} / 04"
 v.findViewById<TextView>(R.id.dots).text = (0..3).joinToString("   ") { if(it == index) "●" else "○" }
 v.findViewById<MaterialButton>(R.id.next).text = if(index == 3) "Start Coding" else "Next"
 v.findViewById<View>(R.id.previous).visibility = if(index == 0) View.INVISIBLE else View.VISIBLE
 }
 override fun onSaveInstanceState(outState: Bundle) { outState.putInt("slide", index); super.onSaveInstanceState(outState) }
}
