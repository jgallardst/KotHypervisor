package com.jgallardst.kothypervisor

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.jgallardst.kothypervisor.xen.PoolViewerActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_hypervisors.*
import kotlinx.android.synthetic.main.connections_row.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.jetbrains.anko.warn


class HypervisorsActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var inflater: LayoutInflater
    private lateinit var connArray: MutableList<ConnectionProperties>
    private lateinit var refArray: MutableList<DatabaseReference>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hypervisors)
        inflater = layoutInflater



        supportActionBar?.title = "Conexiones"

        connections_rv.addItemDecoration( DividerItemDecoration(connections_rv.context, DividerItemDecoration.VERTICAL))

        fetchConns()

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.hypervisors_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun newPool(){

        val intent = Intent(this, NewHypervisorActivity::class.java)
        startActivity(intent)

    }

    private fun fetchConns() {
        connArray = mutableListOf()
        refArray = mutableListOf()

        val uid = FirebaseAuth.getInstance().uid
        val mconnDB = FirebaseDatabase.getInstance().getReference("/$uid/connections/")
        mconnDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                val adapter = GroupAdapter<ViewHolder>()

                p0.children.forEach{
                    val conn : ConnectionProperties? = it.getValue(ConnectionProperties::class.java)
                    refArray.add(it.ref)
                    info( "ConnectionProperties IP: ${conn?.address}" )
                    if(conn != null){
                        adapter.add(ConnectionHolder(conn))
                        connArray.add(conn)
                    }
                }
                adapter.setOnItemClickListener { item, view ->
                    val intent = Intent(view.context, PoolViewerActivity::class.java)
                    val pos = adapter.getAdapterPosition(item)
                    intent.putExtra("connection", connArray.get(pos))
                    startActivity(intent)
                }

                adapter.setOnItemLongClickListener { item, view ->
                    val pos = adapter.getAdapterPosition(item)
                    val builder = AlertDialog.Builder(view.context)
                    builder.setTitle("Borrar conexion").setMessage("¿Seguro que quieres eliminar la conexion?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Si"){dialog, which ->
                            refArray.get(pos).removeValue()
                            toast("Conexion eliminada")
                            fetchConns()
                        }
                        .setNegativeButton("No", null).show();
                    return@setOnItemLongClickListener true

                }
                connections_rv.adapter = adapter

            }

        })
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

class ConnectionHolder(val conn: ConnectionProperties) : Item<ViewHolder>(){

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.fetched_alias_tv.text = conn.alias
        viewHolder.itemView.fetched_dir_tv.text = conn.address
        viewHolder.itemView.fetched_hv_tv.text = conn.host
        viewHolder.itemView.fetched_user_tv.text = conn.user

    }

    override fun getLayout(): Int {
        return R.layout.connections_row
    }

}