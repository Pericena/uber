package com.p1.uber.retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

   // private static Retrofit retrofit = null;

    public static Retrofit getClient(String url){
        //if (retrofit == null){
          Retrofit  retrofit = new Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
       // }
        return retrofit;
    }

    public static Retrofit getClientObject(String url){
        //if (retrofit == null){
          Retrofit  retrofit = new Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
       // }
        return retrofit;
    }
}
