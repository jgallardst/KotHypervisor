package com.jgallardst.kothypervisor

import android.content.DialogInterface
import android.content.Intent
import android.icu.lang.UCharacter.GraphemeClusterBreak.V
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AlertDialog
import android.text.Editable
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jgallardst.kothypervisor.R.id.*
import com.jgallardst.kothypervisor.xen.VMSActivity
import com.xensource.xenapi.*
import kotlinx.android.synthetic.main.activity_vmmanager.*
import org.jetbrains.anko.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.net.URL
import java.util.*



class VMManagerActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var connProperties : ConnectionProperties
    private lateinit var conn : Connection
    private lateinit var session : Session
    private var vm : VM? = null
    private lateinit var status : String
    private var ip_host : String? = "0.0.0.0"
    private var vm_ip : String? = "0.0.0.0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vmmanager)

        migrate_button.isClickable = false
        estado_button.isClickable = false

        val vm_uuid = intent.getStringExtra("uuid")
        val cpu = intent.getDoubleExtra("cpu", 0.0)
        val mem = intent.getDoubleExtra("mem", 0.0)
        connProperties = intent.getParcelableExtra<ConnectionProperties>("connection")

        status = intent.getStringExtra("status")


        doAsync {
            try {
                conn = Connection(URL("http://${connProperties.address}"), "0")
                session = Session.loginWithPassword(
                    conn,
                    connProperties.user,
                    connProperties.pass,
                    APIVersion.latest().toString()
                )

                vm = pickVM(VM.getAll(conn), vm_uuid)

                if(status.toLowerCase() == "running") {
                    ip_host = vm?.getResidentOn(conn)?.getAddress(conn)

                    info { "Ip del pool: $ip_host" }
                }

                getIp(vm_uuid)


                uiThread {
                    if (vm == null) {
                        toast("Imposible cargar VM $vm_uuid")
                        val intent = Intent(it, HypervisorsActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    } else {
                        toast("VM $vm_uuid cargada")
                        migrate_button.isClickable = true
                        estado_button.isClickable = true
                        migrate_button.alpha = 1.0f
                        estado_button.alpha = 1.0f
                        populateButtons()
                    }
                }
            } catch (e: Exception) {
                uiThread {
                    error { e.message }
                    toast("Conexion fallida")
                    val intent = Intent(it, HypervisorsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
            }
        }


        if(status.toLowerCase() == "running") {
            cpu_fill_tv.text = "%.2f".format(cpu) + " %"
            info{"$mem"}
            if (mem != -1.0) {
                mem_fill_tv.text = "%.2f".format(mem) + " %"
                check_tv.setOnClickListener {
                    if(pass_et.text.toString().isEmpty() || user_et.text.toString().isEmpty() || proc_et.text.toString().isEmpty()) {
                        toast("Rellena los campos.")
                        return@setOnClickListener
                    }
                    getProcess(user_et.text.toString(), pass_et.text.toString(), proc_et.text.toString())
                }
            }
            else {
                mem_fill_tv.text = "No guest additions."
                check_tv.text = "No guest additions."
                check_tv.isClickable = false
            }
        } else {
            cpu_fill_tv.text = "Maquina apagada."
            mem_fill_tv.text = "Maquina apagada."
            check_tv.text = "Maquina apagada"
            check_tv.isClickable = false
        }


    }

    private fun getProcess(user: String, pass: String, proc: String) {
        doAsync {
            try {
                val jsch = JSch()
                val session = jsch.getSession(user, vm_ip, 22)
                session.setPassword(pass)
                val command = "service $proc status | grep Active"

                val config = Properties()
                config["StrictHostKeyChecking"] = "no"
                config["PreferredAuthentications"] = "password"
                session.setConfig(config)
                session.connect()

                // SSH Channel
                val channelssh = session.openChannel("exec") as ChannelExec
                var baos = ByteArrayOutputStream()
                channelssh.outputStream = baos
                info { "Command: $command" }
                channelssh.setCommand(command)
                channelssh.connect()
                while (channelssh.isConnected) {
                    continue
                }


                val ret = baos.toString()
                info {"$ret"}
                uiThread {
                    toast(ret)
                }
            } catch (e: Exception) {
                uiThread {

                    error { "${e.message}" }
                    toast("Error leyendo el estado de $proc")
                }
            }
        }
    }

    private fun getIp(uuid: String){
        doAsync {

            uiThread {
                info { "Getting ip..." }
            }
            val command = "xe vm-list params=networks uuid=" + uuid
            val jsch = JSch()
            val session = jsch.getSession(connProperties.user, ip_host, 22)
            session.setPassword(connProperties.pass)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            config["PreferredAuthentications"] = "password"
            session.setConfig(config)
            session.connect()

            // SSH Channel
            val channelssh = session.openChannel("exec") as ChannelExec
            var baos = ByteArrayOutputStream()
            channelssh.outputStream = baos
            uiThread {
                info { "Command: $command" }
            }

            channelssh.setCommand(command)
            channelssh.connect()

            while (channelssh.isConnected) {
                continue
            }

            channelssh.disconnect()

            val toParse = baos.toString().split(" ")

            for (part in toParse) {
                if (part.startsWith("1")) {
                    uiThread {
                        var ip : String = "0.0.0.0"
                        if (part.endsWith(";")){
                            ip = part.dropLast(1)
                        } else ip = part
                        info { "IP addr: $ip" }
                        vm_ip = ip
                    }
                }
            }
        }
    }

    private fun pickVM(VMs : Set<VM>, uuid: String) : VM? {
        for (vm in VMs) {
            if (vm.getUuid(conn) == uuid){
                return vm
            }
        }
        return null
    }

    private fun populateButtons() {
        estado_button.setOnClickListener {
            cambiarEstadoVm()
        }
        migrate_button.setOnClickListener {
            migrarVM()
        }
    }

    private fun cambiarEstadoVm(){
        doAsync {
            if(status.toLowerCase() == "running") {
                uiThread {
                    toast("Apagando...")
                }
                vm?.cleanShutdown(conn)
                uiThread {
                    val intent = Intent(it, VMSActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("connection", connProperties)
                    startActivity(intent)
                }
            }
            else if (status.toLowerCase() == "halted"){
                uiThread {
                    toast("Encendiendo...")
                }
                vm?.start(conn, false, false)
                uiThread {
                    val intent = Intent(it, VMSActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("connection", connProperties)
                    startActivity(intent)
                }
            } else {
                uiThread {
                    toast("Maquina en estado $status, imposible realizar accion")

                }
            }
        }
    }

    private fun migrarVM() {
        toast("Iniciando migracion, consultando pools.")
        doAsync {
            val hosts = Host.getAll(conn)
            val hostsList = ArrayList<Host>()
            hostsList.addAll(hosts)
            val adresses = ArrayList<String>()
            for (h in hosts){
                adresses.add(h.getAddress(conn))
            }
            var selected : Host? = null
            uiThread {
                toast("Pools consultados.")
            }
            if(status.toLowerCase() == "running") {
                uiThread {
                    val builder = AlertDialog.Builder(it)
                    builder.setTitle("Elige un pool")
                    builder.setSingleChoiceItems(adresses.toTypedArray(), -1) { dialog, which ->
                        val sel = hostsList[which]

                        try {
                            selected = sel
                            info {"Seleccionado pool ${adresses[which]}."}
                            toast("Seleccionado pool ${adresses[which]}.")
                            doAsync {
                                migrateTo(selected)
                            }
                        } catch (e: IllegalArgumentException) {
                            // Catch the color string parse exception
                            toast("Error migrando de pool.")
                        }

                        dialog.dismiss()

                    }

                    builder.setNeutralButton("Cancelar") { dialog, which ->
                        // Do something when click the neutral button
                        dialog.cancel()
                    }

                    builder.create().show()
                }
            } else if(status.toLowerCase() == "halted") {
                uiThread {
                    val builder = AlertDialog.Builder(it)
                    builder.setTitle("Elige un pool")
                    builder.setSingleChoiceItems(adresses.toTypedArray(), -1) { dialog, which ->
                        val sel = hostsList[which]

                        try {
                            selected = sel
                            info {"Seleccionado pool ${adresses[which]}."}
                            toast("Seleccionado pool ${adresses[which]}.")
                           // migrateTo(selected)

                        } catch (e: IllegalArgumentException) {
                            // Catch the color string parse exception
                            toast("Error migrando de pool.")
                        }
                        dialog.dismiss()

                    }

                    builder.setNeutralButton("Cancelar") { dialog, which ->
                        // Do something when click the neutral button
                        dialog.cancel()
                    }

                    builder.create().show()

                }
            } else {
                toast("Error migrando de pool, estado de la maquina incorrecto.")
            }
        }
    }

    private fun migrateTo(h : Host?){
        info { "Migrando a ${h?.getAddress(conn)}" }
        val options = HashMap<String, String>()
        options["live"] = "true"
        doAsync {
            try {
                vm?.poolMigrate(conn, h, options )
            } catch (e : Exception){
                error{"Fallo migracion por: ${e.message}"}
            }
            uiThread {
                toast("Migracion completa.")
            }
        }

    }

    override fun onBackPressed() {
        val intent = Intent(this, VMSActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra("connection", connProperties)
        startActivity(intent)
    }
}
