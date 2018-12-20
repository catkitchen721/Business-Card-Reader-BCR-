package com.example.androidocr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVInterface;
import org.opencv.osgi.OpenCVNativeLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("opencv_java3");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
        }
    };

    Bitmap image;
    private TessBaseAPI mTess;
    String datapath = "";
    String picturepath = "";

    long timeSeed = 0l;

    ImageView displayImage;
    TextView runOCR;
    TextView displayText;
    TextView displayEmail;
    TextView displayPhone;
    TextView displayName;
    TextView openContacts;
    TextView takePhoto;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    private static final int REQUEST_CAMERA = 2;
    private static String[] PERMISSIONS_CAMERA = {
            "android.permission.CAMERA" };

    private static final int REQUEST_TO_CAMERA = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        runOCR = (TextView) findViewById(R.id.textView);
        openContacts = (TextView) findViewById(R.id.textView6);
        takePhoto = (TextView) findViewById(R.id.textView7);

        displayText = (TextView) findViewById(R.id.textView2);
        displayName = (TextView) findViewById(R.id.textView5);
        displayPhone = (TextView) findViewById(R.id.textView4);
        displayEmail = (TextView) findViewById(R.id.textView3);
        displayImage = (ImageView) findViewById(R.id.imageView);

        verifyStoragePermissions(this);

        //init image
        image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image3);
        displayImage.setImageBitmap(image);

        //create picture folder
        picturepath = Environment.getExternalStorageDirectory().getPath() + "/ocrPic/";
        createFolder(new File(picturepath));

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir() + "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        //run the OCR on the test_image...
        runOCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processImage();
            }
        });

        //Add the extracted info from Business Card to the phone's contacts...
        openContacts.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                addToContacts();
            }
        });

        //Take a photo...
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_TO_CAMERA) {
            Log.d("錯誤", "不是拍照");
            return;
        }
        if (resultCode != RESULT_OK) {
            Log.e("錯誤", "拍照失敗");
            return;
        }
        try {
            Uri uri = Uri.fromFile(new File(picturepath, String.valueOf(timeSeed) + ".jpg"));
            Log.d("名字", String.valueOf(timeSeed));

            OpenCVLoader.initDebug();
            Mat rgbMat = new Mat();
            Mat grayMat = new Mat();
            Mat newGrayMat = new Mat();
            Bitmap srcBitmap = BitmapFactory.decodeFile(new File(picturepath, String.valueOf(timeSeed) + ".jpg").toString());
            Bitmap grayBitmap = Bitmap.createBitmap(1164, 655, Bitmap.Config.ARGB_8888);
            Utils.bitmapToMat(srcBitmap, rgbMat);//convert original bitmap to Mat, R G B.
            //Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
            Imgproc.pyrDown(rgbMat, grayMat, new Size(rgbMat.cols()*0.5, rgbMat.rows()*0.5));
            Imgproc.pyrDown(grayMat, newGrayMat, new Size(grayMat.cols()*0.5, grayMat.rows()*0.5));
            Log.d("矩陣大小", rgbMat.toString() + newGrayMat.toString());
            Utils.matToBitmap(newGrayMat, grayBitmap); //convert mat to bitmap

            displayImage.setImageBitmap(grayBitmap);
            //image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image3);
            image = grayBitmap;
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("CV Msg", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else {
            Log.d("CV Msg", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        //Android6.0權限解決
        try
        {
            //write is ok?
            int write_permission = PermissionChecker.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            //camera is ok?
            int camera_permission = PermissionChecker.checkSelfPermission(activity,
                    "android.permission.CAMERA");
            if (write_permission != PackageManager.PERMISSION_GRANTED)
            {
                //if no write permission, ask user
                Log.d("ask write", "ask now");
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
            else if(camera_permission != PackageManager.PERMISSION_GRANTED)
            {
                //if no camera permission, ask user
                Log.d("ask camera", "ask now");
                ActivityCompat.requestPermissions(activity, PERMISSIONS_CAMERA, REQUEST_CAMERA);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Bitmap sharpen(Bitmap src, double weight) {
        double[][] SharpConfig = new double[][] {
                { 0 , -2    , 0  },
                { -2, weight, -2 },
                { 0 , -2    , 0  }
        };
        ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
        convMatrix.applyConfig(SharpConfig);
        convMatrix.Factor = weight - 8;
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
    }

    public void processImage(){
        String OCRresult = null;
        mTess.setImage(image);

        OCRresult = mTess.getUTF8Text();
        //displayText.setText(OCRresult);
        extractName(OCRresult);
        extractEmail(OCRresult);
        extractPhone(OCRresult);
    }

    public void extractName(String str){
        System.out.println("Getting the Name");
        final String NAME_REGEX = "^([A-Z]([a-z]*|\\.) *){1,2}([A-Z][a-z]+-?)+$";
        Pattern p = Pattern.compile(NAME_REGEX, Pattern.MULTILINE);
        Matcher m =  p.matcher(str);
        if(m.find()){
            System.out.println(m.group());
            displayName.setText(m.group());
        }
        else{
            displayName.setText("");
        }
    }

    public void extractEmail(String str) {
        System.out.println("Getting the email");
        final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        Pattern p = Pattern.compile(EMAIL_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if(m.find()){
            System.out.println(m.group());
            displayEmail.setText(m.group());
        }
        else{
            displayEmail.setText("");
        }
    }

    public void extractPhone(String str){
        System.out.println("Getting Phone Number");
        final String PHONE_REGEX="(?:^|\\D)(\\d{3})[)\\-. ]*?(\\d{3})[\\-. ]*?(\\d{4})(?:$|\\D)";
        Pattern p = Pattern.compile(PHONE_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if(m.find()){
            System.out.println(m.group());
            displayPhone.setText(m.group());
        }
        else{
            displayPhone.setText("");
        }
    }

    public void createFolder(File dir) {
        //directory does not exist, we create it
        if (!dir.exists()){
            dir.mkdirs();
        }
    }

    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()){
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/eng.traineddata";

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToContacts() {

        // Creates a new Intent to insert a contact
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
         // Sets the MIME type to match the Contacts Provider
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        //Checks if we have the name, email and phone number...
        if(displayName.getText().length() > 0 && ( displayPhone.getText().length() > 0 || displayEmail.getText().length() > 0 )){
            //Adds the name...
            intent.putExtra(ContactsContract.Intents.Insert.NAME, displayName.getText());

            //Adds the email...
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, displayEmail.getText());
            //Adds the email as Work Email
            intent .putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);

            //Adds the phone number...
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, displayPhone.getText());
            //Adds the phone number as Work Phone
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);

            //starting the activity...
            startActivity(intent);
        }else{
            Toast.makeText(getApplicationContext(), "No information to add to contacts!", Toast.LENGTH_LONG).show();
        }
    }

    public void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        timeSeed = System.currentTimeMillis();
        Log.d("名字", String.valueOf(timeSeed));
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(new File(picturepath, String.valueOf(timeSeed) + ".jpg")));
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                return;
            }else{
                startActivityForResult(intent, REQUEST_TO_CAMERA);
            }
        } else {
            startActivityForResult(intent, REQUEST_TO_CAMERA);
        }
    }
}