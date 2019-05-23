package com.jgallardst.kothypervisor.xen

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.view.Menu
import android.view.MenuItem
import com.jgallardst.kothypervisor.ConnectionProperties
import com.jgallardst.kothypervisor.HypervisorsActivity
import com.jgallardst.kothypervisor.R
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
import java.lang.Exception
import java.net.URL

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
                    vmStats()
                    info {"Conexion correcta" }
                    toast("Conexion correcta")
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
                val host_ip = vm.getResidentOn(conn).getAddress(conn)
                val name = vm.getNameLabel(conn)
                val status = vm.getPowerState(conn).toString()
                var cpuUsage = 0.0
                var diskUsage = 0.0
                var memUsage = 0.0

                // TODO: Metricas

                uiThread {
                    val vm_holder = VirtualM(uuid, name, status, cpuUsage, memUsage, diskUsage)
                    VMList.add(vm_holder)
                    adapter.add(VMHolder(vm_holder))
                }
            }
            uiThread {
                vm_rv.adapter = adapter
            }
        }
    }
}

class VMHolder(val vm: VirtualM) : Item<ViewHolder>(){

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.fill_cpu_tv.text = "%.2f".format(vm.cpu_usage) + " %"
        viewHolder.itemView.fill_mem.text = "%.2f".format(vm.mem_usage) + " %"
        viewHolder.itemView.fill_dsk_tv.text = "%.2f".format(vm.disk_usage) + " %"
        viewHolder.itemView.fill_nombre_tv.text = vm.name
        viewHolder.itemView.fill_estado_tv.text = vm.status

    }

    override fun getLayout(): Int {
        return R.layout.vm_rows
    }

}