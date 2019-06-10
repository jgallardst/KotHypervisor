package com.jgallardst.kothypervisor.xen

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.view.Menu
import android.view.MenuItem
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jgallardst.kothypervisor.ConnectionProperties
import com.jgallardst.kothypervisor.HypervisorsActivity
import com.jgallardst.kothypervisor.R
import com.jgallardst.kothypervisor.VMManagerActivity
import com.xensource.xenapi.APIVersion
import com.xensource.xenapi.Connection
import com.xensource.xenapi.Session
import com.xensource.xenapi.VM
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_vms.*
import kotlinx.android.synthetic.main.vm_rows.view.*
import org.jetbrains.anko.*
import java.io.ByteArrayOutputStream
import java.lang.Double.parseDouble
import java.lang.Exception
import java.lang.NumberFormatException
import java.net.URL
import java.util.Properties

class VMSActivity : AppCompatActivity() , AnkoLogger{

    private lateinit var connProperties : ConnectionProperties
    private lateinit var conn : Connection
    private lateinit var session : Session
    private lateinit var VMs : Set<VM>
    private lateinit var VMList : MutableList<VirtualM>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vms)
        supportActionBar?.title = "VMs"
        vm_rv.addItemDecoration( DividerItemDecoration(vm_rv.context, DividerItemDecoration.VERTICAL))

        connProperties = intent.getParcelableExtra<ConnectionProperties>("connection")
        VMList = mutableListOf()

        info {"Attempting connection to ip ${connProperties.address}"}
        doAsync {
            try {
                conn = Connection(URL("http://${connProperties.address}"), "0")
                session = Session.loginWithPassword(
                    conn,
                    connProperties.user,
                    connProperties.pass,
                    APIVersion.latest().toString()
                )

                VMs = VM.getAll(conn)

                uiThread {
                    toast("Cargando VMs (Esto puede tardar)")
                    vmStats()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.vm_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        val intent = Intent(this, HypervisorsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId){
            R.id.menu_pool -> {
                val intent = Intent(this, PoolViewerActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putExtra("connection", connProperties)
                startActivity(intent)
            }
            else -> warn {"Undefined option"}
        }
        return super.onOptionsItemSelected(item)
    }

    private fun vmStats(){
        val adapter = GroupAdapter<ViewHolder>()
        VMList = mutableListOf()
        doAsync {
            for (vm in VMs) {
                if (vm.getIsControlDomain(conn) or  vm.getIsASnapshot(conn) or vm.getIsATemplate(conn)) continue
                val uuid = vm.getUuid(conn)
                val name = vm.getNameLabel(conn)
                val status = vm.getPowerState(conn).toString()
                var cpuUsage = 0.0
                var diskUsage = 0.0
                var memUsage = 0.0

                if(status.toLowerCase() == "running"){
                    val addr = vm.getResidentOn(conn).getAddress(conn)
                    info{"VM $name running on $addr"}
                    val cpuNum = vm.getMetrics(conn).getVCPUsNumber(conn)
                    cpuUsage = getCPUMetrics(addr, uuid, cpuNum)
                    val pvd = vm.getGuestMetrics(conn).getPVDriversVersion(conn)
                    val version = pvd.get("micro")

                    if(version.toString() != "-1") {
                        memUsage = getMemMetrics(addr, uuid)
                        diskUsage = -1.0
                    }
                    else {
                        memUsage = -1.0
                        diskUsage = -1.0
                    }
                }

                uiThread {
                    val vm_holder = VirtualM(uuid, name, status, cpuUsage, memUsage, diskUsage)
                    VMList.add(vm_holder)
                    adapter.add(VMHolder(vm_holder))
                }
            }
            uiThread {
                vm_rv.adapter = adapter
                toast("VM cargadas")
            }

            adapter.setOnItemClickListener { item, view ->
                val intent = Intent(view.context, VMManagerActivity::class.java)
                val pos = adapter.getAdapterPosition(item)
                intent.putExtra("uuid", VMList.get(pos).uuid)
                intent.putExtra("status", VMList.get(pos).status)
                intent.putExtra("connection", connProperties)
                intent.putExtra("mem", VMList.get(pos).mem_usage)
                intent.putExtra("dsk", VMList.get(pos).disk_usage)
                intent.putExtra("cpu", VMList.get(pos).cpu_usage)
                startActivity(intent)
            }
        }
    }

    private fun getCPUMetrics(host_addr : String, uuid: String, cpuNum : Long) : Double {
        val jsch = JSch()
        val session = jsch.getSession(connProperties.user, host_addr, 22)
        session.setPassword(connProperties.pass)
        var cpu : Double = 0.0

        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        config["PreferredAuthentications"] = "password"
        session.setConfig(config)
        session.connect()
        for (i in 0..cpuNum - 1) {

            // SSH Channel
            val channelssh = session.openChannel("exec") as ChannelExec
            val baos = ByteArrayOutputStream()
            channelssh.outputStream = baos
            val command = "xe vm-data-source-query data-source=cpu" + i.toString() + " uuid=" + uuid
            info { "Command: $command" }
            channelssh.setCommand(command)
            channelssh.connect()
            while(channelssh.isConnected){
                continue
            }
            cpu += baos.toString().toDouble()

        }
        session.disconnect()

        return cpu / cpuNum
    }

    private fun getMemMetrics(host_addr : String, uuid: String) : Double {

        val jsch = JSch()
        val session = jsch.getSession(connProperties.user, host_addr, 22)
        session.setPassword(connProperties.pass)
        var total_mem  = 0.0
        var free_mem = 0.0
        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        config["PreferredAuthentications"] = "password"
        session.setConfig(config)
        session.connect()

        // SSH Channel
        var channelssh = session.openChannel("exec") as ChannelExec
        var baos = ByteArrayOutputStream()
        channelssh.outputStream = baos
        val total_mem_command = "xe vm-data-source-query data-source=memory" +  " uuid=" + uuid
        val free_mem_command = "xe vm-data-source-query data-source=memory_internal_free" +  " uuid=" + uuid

        info { "Command: $total_mem_command" }
        channelssh.setCommand(total_mem_command)
        channelssh.connect()
        while(channelssh.isConnected){
            continue
        }

        // Total mem en bits, free mem en kbits, convertimos.
        total_mem = baos.toString().toDouble() / 1024
        info {"Total mem: ${total_mem}"}

        channelssh = session.openChannel("exec") as ChannelExec
        val mem = ByteArrayOutputStream()
        channelssh.outputStream = mem
        info { "Command: $free_mem_command" }
        channelssh.setCommand(free_mem_command)
        try {
            channelssh.connect()
            while (channelssh.isConnected) {
                continue
            }
        } catch (e :JSchException) {
            warn { "No guest additions installed ${e.message}" }
            return -1.0
        }
        info {"Free memory: ${mem}" }

        if(mem.toString().isEmpty()) {
            warn { "No guest additions installed" }
            return -1.0
        }

        free_mem = parseDouble(mem.toString())

        session.disconnect()

        return (((total_mem - free_mem) / total_mem) * 100)
    }

    private fun getDiskUsage(host_addr : String, uuid: String) : Double {
        return 0.0
    }

}

class VMHolder(val vm: VirtualM) : Item<ViewHolder>(){

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.fill_cpu_tv.text = "%.2f".format(vm.cpu_usage) + " %"
        if(vm.mem_usage == -1.0)
            viewHolder.itemView.fill_mem.text = "N/A"
        else
            viewHolder.itemView.fill_mem.text = "%.2f".format(vm.mem_usage) + " %"
        viewHolder.itemView.fill_nombre_tv.text = vm.name
        viewHolder.itemView.fill_estado_tv.text = vm.status

    }

    override fun getLayout(): Int {
        return R.layout.vm_rows
    }

}