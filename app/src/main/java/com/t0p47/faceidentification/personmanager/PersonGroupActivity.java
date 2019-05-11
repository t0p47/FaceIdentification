package com.t0p47.faceidentification.personmanager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.t0p47.faceidentification.R;
import com.t0p47.faceidentification.db.AppDatabase;
import com.t0p47.faceidentification.db.dao.subsets.Name;
import com.t0p47.faceidentification.helper.IdentificationApp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class PersonGroupActivity extends AppCompatActivity {

    //Background task of adding a person group
    class AddPersonGroupTask extends AsyncTask<String, String, String>{

        boolean mAddPerson;

        AddPersonGroupTask(boolean addPerson){
            mAddPerson = addPerson;
        }

        @Override
        protected String doInBackground(String... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();
            try{
                publishProgress("Syncing with server to add person group...");

                Log.d("LOG_TAG","PersonGroupActivity(My): "+params[0]);

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

                GridView gridView = (GridView)findViewById(R.id.gridView_persons);
                personGridViewAdapter = new PersonGridViewAdapter();
                gridView.setAdapter(personGridViewAdapter);

                if(mAddPerson){
                    Log.d("LOG_TAG", "PersonGroupActivity: group added, add person");
                    addPerson();
                }else{
                    Log.d("LOG_TAG", "PersonGroupActivity: group added, doneAndSave");
                    doneAndSave(false);
                }

            }else{
                Log.d("LOG_TAG", "PersonGroupActivity: NO RESULT!");
            }

        }
    }

    class TrainPersonGroupTask extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();

            try{
                publishProgress("Training person group...");

                faceServiceClient.trainLargeFaceList(params[0]);
                return params[0];
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
        protected void onProgressUpdate(String... progress){
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(String result){
            if(result != null){
               finish();
            }
        }
    }

    class DeletePersonTask extends AsyncTask<String, String, String>{

        String mPersonGroupId;
        DeletePersonTask(String personGroupId){
            mPersonGroupId = personGroupId;
        }

        @Override
        protected String doInBackground(String... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();
            try{
                publishProgress("Deleting selected persons...");

                UUID personId = UUID.fromString(params[0]);
                faceServiceClient.deletePersonInLargePersonGroup(mPersonGroupId, personId);
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
        }

    }

    private void setUiBeforeBackgroundTask(){
        progressDialog.show();
    }

    private void setUiDuringBackgroundTask(String progress){
        progressDialog.setMessage(progress);
    }

    public void addPerson(View view){
        /*if(!personGroupExists){
            Log.d("LOG_TAG","PersonGroupActivity: addPersonGroupTask");
            new AddPersonGroupTask(true).execute("0");
        }else{
            Log.d("LOG_TAG","PersonGroupActivity: simply addPerson");
            addPerson();
        }*/
        Log.d("LOG_TAG","PersonGroupActivity: simply addPerson");
        addPerson();
    }

    private void addPerson(){

        Intent intent = new Intent(this, PersonActivity.class);
        intent.putExtra("AddNewPerson", true);
        intent.putExtra("PersonName","");
        intent.putExtra("PersonGroupId", personGroupId);
        startActivity(intent);

    }

    boolean addNewPersonGroup;
    boolean personGroupExists;
    String personGroupId;
    String oldPersonGroupName;

    private static final String TAG = "LOG_TAG";

    AppDatabase db = IdentificationApp.getInstance().getDatabase();

    PersonGridViewAdapter personGridViewAdapter;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_group);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            addNewPersonGroup = bundle.getBoolean("AddNewPersonGroup");
            oldPersonGroupName = bundle.getString("PersonGroupName");
            personGroupId = bundle.getString("PersonGroupId");
            personGroupExists = !addNewPersonGroup;
        }

        initializeGridView();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progress_dialog_title));
    }

    private void initializeGridView(){
        GridView gridView = (GridView) findViewById(R.id.gridView_persons);

        gridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        gridView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                personGridViewAdapter.personChecked.set(position, checked);

                GridView gridView = (GridView)findViewById(R.id.gridView_persons);
                gridView.setAdapter(personGridViewAdapter);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_delete_items, menu);

                personGridViewAdapter.longPressed = true;

                GridView gridView = (GridView) findViewById(R.id.gridView_persons);
                gridView.setAdapter(personGridViewAdapter);

                Button addNewItem = (Button)findViewById(R.id.add_person);
                addNewItem.setEnabled(true);

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

                personGridViewAdapter.longPressed = false;

                for(int i = 0; i < personGridViewAdapter.personChecked.size();++i){
                    personGridViewAdapter.personChecked.set(i, false);
                }

                GridView gridView = (GridView) findViewById(R.id.gridView_persons);
                gridView.setAdapter(personGridViewAdapter);

                Button addNewItem = (Button) findViewById(R.id.add_person);
                addNewItem.setEnabled(true);
            }
        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!personGridViewAdapter.longPressed){
                    String personId = personGridViewAdapter.personIdList.get(position);
                    //String personName = StorageHelper.getPersonName(personId, personGroupId,  PersonGroupActivity.this);

                    final String[] personName = new String[1];

                    db.personDao().getNameById(personId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new DisposableSingleObserver<Name>() {
                                @Override
                                public void onSuccess(Name name) {
                                    Log.d(TAG, "PersonGroupActivity: onSuccess getNameById: "+name.firstName);
                                    personName[0] = name.firstName;
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Log.d(TAG, "PersonGroupActivity: onSuccess getNameById: "+e.getMessage());
                                }
                            });

                    Intent intent = new Intent(PersonGroupActivity.this, PersonActivity.class);
                    intent.putExtra("AddNewPerson", false);
                    intent.putExtra("PersonName", personName[0]);
                    intent.putExtra("PersonId", personId);
                    intent.putExtra("PersonGroupId", personGroupId);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        GridView gridView = (GridView) findViewById(R.id.gridView_persons);
        personGridViewAdapter = new PersonGridViewAdapter();
        gridView.setAdapter(personGridViewAdapter);
    }

    public void doneAndSave(View view) {
        /*if (!personGroupExists) {
            new AddPersonGroupTask(false).execute(personGroupId);
        } else {
            doneAndSave(true);
        }*/
        doneAndSave(true);
    }

    public void doneAndSave(boolean trainPersonGroup){

        /*EditText editTextPersonGroupName = (EditText)findViewById(R.id.edit_person_group_name);
        String newPersonGroupName = editTextPersonGroupName.getText().toString();
        if (newPersonGroupName.equals("")) {
            Log.d("LOG_TAG", "PersonGroupActivity: Person group name could not be empty");
            return;
        }

        StorageHelper.setPersonGroupName(personGroupId, newPersonGroupName, PersonGroupActivity.this);

        if(trainPersonGroup){
            Log.d("LOG_TAG", "PersonGroupActivity: trainPersonGroup");
            new TrainPersonGroupTask().execute(personGroupId);
        }else{
            finish();
        }*/

        finish();
    }

    private void deleteSelectedItems(){
        List<String> newPersonIdList = new ArrayList<>();
        List<Boolean> newPersonChecked = new ArrayList<>();
        List<String> personIdsToDelete = new ArrayList<>();
        for(int i = 0; i < personGridViewAdapter.personChecked.size(); ++i){
            if(personGridViewAdapter.personChecked.get(i)){
                String personId = personGridViewAdapter.personIdList.get(i);
                personIdsToDelete.add(personId);
                new DeletePersonTask("0").execute(personId);
            }else{
                newPersonIdList.add(personGridViewAdapter.personIdList.get(i));
                newPersonChecked.add(false);
            }
        }

        //StorageHelper.deletePersons(personIdsToDelete, personGroupId, this);

        //Completable.fromAction(() -> db.personDao().deletePersons(personIdsToDelete));
        Callable<Integer> clbDeletePersons = new Callable<Integer>(){

            @Override
            public Integer call() throws Exception {
                int deleteCountRes = db.personDao().deletePersons(personIdsToDelete);
                Log.d(TAG,"PersonGroupActivity: deletePersonCountRes: "+deleteCountRes);
                return deleteCountRes;
            }
        };
        Completable.fromCallable(clbDeletePersons)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG,"PersonGroupActivity: onSubscribe DeletePersons: "+d.toString());
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG,"PersonGroupActivity: onComplete DeletePersons: ");
                        personGridViewAdapter.personIdList = newPersonIdList;
                        personGridViewAdapter.personChecked = newPersonChecked;
                        personGridViewAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG,"PersonGroupActivity: onError DeletePersons: "+e.getMessage());
                    }
                });
    }

    private class PersonGridViewAdapter extends BaseAdapter{

        List<String> personIdList;
        List<Boolean> personChecked;
        boolean longPressed;

        PersonGridViewAdapter(){
            longPressed = false;
            personIdList = new ArrayList<>();
            personChecked = new ArrayList<>();

            //Set<String> personIdSet = StorageHelper.getAllPersonIds(personGroupId,PersonGroupActivity.this);

            final List<String>[] receivedPersonIdList = new List[]{new ArrayList<>()};

            db.personDao().getAllPersonIds()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DisposableSingleObserver<List<String>>() {
                        @Override
                        public void onSuccess(List<String> strings) {
                            Log.d(TAG, "PersonGroupActivity: onSuccess getAllPersonIds: "+strings.size());
                            receivedPersonIdList[0] = strings;

                            for (String personId: receivedPersonIdList[0]){
                                personIdList.add(personId);
                                personChecked.add(false);
                            }
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG, "PersonGroupActivity: onError getAllPersonIds: "+e.getMessage());
                        }
                    });

        }

        @Override
        public int getCount(){
            return personIdList.size();
        }

        @Override
        public Object getItem(int position){
            return personIdList.get(position);
        }

        @Override
        public long getItemId(int position){
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent){

            Log.d(TAG,"PersonGroupActivity: getView: personIdListSize: "+getCount());
            if(convertView == null){
                LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_person, parent, false);
            }
            convertView.setId(position);

            String personId = personIdList.get(position);
            //Set<String> faceIdSet = StorageHelper.getAllFaceIds(personId, PersonGroupActivity.this);

            final List<String>[] receivedFaceIdList = new List[]{new ArrayList<>()};

            View finalConvertView1 = convertView;
            db.personDao().getAllFaceIds(personId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DisposableSingleObserver<List<String>>() {
                        @Override
                        public void onSuccess(List<String> strings) {
                            Log.d(TAG,"PersonGroupActivity: onSuccess getAllFaceIds: "+strings.size());
                            receivedFaceIdList[0] = strings;

                            if(!receivedFaceIdList[0].isEmpty()){
                                Iterator<String> it = receivedFaceIdList[0].iterator();
                                //Uri uri = Uri.parse(StorageHelper.getFaceUri(it.next(), PersonGroupActivity.this));
                                final Uri[] uri = new Uri[1];

                                //Log.d(TAG,"PersonGroupActivity: faceUri: "+it.next());

                                db.faceDao().getFaceUri(it.next())
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new DisposableSingleObserver<String>() {
                                            @Override
                                            public void onSuccess(String s) {
                                                Log.d(TAG, "PersonGroupActivity: onSuccess getFaceUri: "+s);
                                                uri[0] = Uri.parse(s);
                                                ((ImageView) finalConvertView1.findViewById(R.id.image_person)).setImageURI(uri[0]);
                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                                Log.d(TAG, "PersonGroupActivity: onError getFaceUri: "+e.getMessage());
                                            }
                                        });



                            }else{
                                Drawable drawable = getResources().getDrawable(R.drawable.select_image);
                                ((ImageView) finalConvertView1.findViewById(R.id.image_person)).setImageDrawable(drawable);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG,"PersonGroupActivity: onError getAllFaceIds: "+e.getMessage());
                        }
                    });



            //Set the text of the item
            //String personName = StorageHelper.getPersonName(personId, personGroupId,PersonGroupActivity.this);
            final String[] personName = new String[1];

            View finalConvertView = convertView;
            db.personDao().getNameById(personId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DisposableSingleObserver<Name>() {
                        @Override
                        public void onSuccess(Name name) {
                            Log.d(TAG,"PersonGroupActivity: onSuccess getNameById: "+name.firstName);
                            personName[0] = name.firstName;

                            Log.d("LOG_TAG", "PersonGroupActivity: get person name from storage by id: "+personId
                                    +", groupId: "+personGroupId+", name: "+ personName[0]);
                            ((TextView) finalConvertView.findViewById(R.id.text_person)).setText(personName[0]);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG,"PersonGroupActivity: onError getNameById: "+e.getMessage());
                        }
                    });



            //Set the checked status of the item
            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox_person);
            if(longPressed){
                checkBox.setVisibility(View.VISIBLE);

                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        personChecked.set(position, isChecked);
                    }
                });
                checkBox.setChecked(personChecked.get(position));
            }else{
                checkBox.setVisibility(View.INVISIBLE);
            }

            return convertView;
        }

    }
}
