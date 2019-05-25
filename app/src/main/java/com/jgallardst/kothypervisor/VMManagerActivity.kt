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
    private var vm_host : String? = "0.0.0.0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vmmanager)

        migrate_button.isClickable = false
        estado_button.isClickable = false

        val vm_uuid = intent.getStringExtra("uuid")
        val cpu = intent.getDoubleExtra("cpu", 0.0)
        val mem = intent.getDoubleExtra("mem", 0.0)
        val dsk = intent.getDoubleExtra("dsk", 0.0)
        status = intent.getStringExtra("status")

        if(status.toLowerCase() == "running") {
            cpu_fill_tv.text = "%.2f".format(cpu) + " %"

            if (mem != -1.0) {
                mem_fill_tv.text = "%.2f".format(mem) + " %"
                vm_host = getIp(vm_uuid)
                check_tv.setOnClickListener {
                    if(pass_et.text.toString().isEmpty() || user_et.text.toString().isEmpty() || proc_et.text.toString().isEmpty()) {
                        toast("Rellena los campos.")
                        return@setOnClickListener
                    }
                    toast(getProcess(user_et.text.toString(), pass_et.text.toString(), proc_et.text.toString()))
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

        connProperties = intent.getParcelableExtra<ConnectionProperties>("connection")

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

                ip_host = vm?.getResidentOn(conn)?.getAddress(conn)

                info {"Ip del pool: $ip_host"}

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



    }

    private fun getProcess(user: String, pass: String, proc: String): String {
        try {
            val jsch = JSch()
            val session = jsch.getSession(user, vm_host, 22)
            session.setPassword(pass)
            var ret = "Error leyendo el estado de $proc"
            val command = "systemctl status $proc | grep Active"

            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            config["PreferredAuthentications"] = "password"
            session.setConfig(config)
            session.connect()

            // SSH Channel
            val channelssh = session.openChannel("exec") as ChannelExec
            var baos = ByteArrayOutputStream()
            channelssh.outputStream = baos
            baos = ByteArrayOutputStream()
            channelssh.outputStream = baos
            info { "Command: $command" }
            channelssh.setCommand(command)
            channelssh.connect(3000)
            while (channelssh.isConnected) {
                info { "Holding ssh vm command." }
                continue
            }

            if(baos.toString().split(" ").size > 2){
                ret = baos.toString().split(" ")[1]
            }

            return ret
        } catch (e : JSchException) {
            return ("Error leyendo el estado de $proc")
        }
    }

    private fun getIp(uuid: String) : String{
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

        channelssh.setCommand(command)
        channelssh.connect()

        while(channelssh.isConnected){
            continue
        }

        channelssh.disconnect()

        val toParse = baos.toString().split(" ")

        for (part in toParse){
            if(part.startsWith("1")) {
                info {"IP addr: $part"}
                return part
            }
        }

        return "0.0.0.0"

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
