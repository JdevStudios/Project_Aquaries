package com.chemsgr.blu.notify;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by CodeSlu on 9/4/2018.
 */

public interface FCM {
    @Headers({

            "Content-Type: application/json",
            "Authorization:key=AAAAdkZFRNs:APA91bFA3suLhTI9fNI9CvVGGT96uw01fUrR4MuK-0g8QBT33hPH6Xo6T1kB5xNY4oNUXukruRKx6hfbtWw6-k5behImay-yXTxzoT49DN8E-vVdM9HU3TGClh7P3ccJ-4JBPJxv3rXy"
    })
    @POST("fcm/send")
    Call<FCMresp> send(@Body Sender body);
}
