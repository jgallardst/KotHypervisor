package com.jgallardst.kothypervisor

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xensource.xenapi.User
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.connections_row.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

class HypervisorsActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var inflater: LayoutInflater
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hypervisors)
        inflater = layoutInflater

        supportActionBar?.title = "Conexiones"

        fetchUsers()

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.hypervisors_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun newPool(){

        val intent = Intent(this, NewHypervisorActivity::class.java)
        startActivity(intent)

    }

    private fun fetchUsers() {
        val uid = FirebaseAuth.getInstance().uid
        val mconnDB = FirebaseDatabase.getInstance().getReference("/$uid/connections/")
        mconnDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {

                p0.children.forEach{
                    val conn : Connection? = it.getValue(Connection::class.java)
                    info( "Connection IP: ${conn?.address}" )
                }

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

class ConnectionHolder(val conn: Connection) : Item<ViewHolder>(){
    private val defaultURI = "https://firebasestorage.googleapis.com/v0/b/kotchat-test.appspot.com/o/images%2Fandroid_icon_256.png?alt=media&token=0f8f1d6e-f333-4c0a-92e1-2e5590bc413f"

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