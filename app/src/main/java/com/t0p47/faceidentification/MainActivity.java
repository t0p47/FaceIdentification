package com.t0p47.faceidentification;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.crash.FirebaseCrash;
import com.t0p47.faceidentification.personmanager.PersonGroupActivity;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void ManagePersonGroup(View view){
        String personGroupId = UUID.randomUUID().toString();

        Intent intent = new Intent(this, PersonGroupActivity.class);
        intent.putExtra("AddNewPersonGroup", true);
        intent.putExtra("PersonGroupName", "");
        intent.putExtra("PersonGroupId", personGroupId);
        startActivity(intent);
    }

    public void IdentificatePersonByPhoto(View view){
        startActivity(new Intent(this, IdentificationActivity.class));
    }
}
