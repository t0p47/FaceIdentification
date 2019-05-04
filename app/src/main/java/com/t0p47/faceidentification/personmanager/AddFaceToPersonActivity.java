package com.t0p47.faceidentification.personmanager;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.t0p47.faceidentification.R;
import com.t0p47.faceidentification.db.AppDatabase;
import com.t0p47.faceidentification.helper.IdentificationApp;
import com.t0p47.faceidentification.helper.ImageHelper;
import com.t0p47.faceidentification.helper.StorageHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.reactivex.Completable;

public class AddFaceToPersonActivity extends AppCompatActivity {

    //Background task of adding face to person
    class AddFaceTask extends AsyncTask<Void, String, Boolean>{

        List<Integer> mfaceIndices;
        AddFaceTask(List<Integer> faceIndices){
            mfaceIndices = faceIndices;
        }

        @Override
        protected Boolean doInBackground(Void... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();
            try{
                publishProgress("Adding face...");
                UUID personId = UUID.fromString(mPersonId);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

                for(Integer index: mfaceIndices){
                    FaceRectangle faceRect = mFaceGridViewAdapter.faceRectList.get(index);

                    //Start the request to add face
                    AddPersistedFaceResult result = faceServiceClient.addPersonFaceInLargePersonGroup(
                            mPersonGroupId,
                            personId,
                            imageInputStream,
                            "User data",
                            faceRect);

                    mFaceGridViewAdapter.faceIdList.set(index, result.persistedFaceId);
                }
                return true;
            }catch (Exception e){
                publishProgress(e.getMessage());

                return false;
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
        protected void onPostExecute(Boolean result){
            setUiAfterAddingFace(result, mfaceIndices);
        }
    }

    private class DetectionTask extends AsyncTask<InputStream, String, Face[]>{

        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(InputStream... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();

            try{
                publishProgress("Detecting...");

                return faceServiceClient.detect(
                        params[0],
                        true,
                        false,
                        null);
            }catch(Exception e){
                mSucceed = false;
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
        protected void onPostExecute(Face[] faces){
            if(mSucceed){
                //TODO: face detected
            }

            setUiAfterDetection(faces, mSucceed);
        }
    }

    AppDatabase db = IdentificationApp.getInstance().getDatabase();

    private void setUiBeforeBackgroundTask(){
        mProgressDialog.show();
    }

    private void setUiDuringBackgroundTask(String progress){
        mProgressDialog.setMessage(progress);
    }

    private void setUiAfterAddingFace(boolean succeed, List<Integer> faceIndices){
        mProgressDialog.dismiss();
        if(succeed){
            String faceIds = "";
            for(Integer index: faceIndices){
                String faceId = mFaceGridViewAdapter.faceIdList.get(index).toString();
                faceIds += faceId+", ";
                FileOutputStream fileOutputStream = null;
                try{
                    File file = new File(getApplicationContext().getFilesDir(), faceId);
                    fileOutputStream = new FileOutputStream(file);
                    mFaceGridViewAdapter.faceThumbnails.get(index)
                            .compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();

                    Uri uri = Uri.fromFile(file);
                    /*StorageHelper.setFaceUri(
                            faceId, uri.toString(), mPersonId, AddFaceToPersonActivity.this);*/

                    com.t0p47.faceidentification.db.entities.Face face = new com.t0p47.faceidentification.db.entities.Face();
                    face.faceId = faceId;
                    face.faceUri = uri.toString();
                    face.personId = mPersonId;

                    Completable.fromAction(() -> db.faceDao().insert(face));

                }catch(Exception e){
                    Log.d("LOG_TAG", e.getMessage());
                }finally{
                    if(fileOutputStream != null){
                        try{
                            fileOutputStream.close();
                        }catch (IOException e){
                            Log.d("LOG_TAG", e.getMessage());
                        }
                    }
                }
            }
            finish();
        }
    }

    private void setUiAfterDetection(Face[] result, boolean succeed){
        mProgressDialog.dismiss();

        if(succeed){
            if(result != null){
                Log.d("LOG_TAG", result.length + " face"
                        + (result.length != 1 ? "s" : "") + " detected");
            }else{
                Log.d("LOG_TAG", "0 face detected");
            }

            mFaceGridViewAdapter = new FaceGridViewAdapter(result);

            GridView gridView = (GridView) findViewById(R.id.gridView_faces_to_select);
            gridView.setAdapter(mFaceGridViewAdapter);
        }
    }

    String mPersonGroupId;
    String mPersonId;
    String mImageUriStr;
    Bitmap mBitmap;
    FaceGridViewAdapter mFaceGridViewAdapter;

    ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face_to_person);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            mPersonId = bundle.getString("PersonId");
            mPersonGroupId = bundle.getString("PersonGroupId");
            mImageUriStr = bundle.getString("ImageUriStr");
            Log.d("LOG_TAG", "AddFaceToPersonActivity: mImageUriStr: "+mImageUriStr);
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.progress_dialog_title));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);

        outState.putString("PersonId", mPersonId);
        outState.putString("PersonGroupId", mPersonGroupId);
        outState.putString("ImageUriStr", mImageUriStr);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);

        mPersonId = savedInstanceState.getString("PersonId");
        mPersonGroupId = savedInstanceState.getString("PersonGroupId");
        mImageUriStr = savedInstanceState.getString("ImageUriStr");
    }

    @Override
    protected void onResume(){
        super.onResume();

        Uri imageUri = Uri.parse(mImageUriStr);
        mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                imageUri, getContentResolver());
        if(mBitmap != null){
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

            new DetectionTask().execute(imageInputStream);
        }
    }

    public void doneAndSave(View view){
        if(mFaceGridViewAdapter != null){
            List<Integer> faceIndices = new ArrayList<>();

            for(int i = 0;i < mFaceGridViewAdapter.faceRectList.size(); ++i){
                if(mFaceGridViewAdapter.faceChecked.get(i)){
                    faceIndices.add(i);
                }
            }

            if(faceIndices.size() > 0){
                new AddFaceTask(faceIndices).execute();
                Log.d("LOG_TAG","AddFaceToPersonActivity: after training group with id: "+mPersonGroupId);
            }else{

                finish();
            }
        }
    }

    private class FaceGridViewAdapter extends BaseAdapter{

        List<UUID> faceIdList;
        List<FaceRectangle> faceRectList;
        List<Bitmap> faceThumbnails;
        List<Boolean> faceChecked;

        FaceGridViewAdapter(Face[] detectionResult){
            faceIdList = new ArrayList<>();
            faceRectList = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            faceChecked = new ArrayList<>();

            if(detectionResult != null){
                List<Face> faces = Arrays.asList(detectionResult);
                for(Face face: faces){
                    try{

                        //Crop face thumbnail with five main landmarks drawn from original image
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                mBitmap, face.faceRectangle));

                        faceIdList.add(null);
                        faceRectList.add(face.faceRectangle);

                        faceChecked.add(true);
                    }catch (IOException e){
                        Log.d("LOG_TAG",e.getMessage());
                    }
                }
            }
        }

        @Override
        public int getCount(){
            return faceRectList.size();
        }

        @Override
        public Object getItem(int position){
            return faceRectList.get(position);
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
                convertView =
                        layoutInflater.inflate(R.layout.item_face_with_checkbox, parent, false);
            }
            convertView.setId(position);

            ((ImageView)convertView.findViewById(R.id.image_face))
                    .setImageBitmap(faceThumbnails.get(position));

            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox_face);
            checkBox.setChecked(faceChecked.get(position));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    faceChecked.set(position, isChecked);
                }
            });

            return convertView;

        }

    }
}
