package com.example.kotlinmaps.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.kotlinmaps.model.Place
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

@Dao
interface PlaceDao {

    @Query("SELECT *FROM Place")
    fun getAll():Flowable<List<Place>> // flowable rxjava dan geliyor

//    @Query("SELECT *FROM Place WHERE id = :id") // aşağıda istediğimiz :id ye göre filtreleme yapabiliriz
//    fun getAllCustom(id: String): List<Place>


    @Insert
    fun instert (place: Place) : Completable //  : Completable rxjava tamamlanabilir anlamına geliyor. async olarak yapmamızı sağlıyor.

    @Delete
    fun delete (place: Place) : Completable

}