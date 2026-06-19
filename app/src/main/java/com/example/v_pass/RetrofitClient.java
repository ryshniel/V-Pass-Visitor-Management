package com.example.v_pass;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // 1. IP address used if you are running on the Android Studio Emulator
    private static final String EMULATOR_URL = "http://10.0.2.2:3000/";

    // 2. IP address used if you are plugging in a physical phone
    // (Replace 192.168.1.X with your laptop's actual Wi-Fi IP address later)
    private static final String PHYSICAL_PHONE_URL = "http://192.168.1.X:3000/";

    private static Retrofit retrofit = null;

    public static VPassApiService getApiService(boolean useEmulator) {
        String baseUrl = useEmulator ? EMULATOR_URL : PHYSICAL_PHONE_URL;

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(VPassApiService.class);
    }
}
