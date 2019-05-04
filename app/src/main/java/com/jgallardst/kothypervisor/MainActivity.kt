package com.jgallardst.kothypervisor

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.jetbrains.anko.warn

class MainActivity : AppCompatActivity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkUser()

        reg_tv.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        log_button.setOnClickListener {
            login()
        }
    }

    private fun checkUser(){
        val uid = FirebaseAuth.getInstance().uid


        if (uid == null){
           info { "No detected user, going to sign-up form"}
        } else {
            val user = FirebaseAuth.getInstance().currentUser

            val intent = Intent(this, HypervisorsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            info { "Logged in as ${uid}"}
        }
    }

    private fun login() {
        val email = login_email_edittext.text!!.toString() ?: ""
        val pass = login_pass_edittext.text!!.toString()  ?: ""

        if (pass.isEmpty() || email.isEmpty()) {
            toast("Rellena ambos campos.")
            warn {"No rellena ambos campos"}
            return
        }

        val mAuth = FirebaseAuth.getInstance()

        mAuth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener {
                if(!it.isSuccessful) {
                    toast("Login fallido")
                    warn { "Login fallido" }
                    return@addOnCompleteListener
                }
                else {
                    info {"Login completado"}
                    toast("Login completado")
                    val user = FirebaseAuth.getInstance().currentUser

                    val intent = Intent(this, HypervisorsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)

                }
            }
            .addOnFailureListener{
                warn {it.message}
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT)
            }
    }
}
