package com.example.v_pass;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface VPassApiService {
    // This matches Route 2 from your Node.js index.js file
    @GET("api/v1/visitor/verify/{uid}")
    Call<VisitorResponse> verifyVisitor(@Path("uid") String uid);
}
