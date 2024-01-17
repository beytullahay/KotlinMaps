package com.example.kotlinmaps.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

// Room ile db işlemleri
@Entity
class Place(
    @ColumnInfo("name") // db ismi
    var name : String,
    @ColumnInfo("latitude")
    var latitude : Double,
    @ColumnInfo("longitude")
    var longitude : Double,
    ) : Serializable {

    @PrimaryKey(autoGenerate = true) // kendin arttır generic et.
    var id = 0

}