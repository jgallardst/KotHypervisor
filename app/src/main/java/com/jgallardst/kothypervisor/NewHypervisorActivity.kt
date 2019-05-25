package com.jgallardst.kothypervisor

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_new_hypervisor.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import java.util.*


class NewHypervisorActivity : AppCompatActivity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_hypervisor)
        val adapter =  ArrayAdapter.createFromResource(this, R.array.hosts, R.layout.support_simple_spinner_dropdown_item)
        spinner_host.adapter = adapter

        supportActionBar?.title = "Añadir conexion"

        new_hv_button.setOnClickListener {
            newPool()
        }
    }

    private fun newPool(){
        val uid = FirebaseAuth.getInstance().uid
        val mDB = FirebaseDatabase.getInstance()
        val dataId = UUID.randomUUID()

        val path = mDB.getReference("/$uid/connections/$dataId")

        val hostType = spinner_host.selectedItem.toString()
        val addr = et_dir.text.toString()
        var user = et_user.text.toString()
        var pass = et_pass.text.toString()
        val alias = et_alias.text.toString()

        if (hostType == "Xen") {

            if (alias.isEmpty() || addr.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                toast("Rellena todos los campos")
                return
            }
        } else {
            if (alias.isEmpty() || addr.isEmpty()) {
                toast("Rellena los campos de alias y direccion")
                return
            }
        }

        if(pass.isEmpty()) pass = ""
        if(user.isEmpty()) user = ""

        info { "User: $user. Address: $addr. Pass: $pass" }

        val connRef = ConnectionProperties(hostType, addr, user, pass, alias)
        path.setValue(connRef)
            .addOnSuccessListener {
                info{ "Succesfuly added connection to database"}
                val intent = Intent(this, HypervisorsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            .addOnFailureListener {
                error( "Failed writing user to database: ${it.message}")
            }
    }
}

