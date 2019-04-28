package com.t0p47.faceidentification;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridView;

import com.google.firebase.crash.FirebaseCrash;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.t0p47.faceidentification.helper.IdentificationApp;
import com.t0p47.faceidentification.helper.StorageHelper;
import com.t0p47.faceidentification.personmanager.PersonActivity;
import com.t0p47.faceidentification.personmanager.PersonGroupActivity;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    class AddPersonGroupTask extends AsyncTask<String, String, String> {

        boolean mAddPerson;

        AddPersonGroupTask(boolean addPerson){
            mAddPerson = addPerson;
        }

        @Override
        protected String doInBackground(String... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();
            try{
                publishProgress("Syncing with server to add person group...");

                Log.d("LOG_TAG","MainActivity(My): "+params[0]);

                //Start creating person group in server
                faceServiceClient.createLargePersonGroup(
                        params[0],
                        getString(R.string.user_provided_person_group_name),
                        getString(R.string.user_provided_person_group_description_data));

                return  params[0];
            }catch(Exception e){
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute(){
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... progress){
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(String result){
            progressDialog.dismiss();

            if(result != null){

                if(mAddPerson){
                    Log.d("LOG_TAG", "MainActivity: group added, add person");
                    //addPerson();
                }else{
                    Log.d("LOG_TAG", "MainActivity: group added, doneAndSave");
                    //doneAndSave(false);
                }

            }else{
                Log.d("LOG_TAG", "MainActivity: NO RESULT!");
            }

        }
    }


    String personGroupId;
    ProgressDialog progressDialog;

    private void setUiBeforeBackgroundTask(){
        progressDialog.show();
    }

    private void setUiDuringBackgroundTask(String progress){
        progressDialog.setMessage(progress);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progress_dialog_title));

        personGroupId = StorageHelper.getPersonGroupId(this);
        if(personGroupId.isEmpty()){
            Log.d("LOG_TAG","MainActivity: no personGroupId, create new");

            String newPersonGroupId = UUID.randomUUID().toString();
            StorageHelper.setPersonGroupName(newPersonGroupId, "01", this);

            new AddPersonGroupTask(false).execute(newPersonGroupId);
            personGroupId = newPersonGroupId;
        }else{
            Log.d("LOG_TAG","MainActivity: personGroupId exist: "+personGroupId);

        }
    }

    public void ManagePersonGroup(View view){


        Intent intent = new Intent(this, PersonActivity.class);
        intent.putExtra("AddNewPersonGroup", true);
        intent.putExtra("PersonGroupName", "");
        intent.putExtra("PersonGroupId", personGroupId);
        startActivity(intent);
    }

    public void IdentificatePersonByPhoto(View view){
        startActivity(new Intent(this, IdentificationActivity.class));
    }
}
