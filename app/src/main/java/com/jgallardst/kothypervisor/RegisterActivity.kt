package com.jgallardst.kothypervisor

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.jgallardst.kothypervisor.R.id.*
import kotlinx.android.synthetic.main.activity_register.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.toast
import org.jetbrains.anko.warn

class RegisterActivity : AppCompatActivity() , AnkoLogger{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        reg_button.setOnClickListener {
            performRegister()
        }

    }

    private fun performRegister(){
        val email = reg_email_edittext.text.toString()
        val pass = reg_pass_edittext.text.toString()

        if (pass.isEmpty() || email.isEmpty()) {
            toast("Rellena ambos campos.")
            warn {"No rellena ambos campos"}
        }

        val mAuth = FirebaseAuth.getInstance()


        mAuth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener
                else {
                    warn{ "Usuario creado"}
                    toast("Usuario registrado")

                    val intent = Intent(this, HypervisorsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
            }
            .addOnFailureListener {
                toast(it.message.toString())
                error {  "Failed to create user: ${it.message.toString()}"}
            }
    }
}
