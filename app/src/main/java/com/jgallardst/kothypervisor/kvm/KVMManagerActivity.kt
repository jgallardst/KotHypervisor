package com.jgallardst.kothypervisor.kvm

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import com.jgallardst.kothypervisor.ConnectionProperties
import com.jgallardst.kothypervisor.HypervisorsActivity
import com.jgallardst.kothypervisor.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_kvmmanager.*
import kotlinx.android.synthetic.main.vm_rows.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.libvirt.*
import java.lang.Exception


class KVMManagerActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var connProperties : ConnectionProperties

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kvmmanager)


        supportActionBar?.title = "KVM VMs"


        kvm_rv.addItemDecoration( DividerItemDecoration(kvm_rv.context, DividerItemDecoration.VERTICAL))

        connProperties = intent.getParcelableExtra<ConnectionProperties>("connection")

        doAsync {
            try{
                val adapter = GroupAdapter<ViewHolder>()
                val ca = ConnectAuthDefault()
                val conn = Connect("qemu+tcp://${connProperties.address}/system", ca, 0)
                val ni = conn.nodeInfo()

                val numOfVMs = conn.numOfDomains()

                for (i in 1 until numOfVMs + 1)
                {
                    val vm = conn.domainLookupByID(i)
                    val kVM = KVirtualM(vm.name, vm.isActive, vm.maxVcpus, (vm.maxMemory / 1024))
                    adapter.add(KVMHolder(kVM))
                }

                uiThread {
                    kvm_rv.adapter = adapter
                }
            }	catch(e : Exception)
            {
                uiThread {
                    error {e.message}
                    toast("Fallo de conexion a KVM")
                    val intent = Intent(it, HypervisorsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }

            }

        }

    }
}

class KVMHolder(val vm: KVirtualM) : Item<ViewHolder>(){

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.fill_cpu_tv.text = vm.NCpus.toString()
        viewHolder.itemView.fill_mem.text = vm.memory.toString()
        viewHolder.itemView.fill_nombre_tv.text = vm.name
        if(vm.status == 0)
         viewHolder.itemView.fill_estado_tv.text = "Apagado"
        else viewHolder.itemView.fill_estado_tv.text = "Encendido"

    }

    override fun getLayout(): Int {
        return R.layout.vm_rows
    }

}
