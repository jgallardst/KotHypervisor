package com.jgallardst.kothypervisor.kvm

class KVirtualM(val name: String, val status : Int, val NCpus : Int, val memory: Long){
    constructor() : this("", 0, 0, 0L)

}