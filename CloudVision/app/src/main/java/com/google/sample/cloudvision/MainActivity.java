/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.cloudvision;

import android.Manifest;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyBzQHz70YSm4m_gPLnsPt1vsDT1U36HJuw";
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    //private TextView mImageDetails;
    private ImageView mMainImage;
    protected static final int RESULT_SPEECH = 1;
    private AudioManager audioManager;
    String inp="";
    TextToSpeech t1;
    View view1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });


        //mImageDetails = (TextView) findViewById(R.id.image_details);
        mMainImage = (ImageView) findViewById(R.id.main_image);
    }

    public void startCameraButton(View v)
    {
        startCamera();
    }
    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    //editText.setText(text.get(0));
                    inp=text.get(0);
                    doSearch(view1);

                    break;
                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;

        }
    }

    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                1200);

                callCloudVision(bitmap);
                mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                //Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            //Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Switch text to loading
        //mImageDetails.setText(R.string.loading_message);
        t1.speak("Uploading image. Please wait.", TextToSpeech.QUEUE_FLUSH, null);

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(10);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return "Unable to Process your Request...Please check your Internet connection";
            }

            protected void onPostExecute(String result) {
                //mImageDetails.setText(result);
                t1.speak(result+"", TextToSpeech.QUEUE_FLUSH, null);
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message = "I found these things:\n\n";

        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        int i=2;

        if (labels != null) {
            if(labels.size()<2)
            {
                i=1;
            }
            for (EntityAnnotation label : labels) {
                message += String.format(Locale.US, " %s", label.getDescription());
                message += "\n";
                if(i-- ==1)
                {
                    break;
                }
            }
        } else {
            message += "nothing";
        }

        return message;
    }

    public void startVoiceControl(View view) {
        Toast.makeText(this, "Under Development...Please wait for Next Update", Toast.LENGTH_SHORT).show();
        t1.speak("Under Development...Please wait for Next Update", TextToSpeech.QUEUE_FLUSH, null);
    }



    public void doSearch(View view) {

        String term = inp;
        StringTokenizer st = new StringTokenizer(inp);
        boolean flag = st.hasMoreElements();
        if(term.length()<=4){
            t1.speak("Searching in Google", TextToSpeech.QUEUE_FLUSH, null);
            search(term);
        }
        else if (term.substring(0,4).equalsIgnoreCase("Open")) {

            //String abc = st.nextElement().toString();
            String abc=term.substring(5).toLowerCase();
            //abc=abc.replaceAll("\\s+","").toLowerCase();
            //Toast.makeText(getBaseContext(),abc,Toast.LENGTH_LONG).show();


            if (abc != null) {
                String toSpeak="Opening "+abc;
                //Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                openAnother(abc);
            } else
                Toast.makeText(getBaseContext(), "Please Enter App name", Toast.LENGTH_LONG).show();
        }
        else if(term.substring(0,4).equalsIgnoreCase("call")){
            Intent callIntent = new Intent(Intent.ACTION_CALL);

            String abc=term.substring(5).toLowerCase();
            String number=getNumber(abc);
            if(number!=null) {
                //Toast.makeText(getBaseContext(), number, Toast.LENGTH_LONG).show();
                callIntent.setData(Uri.parse("tel:" + number));

                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String toSpeak="Calling "+abc ;
                //Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException ie){

                }
                startActivity(callIntent);
            }
            else{
                Toast.makeText(getBaseContext(),"Number Not Found",Toast.LENGTH_LONG).show();
            }
        }
        else if(term.length()>5 &&term.substring(0,5).equalsIgnoreCase("Phone")){
            String mode=term.substring(6).toLowerCase();
            if(mode.equalsIgnoreCase("Silent mode")){
                audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                //Toast.makeText(getBaseContext(),"Phone Mode set to Silent ",Toast.LENGTH_LONG).show();
                t1.speak("Phone Mode set to Silent ", TextToSpeech.QUEUE_FLUSH, null);
            }
            else if(mode.equalsIgnoreCase("Normal mode")){
                audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                //Toast.makeText(getBaseContext(),"Phone Mode set to Normal ",Toast.LENGTH_LONG).show();
                t1.speak("Phone Mode set to Normal ", TextToSpeech.QUEUE_FLUSH, null);
            }
            else if(mode.equalsIgnoreCase("Vibration mode")){
                audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                //Toast.makeText(getBaseContext(),"Phone Mode set to Vibration ",Toast.LENGTH_LONG).show();
                t1.speak("Phone Mode set to Vibration ", TextToSpeech.QUEUE_FLUSH, null);
            }
            inp=" ";
        }
        else if( term.length()>5 && term.substring(0,5).equalsIgnoreCase("wi-fi")||term.substring(0,4).equalsIgnoreCase("wifi") ){
            String status="";
            if(term.substring(0,5).equalsIgnoreCase("wi-fi"))
                status=term.substring(6).toLowerCase();
            else if(term.substring(0,4).equalsIgnoreCase("wifi"))
                status=term.substring(5).toLowerCase();

            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            boolean dhoni;
            String temp=null;
            if(status!=null) {
                if (status.equalsIgnoreCase("on")){
                    dhoni = true;
                    temp="on";
                }
                else {
                    dhoni = false;
                    temp="off";
                }
                t1.speak("Switching "+ temp +"WiFi", TextToSpeech.QUEUE_FLUSH, null);
                wifiManager.setWifiEnabled(dhoni);
            }
        }
        else if(term.substring(0,4).equalsIgnoreCase("play")){
            String abc=term.substring(5);
            if(abc.equalsIgnoreCase("tic Tac toe")){
                t1.speak("Opening Tic Tac Toe", TextToSpeech.QUEUE_FLUSH, null);
                //startActivity(new Intent(this,TicTacToe.class));
            }
        }

        else if( term.length()>8 && term.substring(0,8).equalsIgnoreCase("download")){
            String abc=term.substring(9);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://search?q=<"+abc+">&c=apps"));
            startActivity(intent);
        }

        else if(term.substring(0,4).equalsIgnoreCase("find")){
            googleMaps(term.substring(5));
        }

        else if(term.length()> 24 && term.substring(0,24).equalsIgnoreCase("send whatsapp message to")){
            boolean isWhatsappInstalled = whatsappInstalledOrNot("com.whatsapp");
            if(isWhatsappInstalled) {
                String contactName = term.substring(25);
                String number = getNumber(contactName);
                if (number != null) {
                    // Toast.makeText(this, number, Toast.LENGTH_SHORT).show();
                    Uri uri = Uri.parse("smsto:" + number);
                    Intent i = new Intent(Intent.ACTION_SENDTO, uri);
                    i.setPackage("com.whatsapp");
                    t1.speak("Opening whatsapp", TextToSpeech.QUEUE_FLUSH, null);
                    i.putExtra(Intent.EXTRA_TEXT,"Hello");
                    startActivity(Intent.createChooser(i, ""));
                } else
                    Toast.makeText(this, "Couldn't find contact details", Toast.LENGTH_SHORT).show();
            }
            else{
                t1.speak("Please install whatsapp", TextToSpeech.QUEUE_FLUSH, null);
                Uri uri = Uri.parse("market://details?id=com.whatsapp");
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                Toast.makeText(this, "WhatsApp not Installed",
                        Toast.LENGTH_SHORT).show();
                startActivity(goToMarket);
            }
        }
        else if(term.length()>=7 && term.substring(0,7).equalsIgnoreCase("youtube")){
            String abc=term.substring(8);
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage("com.google.android.youtube");
            intent.putExtra("query",abc);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            t1.speak("Opening youtube", TextToSpeech.QUEUE_FLUSH, null);
            startActivity(intent);
        }

        else {
            t1.speak("Searching in Google", TextToSpeech.QUEUE_FLUSH, null);
            search(term);
        }




    }
    public void googleMaps(String abc){
        // Search for restaurants nearby
        if(isLocationEnabled(getBaseContext())) {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + abc);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            t1.speak("Finding " + abc, TextToSpeech.QUEUE_FLUSH, null);
            startActivity(mapIntent);
        }
        else {
            t1.speak("Please enable GPS " , TextToSpeech.QUEUE_FLUSH, null);
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
        }

    }
    public void search(String term){
        try{
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, term);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(),"Some Error ",Toast.LENGTH_LONG).show();
        }

    }
    public void setText(View view) {
        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
        // Toast.makeText(getBaseContext(),"1.You can also open app,\"say open Google Play <name> for google apps\"\n2.Call <contactname>",Toast.LENGTH_LONG).show();

        try {
            startActivityForResult(intent, RESULT_SPEECH);
            inp="";
        } catch (ActivityNotFoundException a) {
            Toast t = Toast.makeText(getApplicationContext(),
                    "Opps! Your device doesn't support Speech to Text",
                    Toast.LENGTH_SHORT);
            t.show();
        }
    }

    public void openAnother(String appName){


        String data=getPackages(appName);
        if(data.equalsIgnoreCase("Boom"))
            Toast.makeText(getBaseContext(),"App not found",Toast.LENGTH_LONG).show();
        else {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(data);

            if (launchIntent != null) {
                startActivity(launchIntent);//null pointer check in case package name was not found
            }
        }
    }


    private String getPackages(String dhoni) {

        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = getPackageManager();
        //Toast.makeText(getBaseContext(),"*"+dhoni,Toast.LENGTH_LONG).show();
        // get a list of installed apps.
        packages = pm.getInstalledApplications(0);

        for (ApplicationInfo packageInfo : packages) {
            if (pm.getApplicationLabel(packageInfo).toString().equalsIgnoreCase(dhoni)) {
                return packageInfo.packageName;
            }
        }
        return "Boom";
    }

    public String getNumber(String nameOfContact){

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                if(nameOfContact.equalsIgnoreCase(name)) {

                    if (cur.getInt(cur.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor pCur = cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id}, null);
                        while (pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                            return phoneNo;
                            //Toast.makeText(getBaseContext(), "Name: " + name
                            //      + ", Phone No: " + phoneNo, Toast.LENGTH_SHORT).show();
                        }
                        pCur.close();
                    }
                }
            }
        }
        return null;

    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }

    public void showInfo(View view) {
        //startActivity(new Intent(this,Infor.class));
    }

    private boolean whatsappInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }
}
