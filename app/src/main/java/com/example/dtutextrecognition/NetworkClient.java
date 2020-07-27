package com.example.dtutextrecognition;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClient {
    private  static Retrofit retrofit;

    public static Retrofit getRetrofit(String url) {
        okhttp3.OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

//        return new Retrofit.Builder()
//                .setEndpoint(BuildConfig.BASE_URL)
//                .setConverter(new GsonConverter(gson))
//                .setClient(new OkClient(okHttpClient))
//                .build();

        if(retrofit==null) {
            retrofit = new Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create()).client(okHttpClient).build();


        }   return retrofit;
    }

}
