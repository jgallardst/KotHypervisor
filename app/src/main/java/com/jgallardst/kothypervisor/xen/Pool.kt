package com.jgallardst.kothypervisor.xen

class Pool(val uuid: String, val name: String, val total_cpus: Int, val cpu_usage : Double, val mem_usage: Double){
    constructor() : this("", "", 0, 0.0, 0.0)

}