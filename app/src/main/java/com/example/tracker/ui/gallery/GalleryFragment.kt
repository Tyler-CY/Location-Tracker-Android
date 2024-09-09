package com.example.tracker.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tracker.databinding.FragmentGalleryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val TAG = "GalleryFragment"

    private val DEBUGGING = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        auth = Firebase.auth
        if (DEBUGGING) {
            Firebase.auth.useEmulator("10.0.2.2", 9099)
        }

        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val usernameInput: TextView = binding.usernameInput
        val passwordInput: TextView = binding.passwordInput
        val signUpButton: Button = binding.signUpButton
        val loginButton: Button = binding.loginButton
        val logoutButton: Button = binding.logOutButton

        signUpButton.setOnClickListener {
            if (!usernameInput.text.isNullOrEmpty() && !passwordInput.text.isNullOrEmpty()) {
                auth.createUserWithEmailAndPassword(
                    usernameInput.text.toString(),
                    passwordInput.text.toString()
                ).addOnSuccessListener {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    Toast.makeText(
                        context,
                        "Sign up success.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }.addOnFailureListener { task ->
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.cause)
                    Toast.makeText(
                        context,
                        "Sign up failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }

        loginButton.setOnClickListener {
            if (!usernameInput.text.isNullOrEmpty() && !passwordInput.text.isNullOrEmpty()) {
                auth.signInWithEmailAndPassword(
                    usernameInput.text.toString(),
                    passwordInput.text.toString()
                ).addOnSuccessListener {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    Toast.makeText(
                        context,
                        "Login success.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }.addOnFailureListener { task ->
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.cause)
                    Toast.makeText(
                        context,
                        "Login failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }

        logoutButton.setOnClickListener {
            auth.signOut()
        }



        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}