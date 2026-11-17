package com.estebanmiretti.gps

import retrofit2.Call
import retrofit2.http.GET

interface ApiDataInterface {

    @GET("avisocorto_GeoRSS.xml")
    fun getData(): Call<Rss>
}
