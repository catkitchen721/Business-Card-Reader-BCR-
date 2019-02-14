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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.RETR_CCOMP;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.minAreaRect;

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
    int threshold_value = 100;
    boolean nameSuccess = false;
    boolean phoneSuccess = false;
    boolean emailSuccess = false;
    boolean recognitionSuccess = false;

    ImageView displayImage;
    TextView runOCR;
    TextView displayText;
    TextView displayEmail;
    TextView displayPhone;
    TextView displayName;
    TextView openContacts;
    TextView takePhoto;
    SeekBar thresValue;

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

        thresValue = (SeekBar) findViewById(R.id.thresValue);

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

        //Set SeekBar...
        thresValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                displayText.setText("Thres:" + progress + "  / 255 ");
                threshold_value = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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
            Mat thsMat = new Mat();
            Mat x05Mat = new Mat();
            Mat x025Mat = new Mat();
            Mat brightMat = new Mat();
            Bitmap srcBitmap = BitmapFactory.decodeFile(new File(picturepath, String.valueOf(timeSeed) + ".jpg").toString());
            Bitmap x025Bitmap = Bitmap.createBitmap(srcBitmap.getWidth()/4, srcBitmap.getHeight()/4, Bitmap.Config.ARGB_8888);
            Utils.bitmapToMat(srcBitmap, rgbMat);//convert original bitmap to Mat, R G B.

            /* Tempararyly remove bright function.
            rgbMat.convertTo(brightMat, -1, 1, 0);
            */

            /* Tempararyly remove threshold function.
            Imgproc.threshold(grayMat, thsMat, threshold_value, 255, Imgproc.THRESH_TOZERO);
            */

            detectText(rgbMat);  // Tag All Text Regions.

            Imgproc.pyrDown(rgbMat, x05Mat, new Size(rgbMat.cols()*0.5, rgbMat.rows()*0.5));
            Imgproc.pyrDown(x05Mat, x025Mat, new Size(x05Mat.cols()*0.5, x05Mat.rows()*0.5));
            Log.d("矩陣大小", rgbMat.toString() + x025Mat.toString());
            Utils.matToBitmap(x025Mat, x025Bitmap); //convert mat to bitmap

            displayImage.setImageBitmap(x025Bitmap);
            image = x025Bitmap;

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

    // CV Function Region
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

    private Mat preprocessText(Mat grayMat) {
        // Sobel, find border
        Mat sbl = new Mat();
        Imgproc.Sobel(grayMat, sbl, CV_8U, 1, 0, 3, 1, 0);

        // Binary
        Mat bi = new Mat();
        Imgproc.threshold(sbl, bi, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);

        Mat element1 = Imgproc.getStructuringElement(MORPH_RECT, new Size(30, 9));
        Mat element2 = Imgproc.getStructuringElement(MORPH_RECT, new Size(24, 4));

        // 1st dilate
        Mat dlt1 = new Mat();
        Imgproc.dilate(bi, dlt1, element2);

        // 1st erode
        Mat erd1 = new Mat();
        Imgproc.erode(dlt1, erd1, element1);

        // 2nd dilate
        Mat dlt2 = new Mat();
        Imgproc.dilate(erd1, dlt2, element2);

        return  dlt2;
    }

    public void detectText(Mat rgbMat) {
        Mat grayMat = new Mat();
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat

        Mat dial = preprocessText(grayMat);

        Vector<RotatedRect> rects = findTextRegion(dial);

        for(int i=0;i<rects.size();i++)
        {
            Point P[] = new Point[4];
            rects.get(i).points(P);
            for(int j=0;j<4;j++)
            {
                Imgproc.line(rgbMat, P[j], P[(j + 1) % 4], new Scalar(0,255,0), 2);
            }
        }
    }

    private Vector<RotatedRect> findTextRegion(Mat ppedMat) {
        Vector<RotatedRect> rects = new Vector<>();

        // find contours
        Vector<MatOfPoint> contours = new Vector<>();
        MatOfPoint2f contours2f = new MatOfPoint2f();
        MatOfInt4 hry = new MatOfInt4();
        Imgproc.findContours(ppedMat, contours, hry, RETR_CCOMP, CHAIN_APPROX_SIMPLE, new Point(0, 0));

        // find small area
        for(int i=0;i<contours.size();i++)
        {
            double area = Imgproc.contourArea(contours.get(i));
            if(area < 1000) continue;

            contours.get(i).convertTo(contours2f, CvType.CV_32FC2);
            double epsilon = 0.001 * Imgproc.arcLength(contours2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contours2f, approx, epsilon, true);

            RotatedRect rect = Imgproc.minAreaRect(contours2f);

            int m_width = rect.boundingRect().width;
            int m_height = rect.boundingRect().height;

            if (m_height > m_width * 1.2) continue;

            rects.add(rect);
        }

        return  rects;
    }
    // CV Function Region

    public void processImage(){
        String OCRresult = null;
        mTess.setImage(image);

        OCRresult = mTess.getUTF8Text();
        //displayText.setText(OCRresult);
        extractName(OCRresult);
        extractEmail(OCRresult);
        extractPhone(OCRresult);
        System.out.println(OCRresult);
    }

    public void extractName(String str){
        System.out.println("Getting the Name");
        final String NAME_REGEX = "^([A-Z]([a-z]*|\\.) *){1,2}([A-Z][a-z]+-?)+$";
        Pattern p = Pattern.compile(NAME_REGEX, Pattern.MULTILINE);
        Matcher m =  p.matcher(str);
        if(m.find()){
            System.out.println(m.group());
            displayName.setText(m.group());
            nameSuccess = true;
        }
        else{
            displayName.setText("None Name");
            nameSuccess = false;
            recognitionSuccess = false;
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
            emailSuccess = true;
            if(nameSuccess)
            {
                recognitionSuccess = true;
            }
        }
        else{
            displayEmail.setText("None Email");
            emailSuccess = false;
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
            phoneSuccess = true;
            if(nameSuccess)
            {
                recognitionSuccess = true;
            }
        }
        else{
            displayPhone.setText("None Phone");
            phoneSuccess = false;
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
        if(recognitionSuccess){
            //Adds the name...
            intent.putExtra(ContactsContract.Intents.Insert.NAME, displayName.getText());

            //Adds the email...
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, displayEmail.getText());
            //Adds the email as Work Email
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);

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