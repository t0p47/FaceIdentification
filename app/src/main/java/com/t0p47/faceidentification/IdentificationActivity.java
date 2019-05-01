package com.t0p47.faceidentification;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.t0p47.faceidentification.helper.IdentificationApp;
import com.t0p47.faceidentification.helper.ImageHelper;
import com.t0p47.faceidentification.helper.StorageHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class IdentificationActivity extends AppCompatActivity {

    private static final int REQUEST_TAKE_PHOTO = 1;

    private static final String TAG = "LOG_TAG";

    private Uri mUriPhotoTaken;

    String mPersonGroupId;

    boolean detected;

    FaceListAdapter mFaceListAdapter;

    ProgressDialog progressDialog;

    private Bitmap mBitmap;


    private class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]>{

        private boolean mSucceed = true;
        String mPersonGroupId;

        IdentificationTask(String personGroupId){
            this.mPersonGroupId = personGroupId;
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params){

            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();
            try{
                publishProgress("Получение группы сотрудников...");

                TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(
                        this.mPersonGroupId);
                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Статус подготовки фотографий сотрудников " + trainingStatus.status);
                    mSucceed = false;
                    return null;
                }

                publishProgress("Идентификация...");

                return faceServiceClient.identityInLargePersonGroup(
                        this.mPersonGroupId,
                        params,
                        1);
            }catch (Exception e){
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
        protected void onProgressUpdate(String... values){
            setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(IdentifyResult[] result){
            setUiAfterIdentification(result, mSucceed);
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identification);

        detected = false;

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progress_dialog_title));

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            mPersonGroupId = bundle.getString("PersonGroupId");
        }
    }

    public void onResume(){

        super.onResume();
    }

    private void setUiBeforeBackgroundTask(){progressDialog.show();}

    private void setUiDuringBackgroundTask(String progress){
        progressDialog.setMessage(progress);

        setInfo(progress);
    }

    private void setUiAfterIdentification(IdentifyResult[] result, boolean succeed){

        progressDialog.dismiss();

        setAllButtonsEnabledStatus(true);
        setIdentifyButtonEnableStatus(false);

        if(succeed){

            setInfo("Идентификация выполнена");

            if(result != null){

                mFaceListAdapter.setIdentificationResult(result);

                String logString = "Результат: успех.";
                for(IdentifyResult identifyResult: result){
                    logString += "Лицо " + identifyResult.faceId.toString() + " идентифицировано как "
                            + (identifyResult.candidates.size() > 0
                            ? identifyResult.candidates.get(0).personId.toString()
                            : "Неизвестное лицо")
                            + ". ";
                }

                ListView listView = (ListView) findViewById(R.id.list_identified_faces);
                listView.setAdapter(mFaceListAdapter);

            }

        }

    }

    private void setDetectButtonEnabledStatus(boolean isEnabled) {
        Button detectButton = (Button) findViewById(R.id.detect);
        detectButton.setEnabled(isEnabled);
    }

    private void setUiAfterDetection(Face[] result, boolean succeed) {
        // Detection is done, hide the progress dialog.
        progressDialog.dismiss();

        // Enable all the buttons.
        setAllButtonsEnabledStatus(true);

        // Disable button "detect" as the image has already been detected.
        setDetectButtonEnabledStatus(false);

        if (succeed) {
            // The information about the detection result.
            String detectionResult;
            if (result != null) {
                detectionResult = result.length + " лиц"
                        + (result.length != 1 ? "а" : "о") + " обнаружено";

                // Show the detected faces on original image.
                ImageView imageView = (ImageView) findViewById(R.id.image);
                imageView.setImageBitmap(ImageHelper.drawFaceRectanglesOnBitmap(
                        mBitmap, result, true));

                // Set the adapter of the ListView which contains the details of the detected faces.
                FaceListAdapter faceListAdapter = new FaceListAdapter(result);

                // Show the detailed list of detected faces.
                ListView listView = (ListView) findViewById(R.id.list_identified_faces);
                listView.setAdapter(faceListAdapter);
            } else {
                detectionResult = "Лиц на фотографии не обнаружено";
            }
            setInfo(detectionResult);
        }

        mUriPhotoTaken = null;
        mBitmap = null;
    }

    private class DetectionTaskSeparate extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();
            try {
                publishProgress("Обнаружение...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        true,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        new FaceServiceClient.FaceAttributeType[] {
                                FaceServiceClient.FaceAttributeType.Age,
                                FaceServiceClient.FaceAttributeType.Gender,
                                FaceServiceClient.FaceAttributeType.Smile,
                                FaceServiceClient.FaceAttributeType.Glasses,
                                FaceServiceClient.FaceAttributeType.FacialHair,
                                FaceServiceClient.FaceAttributeType.Emotion,
                                FaceServiceClient.FaceAttributeType.HeadPose,
                                FaceServiceClient.FaceAttributeType.Accessories,
                                FaceServiceClient.FaceAttributeType.Blur,
                                FaceServiceClient.FaceAttributeType.Exposure,
                                FaceServiceClient.FaceAttributeType.Hair,
                                FaceServiceClient.FaceAttributeType.Makeup,
                                FaceServiceClient.FaceAttributeType.Noise,
                                FaceServiceClient.FaceAttributeType.Occlusion
                        });
            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                //addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            //addLog("Request: Detecting in image " + mImageUri);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            if (mSucceed) {
                /*addLog("Response: Success. Detected " + (result == null ? 0 : result.length)
                        + " face(s) in " + mImageUri);*/
            }

            // Show the result on screen when detection is done.
            setUiAfterDetection(result, mSucceed);
        }
    }

    private class DetectionTask extends AsyncTask<InputStream, String, Face[]>{

        @Override
        protected Face[] doInBackground(InputStream... params){
            FaceServiceClient faceServiceClient = IdentificationApp.getFaceServiceClient();
            try{
                publishProgress("Обнаружение...");

                return faceServiceClient.detect(
                        params[0],
                        true,
                        false,
                        null);
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
        protected void onProgressUpdate(String... values){
            setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(Face[] result){
            progressDialog.dismiss();

            setAllButtonsEnabledStatus(true);

            if(result != null){
                mFaceListAdapter = new FaceListAdapter(result);
                ListView listView = (ListView)findViewById(R.id.list_identified_faces);
                listView.setAdapter(mFaceListAdapter);

                if(result.length == 0){
                    detected = false;
                    setInfo("Лиц не обнаружено!");
                }else{
                    detected = true;
                    setInfo("Нажмите кнопку \"Идентифицировать\" для идентификации лица на фотографии.\n"+
                            "Нажмите кнопку \"Обнаружение\" для обнаружения лица на фотографии.");
                }
            }else{
                detected = false;
            }

            refreshIdentifyButtonEnabledStatus();
        }
    }

    private void detect(Bitmap bitmap){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        setAllButtonsEnabledStatus(false);

        new DetectionTask().execute(inputStream);
    }

    // Called when the "Detect" button is clicked.
    public void detect(View view) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        new DetectionTaskSeparate().execute(inputStream);

        // Prevent button click during detecting.
        setAllButtonsEnabledStatus(false);
    }

    public void takePhoto(View view){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager()) != null){

            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try{
                File file = File.createTempFile("IMG_", ".jpg", storageDir);
                /*if(Build.VERSION.SDK_INT >= 24){
                    Context context = IdentificationActivity.this;
                    mUriPhotoTaken = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + "com.t0p47.faceidentification.provider", file);
                }else{
                    mUriPhotoTaken = Uri.fromFile(file);
                }*/
                mUriPhotoTaken = Uri.fromFile(file);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            }catch(IOException e){
                setInfo(e.getMessage());
            }
        }
    }

    public void identify(View view){
        if(detected){

            List<UUID> faceIds = new ArrayList<>();
            for(Face face: mFaceListAdapter.faces){
                faceIds.add(face.faceId);
            }

            setAllButtonsEnabledStatus(false);

            new IdentificationTask(mPersonGroupId).execute(
                    faceIds.toArray(new UUID[faceIds.size()]));
        }else{
            setInfo("Пожалуйста сделайте фотографию лица");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(requestCode){
            case REQUEST_TAKE_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri imageUri;
                    if(data == null || data.getData() == null){
                        imageUri = mUriPhotoTaken;
                    }else{
                        imageUri = data.getData();
                    }

                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            imageUri, getContentResolver());

                    if(mBitmap != null){
                        ImageView imageView = (ImageView) findViewById(R.id.image);
                        imageView.setImageBitmap(mBitmap);
                    }

                    //Clear identification result
                    FaceListAdapter faceListAdapter = new FaceListAdapter(null);
                    ListView listView = (ListView) findViewById(R.id.list_identified_faces);
                    listView.setAdapter(faceListAdapter);

                    setInfo("");

                    detect(mBitmap);
                }
                break;
            default:
                break;
        }
    }

    private void setInfo(String info){
        TextView textView =findViewById(R.id.info);
        textView.setText(info);
    }

    private void setAllButtonsEnabledStatus(boolean isEnabled){

        Button groupButton = (Button) findViewById(R.id.select_image);
        groupButton.setEnabled(isEnabled);

        Button identifyButton = (Button) findViewById(R.id.identify);
        identifyButton.setEnabled(isEnabled);
    }

    private void setIdentifyButtonEnableStatus(boolean isEnabled){
        Button button = (Button) findViewById(R.id.identify);
        button.setEnabled(isEnabled);
    }

    private void refreshIdentifyButtonEnabledStatus(){
        if(detected){
            setIdentifyButtonEnableStatus(true);
        }else{
            setIdentifyButtonEnableStatus(false);
        }
    }

    private class FaceListAdapter extends BaseAdapter{

        //Detected face
        List<Face> faces;

        List<IdentifyResult> mIdentifyResults;

        List<Bitmap> faceThumbnails;

        FaceListAdapter(Face[] detectionResults){

            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            mIdentifyResults = new ArrayList<>();

            Log.d(TAG, "IdentificationActivity: PreDetection");

            if(detectionResults != null){

                faces = Arrays.asList(detectionResults);

                Log.d(TAG, "IdentificationActivity: , faces size: "+String.valueOf(faces.size()));

                for (Face face : faces){

                    try{
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                mBitmap, face.faceRectangle
                        ));
                    }catch (IOException e){
                        setInfo(e.getMessage());
                    }
                }
            }
        }

        public void setIdentificationResult(IdentifyResult[] identifyResults) {
            mIdentifyResults = Arrays.asList(identifyResults);
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent){

            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(
                        R.layout.item_face_with_description, parent, false);
            }
            convertView.setId(position);

            ((ImageView)convertView.findViewById(R.id.face_thumbnail)).setImageBitmap(faceThumbnails.get(position));


            Log.d(TAG, "IdentificationActivity: preIdentification - "+String.valueOf(mIdentifyResults.size())
                    +", faces size: "+String.valueOf(faces.size()));

            if(mIdentifyResults.size() == faces.size()){
                Log.d(TAG, "IdentificationActivity: mIdentify.size == faces.size");
                DecimalFormat formatter = new DecimalFormat("#0.00");
                if(mIdentifyResults.get(position).candidates.size() > 0){
                    String personId =
                            mIdentifyResults.get(position).candidates.get(0).personId.toString();
                    String personName = StorageHelper.getPersonName(
                            personId, mPersonGroupId,IdentificationActivity.this
                    );

                    String identity = "Сотрудник: "+personName+"\n"
                            +"Соответствие: "+formatter.format(
                                    mIdentifyResults.get(position).candidates.get(0).confidence
                    );

                    ((TextView) convertView.findViewById(R.id.text_detected_face)).setText(
                            R.string.face_cannot_be_identified
                    );
                }
            }

            return convertView;

        }

    }

}
