package com.jgallardst.kothypervisor.xen

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.DividerItemDecoration
import android.view.Menu
import android.view.MenuItem
import com.jgallardst.kothypervisor.*
import com.xensource.xenapi.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_pool_viewer.*
import kotlinx.android.synthetic.main.pools_row.view.*
import org.jetbrains.anko.*
import java.lang.Exception
import java.net.URL


class PoolViewerActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var connProperties : ConnectionProperties
    private lateinit var conn : Connection
    private lateinit var session : Session
    private lateinit var hosts : Set<Host>
    private lateinit var PoolList : MutableList<Pool>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pool_viewer)

        supportActionBar?.title = "Pools"


        pools_rv.addItemDecoration( DividerItemDecoration(pools_rv.context, DividerItemDecoration.VERTICAL))
        PoolList = mutableListOf()
        connProperties = intent.getParcelableExtra<ConnectionProperties>("connection")
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
                hosts = Host.getAll(conn)


                uiThread {
                    toast ("Cargando Pools (Esto puede tardar)" )
                    hostsStats()
                }
            } catch (e : Exception) {
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
        menuInflater.inflate(R.menu.pool_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        val intent = Intent(this, HypervisorsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId){
            R.id.menu_vms -> {
                val intent = Intent(this, VMSActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putExtra("connection", connProperties)
                startActivity(intent)
            }
            else -> warn {"Undefined option"}
        }
        return super.onOptionsItemSelected(item)
    }


    private fun hostsStats() {
        val adapter = GroupAdapter<ViewHolder>()
        PoolList = mutableListOf()

        doAsync {
            for (host in hosts) {
                val uuid = host.getUuid(conn)
                val name = host.getNameLabel(conn)
                val total_cpus = host.getHostCPUs(conn).size
                var cpu_usage = 0.0
                for (hc in host.getHostCPUs(conn)) {
                    cpu_usage += hc.getUtilisation(conn)
                }
                cpu_usage /= total_cpus
                val mem_percent = (1 - (host.computeFreeMemory(conn) * 1.0 / host.getMetrics(conn).getMemoryTotal(conn))) *100;

                uiThread {
                    info {"Pool name: $name"}
                    val pool = Pool(uuid ,name, total_cpus, cpu_usage , mem_percent )
                    PoolList.add(pool)
                    adapter.add(PoolHolder(pool))
                }
            }
            uiThread {
                toast ("Pools cargados" )
                pools_rv.adapter = adapter
            }
        }
    }
}

class PoolHolder(val pool : Pool) : Item<ViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.pools_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.fetched_user_tv.text = "%.2f".format(pool.mem_usage) + " %"
        viewHolder.itemView.fetched_alias_tv.text = pool.name
        viewHolder.itemView.fetched_dir_tv.text = pool.total_cpus.toString()
        viewHolder.itemView.fetched_hv_tv.text = "%.2f".format(pool.cpu_usage) + " %"

    }

}
