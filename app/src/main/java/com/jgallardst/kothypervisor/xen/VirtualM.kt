package com.jgallardst.kothypervisor.xen

class VirtualM(val uuid: String, val name: String, val status: String, val cpu_usage : Double, val mem_usage: Double, val disk_usage: Double){
    constructor() : this("", "", "", 0.0, 0.0, 0.0)

}