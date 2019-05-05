package com.jgallardst.kothypervisor.xen

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import com.jgallardst.kothypervisor.ConnectionProperties
import com.jgallardst.kothypervisor.HypervisorsActivity
import com.jgallardst.kothypervisor.R
import com.xensource.xenapi.APIVersion
import com.xensource.xenapi.Connection
import com.xensource.xenapi.Session
import org.jetbrains.anko.*
import java.lang.Exception
import java.net.URL

class PoolViewerActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var connProperties : ConnectionProperties
    private lateinit var conn : Connection
    private lateinit var session : Session

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connProperties = intent.getParcelableExtra<ConnectionProperties>("connection")
        info {"Attempting connection to ip ${connProperties.address}"}
        doAsync {
            Looper.prepare()
            try {
                conn = Connection(URL("http://${connProperties.address}"), "0")
                session = Session.loginWithPassword(
                    conn,
                    connProperties.user,
                    connProperties.pass,
                    APIVersion.latest().toString()
                )
                info { "Conexion correcta" }
                toast("Conexion correcta")
            } catch (e : Exception) {
                error { e.message }
                toast("Conexion fallida")
                val intent = Intent(this@PoolViewerActivity, HypervisorsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }

        }
        setContentView(R.layout.activity_pool_viewer)
    }
}
