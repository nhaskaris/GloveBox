package com.eliteonetube.glovebox.data.api

import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Single shared NHTSA vPIC client; the response models use Moshi codegen adapters. */
object VinApiClient {
    val api: VinDecoderApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://vpic.nhtsa.dot.gov/api/")
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(VinDecoderApi::class.java)
    }
}
