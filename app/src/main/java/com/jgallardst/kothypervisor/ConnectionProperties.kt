package com.jgallardst.kothypervisor

import android.os.Parcel
import android.os.Parcelable


class ConnectionProperties(val host: String, val address: String, val user: String, val pass: String, val alias: String) :
    Parcelable {
    constructor() : this("", "", "", "", "")

    constructor(source: Parcel) : this(
        source.readString(),
        source.readString(),
        source.readString(),
        source.readString(),
        source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(host)
        writeString(address)
        writeString(user)
        writeString(pass)
        writeString(alias)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ConnectionProperties> = object : Parcelable.Creator<ConnectionProperties> {
            override fun createFromParcel(source: Parcel): ConnectionProperties = ConnectionProperties(source)
            override fun newArray(size: Int): Array<ConnectionProperties?> = arrayOfNulls(size)
        }
    }
}