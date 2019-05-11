package com.t0p47.faceidentification.personmanager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.t0p47.faceidentification.R;
import com.t0p47.faceidentification.db.AppDatabase;
import com.t0p47.faceidentification.db.entities.Person;
import com.t0p47.faceidentification.helper.IdentificationApp;
import com.t0p47.faceidentification.helper.StorageHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class PersonActivity extends AppCompatActivity {

    class  AddPersonTask extends AsyncTask<String, String,String>{

        boolean mAddFace;

        AddPersonTask(boolean addFace){
            mAddFace = addFace;
        }

        @Override
        protected String doInBackground(String... params){
            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();
            try{
                publishProgress("Syncing with server to add person");


                //Start the request to creating person.
                CreatePersonResult createPersonResult = faceServiceClient.createPersonInLargePersonGroup(
                        params[0],
                        getString(R.string.user_provided_person_name),
                        getString(R.string.user_provided_person_group_description_data));

                return createPersonResult.personId.toString();

            }catch (Exception e){
                publishProgress(e.getMessage());

                return null;
            }
        }

        @Override
        protected void onPreExecute(){
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onPostExecute(String result){
            progressDialog.dismiss();

            if(result != null){

                personId = result;
                Log.d("LOG_TAG","PersonActivity: personId of added person "+personId);

                if(mAddFace){
                    addFace();
                }else{
                    doneAndTrain();
                }
            }
        }
    }

    class DeleteFaceTask extends AsyncTask<String, String,String>{

        String mPersonGroupId;
        UUID mPersonId;

        DeleteFaceTask(String personGroupId, String personId){
            mPersonGroupId = personGroupId;
            mPersonId= UUID.fromString(personId);
        }

        @Override
        protected String doInBackground(String... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();
            try{
                publishProgress("Deleting selected faces...");

                UUID faceId = UUID.fromString(params[0]);
                faceServiceClient.deletePersonFaceInLargePersonGroup(personGroupId, mPersonId, faceId);
                return params[0];
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
                //TODO:Face successfully deleted
            }
        }
    }

    class TrainPersonGroupTask extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();

            try{
                publishProgress("Training person group...");

                Log.d("LOG_TAG","PersonActivity: train "+params[0]);
                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];
            }catch (Exception e){
                Log.d("LOG_TAG","PersonActivity: trainError: "+e.getMessage());
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
                Log.d("LOG_TAG","PersonActivity: train large group result: "+result);
                finish();
            }
        }
    }

    private void setUiBeforeBackgroundTask(){
        progressDialog.show();
    }


    private void setUiDuringBackgroundTask(String progress){
        progressDialog.setMessage(progress);
    }

    private static final String TAG = "LOG_TAG";

    boolean addNewPerson;
    String personId;
    String personGroupId;
    String oldPersonName;

    String receivedFaceUri;
    String receivedFaceId;

    private Uri mUriPhotoTaken;

    AppDatabase db = IdentificationApp.getInstance().getDatabase();

    private static final int REQUEST_SELECT_IMAGE = 0;
    private static final int REQUEST_ADD_FACE = 1;

    FaceGridViewAdapter faceGridViewAdapter;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            addNewPerson = bundle.getBoolean("AddNewPerson");
            personGroupId = bundle.getString("PersonGroupId");
            oldPersonName = bundle.getString("PersonName");
            if(!addNewPerson){
                personId = bundle.getString("PersonId");
            }
        }

        initializeGridView();

        EditText editTextPersonName = (EditText)findViewById(R.id.edit_person_name);
        editTextPersonName.setText(oldPersonName);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progress_dialog_title));
    }

    private void initializeGridView(){

        GridView gridView = (GridView) findViewById(R.id.gridView_faces);

        gridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        gridView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

                faceGridViewAdapter.faceChecked.set(position, checked);

                GridView gridView = (GridView) findViewById(R.id.gridView_faces);
                gridView.setAdapter(faceGridViewAdapter);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {

                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_delete_items, menu);

                faceGridViewAdapter.longPressed = true;

                GridView gridView = (GridView) findViewById(R.id.gridView_faces);
                gridView.setAdapter(faceGridViewAdapter);

                Button addNewItem = (Button) findViewById(R.id.add_face);
                addNewItem.setEnabled(false);

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                switch (item.getItemId()){
                    case R.id.menu_delete_items:
                        deleteSelectedItems();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                faceGridViewAdapter.longPressed =false;

                for(int i = 0;i< faceGridViewAdapter.faceIdList.size();++i){
                    faceGridViewAdapter.faceChecked.set(i, false);
                }

                GridView gridView = (GridView) findViewById(R.id.gridView_faces);
                gridView.setAdapter(faceGridViewAdapter);

                Button addNewItem = (Button) findViewById(R.id.add_face);
                addNewItem.setEnabled(true);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        faceGridViewAdapter = new FaceGridViewAdapter();
        GridView gridView = (GridView) findViewById(R.id.gridView_faces);
        gridView.setAdapter(faceGridViewAdapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);

        outState.putBoolean("AddNewPerson", addNewPerson);
        outState.putString("PersonId",personId);
        outState.putString("PersonGroupId", personGroupId);
        outState.putString("OldPersonName", oldPersonName);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);

        addNewPerson = savedInstanceState.getBoolean("AddNewPerson");
        personId = savedInstanceState.getString("PersonId");
        personGroupId = savedInstanceState.getString("PersonGroupId");
        oldPersonName = savedInstanceState.getString("OldPersonName");
    }

    public void doneAndSave(View view){
        if(personId == null){
            new AddPersonTask(false).execute(personGroupId);
        }else{
            doneAndTrain();
        }
    }

    public void addFace(View view){
        if(personId == null){
            new AddPersonTask(true).execute(personGroupId);
        }else{
            addFace();
        }
    }

    private void doneAndTrain(){

        TextView textWarning = (TextView)findViewById(R.id.info);
        EditText editeTextPersonName = (EditText)findViewById(R.id.edit_person_name);
        String newPersonName = editeTextPersonName.getText().toString();
        if(newPersonName.equals("")){
            textWarning.setText(R.string.person_name_empty_warning_message);
        }

        Log.d("LOG_TAG","PersonActivity: setPersonName: personId: "+ personId
            + ", newPersonName: "+newPersonName+", personGroupId: "+personGroupId);
        //StorageHelper.setPersonName(personId, newPersonName, personGroupId, PersonActivity.this);

        Person newPerson = new Person();
        newPerson.counter = 0;
        newPerson.firstName = newPersonName;
        newPerson.PassportNumber = "5014775864";
        newPerson.personId = personId;
        newPerson.faceIdList = new ArrayList<>();
        Log.d(TAG,"PersonActivity: receivedFaceId: "+receivedFaceId
            +", receivedFaceUri: "+receivedFaceUri);

        newPerson.faceIdList.add(receivedFaceId);

        //Completable.fromAction(() -> db.personDao().insertPerson(newPerson));
        Callable<Long> clbInsertPerson = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                Long insertPersonId = db.personDao().insertPerson(newPerson);
                Log.d(TAG,"PersonActivity: insertPerson, Id: "+insertPersonId);
                return null;
            }
        };

        Completable.fromCallable(clbInsertPerson)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG,"PersonActivity: onSubscribe insertPerson: "+d.toString());
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG,"PersonActivity: onComplete insertPerson: ");



                        new TrainPersonGroupTask().execute(personGroupId);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG,"PersonActivity: onError insertPerson: "+e.getMessage());
                    }
                });


        //new TrainPersonGroupTask().execute(personGroupId);
    }

    private void addFace(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager()) != null){
            //startActivityForResult(intent, REQUEST_SELECT_IMAGE);

            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try{
                File file = File.createTempFile("IMG_", ".jpg", storageDir);

                mUriPhotoTaken = Uri.fromFile(file);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                startActivityForResult(intent, REQUEST_SELECT_IMAGE);
            }catch(IOException e){
                Log.d("LOG_TAG","PersonActivity: " + e.getMessage());
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case REQUEST_SELECT_IMAGE:
                if(resultCode == RESULT_OK){
                    //Uri uriImagePicked = data.getData();

                    Uri imageUri;
                    if(data == null || data.getData() == null){
                        imageUri = mUriPhotoTaken;
                    }else{
                        imageUri = data.getData();
                    }


                    Intent intent = new Intent(this, AddFaceToPersonActivity.class);
                    intent.putExtra("PersonId", personId);
                    intent.putExtra("PersonGroupId", personGroupId);
                    Log.d("LOG_TAG", "PersonActivity: mImageUriStr: "+imageUri.toString());
                    intent.putExtra("ImageUriStr", imageUri.toString());
                    startActivityForResult(intent, REQUEST_ADD_FACE);
                }
                break;
            case REQUEST_ADD_FACE:

                if(resultCode == RESULT_OK){
                    receivedFaceUri = data.getStringExtra("FaceUri");
                    receivedFaceId = data.getStringExtra("FaceId");
                }

                break;
                default:
                    break;
        }
    }

    private void deleteSelectedItems(){
        List<String> newFaceIdList = new ArrayList<>();
        List<Boolean> newFaceChecked = new ArrayList<>();
        List<String> faceIdsToDelete = new ArrayList<>();
        for(int i =0;i< faceGridViewAdapter.faceChecked.size();++i){
            boolean checked = faceGridViewAdapter.faceChecked.get(i);
            if(checked){
                String faceId = faceGridViewAdapter.faceIdList.get(i);
                faceIdsToDelete.add(faceId);
                new DeleteFaceTask(personGroupId, personId).execute(faceId);
            }else{
                newFaceIdList.add(faceGridViewAdapter.faceIdList.get(i));
                newFaceChecked.add(false);
            }
        }

        //StorageHelper.deleteFaces(faceIdsToDelete,personId, this);

        //Completable.fromAction(() -> db.faceDao().deleteFaces(faceIdsToDelete));
        Callable<Integer> clbDeleteFaces = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                int deletedFacesCount = db.faceDao().deleteFaces(faceIdsToDelete);
                Log.d(TAG,"PersonActivity: deleteFaces: deletedFacesCount: "+deletedFacesCount);

                faceGridViewAdapter.faceIdList = newFaceIdList;
                faceGridViewAdapter.faceChecked= newFaceChecked;
                faceGridViewAdapter.notifyDataSetChanged();

                return deletedFacesCount;
            }
        };

        Completable.fromCallable(clbDeleteFaces)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG,"PersonActivity: deleteFaces: onSubscribe: "+d.toString());
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG,"PersonActivity: deleteFaces: onSubscribe: ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG,"PersonActivity: deleteFaces: onSubscribe: "+e.getMessage());
                    }
                });

        /*faceGridViewAdapter.faceIdList = newFaceIdList;
        faceGridViewAdapter.faceChecked= newFaceChecked;
        faceGridViewAdapter.notifyDataSetChanged();*/

    }

    private class FaceGridViewAdapter extends BaseAdapter{
        List<String> faceIdList;
        List<Boolean> faceChecked;
        boolean longPressed;

        FaceGridViewAdapter(){
            longPressed = false;
            faceIdList = new ArrayList<>();
            faceChecked = new ArrayList<>();

            //Set<String> faceIdSet = StorageHelper.getAllFaceIds(personId, PersonActivity.this);
            final List<String>[] receivedFaceIdList = new List[]{new ArrayList<>()};

            Log.d(TAG,"PersonActivity: beforeFaceIds");

            db.personDao().getAllFaceIds(personId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DisposableSingleObserver<List<String>>() {
                        @Override
                        public void onSuccess(List<String> strings) {
                            Log.d(TAG, "PersonActivity: onSuccess getAllFaceIds: "+strings.size());
                            receivedFaceIdList[0] = strings;
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG, "PersonActivity: onError getAllFaceIds: "+e.getMessage());
                        }
                    });

            for(String faceId: receivedFaceIdList[0]){
                faceIdList.add(faceId);
                faceChecked.add(false);
            }
        }

        @Override
        public int getCount(){
            return faceIdList.size();
        }

        @Override
        public Object getItem(int position){
            return faceIdList.get(position);
        }

        @Override
        public long getItemId(int position){
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent){

            if(convertView == null){
                LayoutInflater layoutInflater =
                        (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face_with_checkbox, parent, false);
            }
            convertView.setId(position);

            /*Uri uri =Uri.parse(StorageHelper.getFaceUri(
                    faceIdList.get(position), PersonActivity.this));*/

            final Uri[] uri = new Uri[1];

            db.faceDao().getFaceUri(faceIdList.get(position))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DisposableSingleObserver<String>() {
                        @Override
                        public void onSuccess(String s) {
                            Log.d(TAG,"PersonActivity: onSuccess getFaceUri: "+s);
                            uri[0] = Uri.parse(s);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG,"PersonActivity: onSuccess getFaceUri: "+e.getMessage());
                        }
                    });

            ((ImageView)convertView.findViewById(R.id.image_face)).setImageURI(uri[0]);

            //Set the checked status of the item
            CheckBox checkBox = (CheckBox)convertView.findViewById(R.id.checkbox_face);
            if(longPressed){
                checkBox.setVisibility(View.VISIBLE);

                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        faceChecked.set(position, isChecked);
                    }
                });
                checkBox.setChecked(faceChecked.get(position));
            }else{
                checkBox.setVisibility(View.INVISIBLE);
            }

            return convertView;
        }
    }
}
