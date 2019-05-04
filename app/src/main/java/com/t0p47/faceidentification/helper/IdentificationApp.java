package com.t0p47.faceidentification.helper;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.os.StrictMode;

import com.crashlytics.android.Crashlytics;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.t0p47.faceidentification.R;
import com.t0p47.faceidentification.db.AppDatabase;

import io.fabric.sdk.android.Fabric;

public class IdentificationApp extends Application {

    public static IdentificationApp instance;

    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = Room.databaseBuilder(this, AppDatabase.class, "database")
                .build();

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Fabric.with(this, new Crashlytics());
        sFaceServiceClient = new FaceServiceRestClient(getString(R.string.endpoint), getString(R.string.subscription_key));
    }

    public static IdentificationApp getInstance(){
        return instance;
    }

    public AppDatabase getDatabase(){
        return database;
    }

    public static FaceServiceClient getFaceServiceClient() {
        return sFaceServiceClient;
    }

    private static FaceServiceClient sFaceServiceClient;
}
