package com.t0p47.faceidentification;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.t0p47.faceidentification.db.AppDatabase;
import com.t0p47.faceidentification.db.entities.PersonGroup;
import com.t0p47.faceidentification.helper.IdentificationApp;
import com.t0p47.faceidentification.personmanager.PersonGroupActivity;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

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

                //Log.d("LOG_TAG","MainActivity(My): "+params[0]);

                //Start creating person group in server
                faceServiceClient.createLargePersonGroup(
                        params[0],
                        getString(R.string.user_provided_person_group_name),
                        getString(R.string.user_provided_person_group_description_data));

                return  params[0];
            }catch(Exception e){
                publishProgress(e.getMessage());
                Log.d("LOG_TAG", "MainActivity: group add error: "+e.getMessage());
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
                    Log.d("LOG_TAG", "MainActivity: group added, add person: "+result);
                    //addPerson();
                }else{
                    Log.d("LOG_TAG", "MainActivity: group added, doneAndSave: "+result);
                    doneAndSave();
                }



            }else{
                Log.d("LOG_TAG", "MainActivity: NO RESULT!");
            }

        }
    }

    class TrainPersonGroupTask extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();

            try{
                publishProgress("Training person group...");

                Log.d("LOG_TAG","MainActivity: train "+params[0]);
                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];
            }catch (Exception e){
                Log.d("LOG_TAG","MainActivity: trainError: "+e.getMessage());
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
            if(result != null){
                Log.d("LOG_TAG","MainActivity: train large group result: "+result);
                //finish();
            }
        }
    }

    private static String TAG = "LOG_TAG";

    String personGroupId;
    ProgressDialog progressDialog;
    AppDatabase db = IdentificationApp.getInstance().getDatabase();

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

        //Log.d(TAG, "MainActivity: applicationId: "+BuildConfig.APPLICATION_ID);

        //personGroupId = StorageHelper.getPersonGroupId(this);

        db.personGroupDao().getPersonGroupId()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<String>() {
                    @Override
                    public void onSuccess(String s) {
                        Log.d(TAG,"MainActivity: getPersonGroupId success: "+s);
                        personGroupId = s;
                        checkPersonGroup();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "MainActivity: can't get personGroupId: "+e.getMessage());
                        checkPersonGroup();
                    }
                });


    }

    private void checkPersonGroup(){
        if(personGroupId == null || personGroupId.isEmpty()){
            Log.d("LOG_TAG","MainActivity: no personGroupId, create new");

            String newPersonGroupId = UUID.randomUUID().toString();

            PersonGroup newPersonGroup = new PersonGroup();
            newPersonGroup.personGroupId = newPersonGroupId;
            newPersonGroup.personGroupName = "01";

            Callable<Long> clb = new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    long insertRes = db.personGroupDao().insertPersonGroup(newPersonGroup);
                    Log.d(TAG,"MainActivity: InsertingPersonGroup: insertRes: "+insertRes);
                    return insertRes;
                }
            };

            Completable.fromCallable(clb)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Log.d(TAG,"MainActivity: onSubscribe insertPersonGroup: "+d.toString());
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG,"MainActivity: onComplete insertPersonGroup: ");
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG,"MainActivity: onError insertPersonGroup: "+e.getMessage());
                        }
                    });

            new AddPersonGroupTask(false).execute(newPersonGroupId);
            personGroupId = newPersonGroupId;
        }else{
            Log.d("LOG_TAG","MainActivity: personGroupId exist: "+personGroupId);
        }
    }

    private void doneAndSave(){
        new TrainPersonGroupTask().execute(personGroupId);
    }

    public void ManagePersonGroup(View view){



        Intent intent = new Intent(this, PersonGroupActivity.class);
        intent.putExtra("AddNewPersonGroup", true);
        intent.putExtra("PersonGroupName", "");
        intent.putExtra("PersonGroupId", personGroupId);
        startActivity(intent);
    }

    public void IdentificatePersonByPhoto(View view){

        Intent intent = new Intent(this, IdentificationActivity.class);
        intent.putExtra("PersonGroupId", personGroupId);
        startActivity(intent);
    }

    private int longAction(String text){
        Log.d(TAG,"MainActivity: longAction");

        try{
            TimeUnit.SECONDS.sleep(1);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        return Integer.parseInt(text);
    }

    class CallableLongAction implements Callable<Integer>{

        private final String data;

        public CallableLongAction(String data){
            this.data = data;
        }

        @Override
        public Integer call() throws Exception{

            return longAction(data);
        }

    }

}
