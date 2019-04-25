package com.jgallardst.kothypervisor

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

class HypervisorsActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var inflater: LayoutInflater
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hypervisors)
        inflater = layoutInflater

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.hypervisors_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun newPool(){

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menu_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            R.id.menu_new_pool ->{
                newPool()
                info{ "New pool clicked"}
            }
            else -> warn {"Undefined option"}
        }
        return super.onOptionsItemSelected(item)
    }


}
