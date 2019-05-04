package com.jgallardst.kothypervisor


class Connection(val host: String, val address: String, val user: String, val pass: String, val alias: String){
    constructor(): this("","","", "", "")
}