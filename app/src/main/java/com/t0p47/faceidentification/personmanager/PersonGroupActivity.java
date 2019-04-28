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
import com.t0p47.faceidentification.helper.IdentificationApp;
import com.t0p47.faceidentification.helper.StorageHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        if(!personGroupExists){
            Log.d("LOG_TAG","PersonGroupActivity: addPersonGroupTask");
            new AddPersonGroupTask(true).execute("0");
        }else{
            Log.d("LOG_TAG","PersonGroupActivity: simply addPerson");
            addPerson();
        }
    }

    private void addPerson(){

        Intent intent = new Intent(this, PersonActivity.class);
        intent.putExtra("AddNewPerson", true);
        intent.putExtra("PersonName","");
        intent.putExtra("PersonGroupId", "0");
        startActivity(intent);

    }

    boolean addNewPersonGroup;
    boolean personGroupExists;
    String personGroupId;
    String oldPersonGroupName;

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

        EditText editTextPersonGroupName = (EditText)findViewById(R.id.edit_person_group_name);
        editTextPersonGroupName.setText(oldPersonGroupName);
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
                    String personName = StorageHelper.getPersonName(personId, PersonGroupActivity.this);

                    Intent intent = new Intent(PersonGroupActivity.this, PersonActivity.class);
                    intent.putExtra("AddNewPerson", false);
                    intent.putExtra("PersonName", personName);
                    intent.putExtra("PersonId", personId);
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
        if (!personGroupExists) {
            new AddPersonGroupTask(false).execute(personGroupId);
        } else {
            doneAndSave(true);
        }
    }

    public void doneAndSave(boolean trainPersonGroup){

        EditText editTextPersonGroupName = (EditText)findViewById(R.id.edit_person_group_name);
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
        }
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

        StorageHelper.deletePersons(personIdsToDelete, "0", this);

        personGridViewAdapter.personIdList = newPersonIdList;
        personGridViewAdapter.personChecked = newPersonChecked;
        personGridViewAdapter.notifyDataSetChanged();
    }

    private class PersonGridViewAdapter extends BaseAdapter{

        List<String> personIdList;
        List<Boolean> personChecked;
        boolean longPressed;

        PersonGridViewAdapter(){
            longPressed = false;
            personIdList = new ArrayList<>();
            personChecked = new ArrayList<>();

            Set<String> personIdSet = StorageHelper.getAllPersonIds("0",PersonGroupActivity.this);
            for (String personId: personIdSet){
                personIdList.add(personId);
                personChecked.add(false);
            }
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

            if(convertView == null){
                LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_person, parent, false);
            }
            convertView.setId(position);

            String personId = personIdList.get(position);
            Set<String> faceIdSet = StorageHelper.getAllFaceIds(personId, PersonGroupActivity.this);

            if(!faceIdSet.isEmpty()){
                Iterator<String> it = faceIdSet.iterator();
                Uri uri = Uri.parse(StorageHelper.getFaceUri(it.next(), PersonGroupActivity.this));
                ((ImageView)convertView.findViewById(R.id.image_person)).setImageURI(uri);
            }else{
                Drawable drawable = getResources().getDrawable(R.drawable.select_image);
                ((ImageView)convertView.findViewById(R.id.image_person)).setImageDrawable(drawable);
            }

            //Set the text of the item
            String personName = StorageHelper.getPersonName(personId, PersonGroupActivity.this);
            ((TextView)convertView.findViewById(R.id.text_person)).setText(personName);

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
