package com.example.androidocr;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import org.opencv.core.Rect;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGRA2RGB;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.RETR_CCOMP;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.minAreaRect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

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


    public static int REQUEST_CODE_SIGN_IN = 99;
    private static final String TAG = "MainActivity";
    public DriveServiceHelper mDriveServiceHelper;
    private GoogleSignInClient client;

    Mat LineMat;
    Mat dilaMat;
    Mat gMat;
    Mat sobelMat;
    Bitmap image;
    Vector<Bitmap> croppedImages;
    Vector<Rect> TextRects = new Vector<>(); //存放各個文字框
    Vector<Mat> ROIs = new Vector<>(); //用TextRects配合原圖 擷取出每個方框 存在ROIs
    Point[] pts;
    private TessBaseAPI mTess;
    String datapath = "";
    String picturepath = "";
    String trainLan = "";

    public AlertDialog loadingDialog;
    Handler threadHdlr = new Handler();

    long timeSeed = 0l;
    int threshold_value = 100;
    boolean nameSuccess = false;
    boolean phoneSuccess = false;
    boolean emailSuccess = false;
    boolean recognitionSuccess = false;

    private ImageView displayImage;
    private Button runOCR;
    private TextView displayText;
    private TextView accountName;
    private EditText displayEmail;
    private EditText displayPhone;
    private EditText displayName;
    private Button openContacts;
    private Button takePhoto;
    private Button fromGallery;
    private Button toExcel;
    private Button uploadGD;
    //    private Button signOutGD;
    //  private CheckBox isHanyu;
    // private CheckBox isCallingCode;
    // SeekBar thresValue;
    private DrawerLayout drawerLayout;
    private NavigationView navigation_view;
    private Toolbar toolbar;
    private Boolean hanyu_switchPref;
    private Boolean countrycode_switchPref;


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    private static final int REQUEST_CAMERA = 2;
    private static String[] PERMISSIONS_CAMERA = {
            "android.permission.CAMERA"};

    private static final int REQUEST_TO_CAMERA = 15;
    private static final int REQUEST_TO_GALLERY = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder(); //修正Android 7.0以上拍照系統功能
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        runOCR = (Button) findViewById(R.id.run_OCR);
        openContacts = (Button) findViewById(R.id.add_toContact);
        takePhoto = (Button) findViewById(R.id.take_photo);
        fromGallery = (Button) findViewById(R.id.open_gallery);
        toExcel = (Button) findViewById(R.id.save_toExcel);
        uploadGD = (Button) findViewById(R.id.upload_button);
        //   signOutGD = (Button) findViewById(R.id.signout_button);

        displayText = (TextView) findViewById(R.id.display_the_result);
        displayName = (EditText) findViewById(R.id.result_name);
        displayPhone = (EditText) findViewById(R.id.result_phone);
        displayEmail = (EditText) findViewById(R.id.result_email);
        displayImage = (ImageView) findViewById(R.id.imageView);
        //  isHanyu = (CheckBox) findViewById(R.id.isHanyu);
        //  isCallingCode = (CheckBox) findViewById(R.id.isCallingCode);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        navigation_view = (NavigationView) findViewById(R.id.navigation_view);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        accountName = navigation_view.getHeaderView(0).findViewById(R.id.email_name);
        LineMat = new Mat();
        dilaMat = new Mat();
        gMat = new Mat();
        sobelMat = new Mat();

        verifyStoragePermissions(this);

        //lock texts
        displayName.setFocusable(false);
        displayName.setEnabled(false);
        displayName.setFocusableInTouchMode(false);
        displayPhone.setFocusable(false);
        displayPhone.setEnabled(false);
        displayPhone.setFocusableInTouchMode(false);
        displayEmail.setFocusable(false);
        displayEmail.setEnabled(false);
        displayEmail.setFocusableInTouchMode(false);

        //initialize hanyu check
        // isHanyu.setChecked(true);
        // isCallingCode.setChecked(true);

        //init image
        image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image3);

        //create picture folder
        picturepath = Environment.getExternalStorageDirectory().getPath() + "/ocrPic/";
        createFolder(new File(picturepath));

        //initialize Tesseract API
        String language = "eng";
        trainLan = "tessdata/" + language + ".traineddata";
        datapath = getFilesDir() + "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        if (Build.VERSION.SDK_INT >= 21) {
            uploadGD.setBackgroundResource(R.drawable.ripple_sample);
            //      signOutGD.setBackgroundResource(R.drawable.ripple_sample);
            openContacts.setBackgroundResource(R.drawable.ripple_sample);
            toExcel.setBackgroundResource(R.drawable.ripple_sample);
            runOCR.setBackgroundResource(R.drawable.ripple_sample);
            takePhoto.setBackgroundResource(R.drawable.ripple_sample);
            fromGallery.setBackgroundResource(R.drawable.ripple_sample);
        }

        //run the OCR on the test_image...
        runOCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ROIs.size() == 0) {
                    Toast.makeText(getApplicationContext(), "You should take a picture first !", Toast.LENGTH_LONG).show();
                    return;
                }
                loadingDialog = new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Loading...")
                        .create();

                loadingDialog.setCancelable(false);
                loadingDialog.show();

                Thread thr = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        processImageCounting();

                        threadHdlr.post(new Runnable() {
                            public void run() {
                                processImageTail();
                                loadingDialog.dismiss();
                            }
                        });
                    }
                });
                thr.start();
            }
        });


        // 用toolbar做為APP的ActionBar
        setSupportActionBar(toolbar);

        // 將drawerLayout和toolbar整合，會出現「三」按鈕
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        // 為navigatin_view設置點擊事件
        navigation_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                // 點選時收起選單
                drawerLayout.closeDrawer(GravityCompat.START);

                // 取得選項id
                int id = item.getItemId();

                // 依照id判斷點了哪個項目並做相應事件
                if (id == R.id.action_logging) {
                    // 按下「登入/登出」要做的事
                    if (mDriveServiceHelper == null) {
                        requestSignIn();
                    } else {
                        client.signOut();
                        handleSignInResult(null);
                    }
                    return true;
                } else if (id == R.id.action_settings) {
                    // 按下「設定」要做的事
                    Toast.makeText(MainActivity.this, "設定", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.action_help) {
                    // 按下「使用說明」要做的事
                    Toast.makeText(MainActivity.this, "使用說明", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.action_about) {
                    // 按下「關於」要做的事
                    Toast.makeText(MainActivity.this, "關於你的歌", Toast.LENGTH_SHORT).show();
                    return true;
                }
                // 略..

                return false;
            }
        });
        // 還原設定中的Values
        android.support.v7.preference.PreferenceManager
                .setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sharedPref =
                android.support.v7.preference.PreferenceManager
                        .getDefaultSharedPreferences(this);
        hanyu_switchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_HANYU_SWITCH, false);
        countrycode_switchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_COUNTRYCODE_SWITCH, false);


        // 請求登入Google Acoount
        requestSignIn();
        // 請求系統權限
        checkPermission();
    }

    private void requestSignIn() {                          //向Google要求登入帳號後要使用的服務
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE),           //要求的服務類型
                                new Scope(DriveScopes.DRIVE_APPDATA))
                        .requestIdToken("561862270460-omke2t4cqrbcd5tq8jf1etogpug73jb3.apps.googleusercontent.com")
                        .build();
        //GoogleSignInClient client = GoogleSignIn.getClient(getActivity(), signInOptions);
        client = GoogleSignIn.getClient(this, signInOptions);
        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);     //發出SignInIntent  接著會跳出使用者是否同意授權的Dialog
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {            //確定取得Account與憑證
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());
                    Toast.makeText(getApplicationContext(), "Signed in as " + googleAccount.getEmail(), Toast.LENGTH_LONG).show();
                    navigation_view.getMenu().findItem(R.id.action_logging).setTitle("登出Google Account");
                    accountName.setText("登入狀態:" + googleAccount.getEmail());
                    //    signOutGD.setText(R.string.Signout_banner);
                    // Use the authenticated account to sign in to the Drive service.
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    Drive googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("AppName")
                                    .build();

                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.
                    mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                })
                .addOnFailureListener(exception -> {
                    navigation_view.getMenu().findItem(R.id.action_logging).setTitle(R.string.Signin_banner);
                    navigation_view.getMenu().findItem(R.id.action_logging).setTitle("登入Google Account");
                    accountName.setText("登入狀態:尚未登入");
                    //      signOutGD.setText(R.string.Signin_banner);
                    Toast.makeText(getApplicationContext(), "Signed out.", Toast.LENGTH_LONG).show();

                    mDriveServiceHelper = null;
                    Log.e("123123132132132", "Unable to sign in.", exception);
                });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_IN) {              //RequestCode
            handleSignInResult(data);
        }
        if (requestCode != REQUEST_TO_CAMERA && requestCode != REQUEST_TO_GALLERY) {
            Log.d("錯誤", "不是拍照");
            return;
        }
        if (resultCode != RESULT_OK) {
            Log.e("錯誤", "拍照失敗");
            return;
        }
        if (requestCode == REQUEST_TO_CAMERA) {
            try {
                Uri uri = Uri.fromFile(new File(picturepath, String.valueOf(timeSeed) + ".jpg"));
                Log.d("名字", String.valueOf(timeSeed));

                OpenCVLoader.initDebug();
                Mat rgbMat = new Mat();
                //Mat grayMat = new Mat();
                //Mat thsMat = new Mat();
                Mat x05Mat = new Mat();
                Mat x025Mat = new Mat();
                //Mat brightMat = new Mat();
                Bitmap srcBitmap = BitmapFactory.decodeFile(new File(picturepath, String.valueOf(timeSeed) + ".jpg").toString());
                Bitmap dstBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);
                Bitmap x025Bitmap = Bitmap.createBitmap(srcBitmap.getWidth() / 4, srcBitmap.getHeight() / 4, Bitmap.Config.ARGB_8888);
                Utils.bitmapToMat(srcBitmap, rgbMat);//convert original bitmap to Mat, R G B.

            /* Tempararyly remove bright function.
            rgbMat.convertTo(brightMat, -1, 1, 0);
            */

            /* Tempararyly remove threshold function.
            Imgproc.threshold(grayMat, thsMat, threshold_value, 255, Imgproc.THRESH_TOZERO);
            */
                Imgproc.pyrDown(rgbMat, x05Mat, new Size(rgbMat.cols() * 0.5, rgbMat.rows() * 0.5));
                Imgproc.pyrDown(x05Mat, x025Mat, new Size(x05Mat.cols() * 0.5, x05Mat.rows() * 0.5));

                ImagePers imgprs = new ImagePers(x025Mat);
                Mat afterImgprsResult = imgprs.returnImg();

                ROIs.clear();
                detectText(afterImgprsResult);  // Tag All Text Regions.

                Utils.matToBitmap(afterImgprsResult, x025Bitmap);
                image = x025Bitmap;

                int mode = 1;
                if (mode == 0) {
                    Utils.matToBitmap(afterImgprsResult, x025Bitmap);
                } else if (mode == 1) {
                    Utils.matToBitmap(LineMat, x025Bitmap);
                } else if (mode == 2) {
                    Utils.matToBitmap(dilaMat, x025Bitmap);
                } else if (mode == 3) {
                    Utils.matToBitmap(gMat, x025Bitmap);
                } else if (mode == 4) {
                    Utils.matToBitmap(sobelMat, x025Bitmap);
                }
                //Utils.matToBitmap(x025Mat, x025Bitmap); //convert mat to bitmap
                //Utils.matToBitmap(rgbMat, dstBitmap); //convert mat to bitmap

                displayImage.setImageBitmap(x025Bitmap);

                //runOCR.setEnabled(true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (requestCode == REQUEST_TO_GALLERY) {
            Log.d("拍照正常", "是");
            try {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {
                        MediaStore.Images.Media.DATA
                };
                String path;
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                path = cursor.getString(columnIndex);
                cursor.close();

                Bitmap srcBitmap = BitmapFactory.decodeFile(path);

                Bitmap dstBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);
                //    Bitmap x025Bitmap = Bitmap.createBitmap(srcBitmap.getWidth()/4, srcBitmap.getHeight()/4, Bitmap.Config.RGB_565);

                image = srcBitmap;


                Mat rgbMat = new Mat();
                Mat dstMat = new Mat();
                Mat grayMat = new Mat();
                Mat x05Mat = new Mat();
                Mat x025Mat = new Mat();


                Utils.bitmapToMat(srcBitmap, rgbMat);//convert original bitmap to Mat, R G B.

                int mode = 1;

             /* Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
                Imgproc.bilateralFilter(grayMat,dstMat,5,30,30);
                Imgproc.cvtColor(grayMat,rgbMat,Imgproc.COLOR_GRAY2RGB);*/
                if (rgbMat.cols() > 1500 && rgbMat.rows() > 1500) {
                    Imgproc.pyrDown(rgbMat, x05Mat, new Size(rgbMat.cols() * 0.5, rgbMat.rows() * 0.5));
                    Imgproc.pyrDown(x05Mat, x025Mat, new Size(x05Mat.cols() * 0.5, x05Mat.rows() * 0.5));

                    ImagePers imgprs = new ImagePers(x025Mat);
                    System.out.println("-------------5654544444444444444444444444444444444");
                    Mat afterImgprsResult = imgprs.returnImg();

                    ROIs.clear();
                    detectText(afterImgprsResult);  // Tag All Text Regions.

                    dstBitmap = Bitmap.createBitmap((int) (srcBitmap.getWidth() * 0.25), (int) (srcBitmap.getHeight() * 0.25), Bitmap.Config.ARGB_8888);

                    Utils.matToBitmap(afterImgprsResult, dstBitmap);
                    image = dstBitmap;

                    if (mode == 0) {
                        Utils.matToBitmap(afterImgprsResult, dstBitmap);
                    } else if (mode == 1) {
                        Utils.matToBitmap(LineMat, dstBitmap);
                    } else if (mode == 2) {
                        Utils.matToBitmap(dilaMat, dstBitmap);
                    } else if (mode == 3) {
                        Utils.matToBitmap(gMat, dstBitmap);
                    } else if (mode == 4) {
                        Utils.matToBitmap(sobelMat, dstBitmap);
                    }
                } else {
                    ImagePers imgprs = new ImagePers(rgbMat);
                    Mat afterImgprsResult = imgprs.returnImg();

                    ROIs.clear();
                    detectText(afterImgprsResult);  // Tag All Text Regions.

                    dstBitmap = Bitmap.createBitmap((int) (srcBitmap.getWidth()), (int) (srcBitmap.getHeight()), Bitmap.Config.ARGB_8888);

                    Utils.matToBitmap(afterImgprsResult, dstBitmap);
                    image = dstBitmap;

                    if (mode == 0) {
                        Utils.matToBitmap(afterImgprsResult, dstBitmap);
                    } else if (mode == 1) {
                        Utils.matToBitmap(LineMat, dstBitmap);
                    } else if (mode == 2) {
                        Utils.matToBitmap(dilaMat, dstBitmap);
                    } else if (mode == 3) {
                        Utils.matToBitmap(gMat, dstBitmap);
                    } else if (mode == 4) {
                        Utils.matToBitmap(sobelMat, dstBitmap);
                    }
                }

                System.out.println(ROIs.size() + "個\n");
                displayImage.setImageBitmap(dstBitmap);


                /*
                dstBitmap = Bitmap.createBitmap(ROIs.get(4).width(), ROIs.get(4).height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(ROIs.get(4), dstBitmap); //convert mat to bitmap
                System.out.println(ROIs.size());
                displayImage.setImageBitmap(dstBitmap);
                */

                //runOCR.setEnabled(true);

            } catch (Exception e) {
                e.printStackTrace();
            }
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
        try {
            //write is ok?
            int write_permission = PermissionChecker.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            //camera is ok?
            int camera_permission = PermissionChecker.checkSelfPermission(activity,
                    "android.permission.CAMERA");
            if (write_permission != PackageManager.PERMISSION_GRANTED) {
                //if no write permission, ask user
                Log.d("ask write", "ask now");
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            } else if (camera_permission != PackageManager.PERMISSION_GRANTED) {
                //if no camera permission, ask user
                Log.d("ask camera", "ask now");
                ActivityCompat.requestPermissions(activity, PERMISSIONS_CAMERA, REQUEST_CAMERA);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CV Function Region
    public Mat blur(Mat rgbMat) {
        Mat dstMat = new Mat();
        Mat aaMat = new Mat();

        Imgproc.cvtColor(rgbMat, aaMat, COLOR_RGB2GRAY);
        Imgproc.bilateralFilter(aaMat, dstMat, 300, 200, 200);
        rgbMat = dstMat.clone();
        return rgbMat;
    }

    public Mat detectText(Mat rgbMat) {
        Mat grayMat = new Mat();
        Mat grayMat_equalizeHist = new Mat();
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
 /*      Imgproc.equalizeHist(grayMat,grayMat_equalizeHist);
        Mat dial = preprocessText(grayMat_equalizeHist);*/
        gMat = grayMat.clone();
        Mat dial = preprocessText(grayMat);
        Bitmap dstBitmap;
        dstBitmap = Bitmap.createBitmap(rgbMat.width(), rgbMat.height(), Bitmap.Config.ARGB_8888);

        Vector<RotatedRect> rects = findTextRegion(dial);
        Utils.matToBitmap(dial, dstBitmap);
        //印出 dilate 後的圖
        //      displayImage.setImageBitmap(dstBitmap);
        dilaMat = dial.clone();
        Log.d("做完沒", "做完了");
        //  Vector<Bitmap> brectsBitmap = new Vector<>();

        /*for(int i=0;i<rects.size();i++)
        {
            *//*Point P[] = new Point[4];
            rects.get(i).points(P);
            for(int j=0;j<4;j++)
            {
                Imgproc.line(rgbMat, P[j], P[(j + 1) % 4], new Scalar(0,255,0), 2);
            }*//*
            Imgproc.rectangle(rgbMat, rects.get(i).boundingRect().tl(), rects.get(i).boundingRect().br(), new Scalar(0,255,0), 2);
            *//*Mat imgDesc = new Mat(rects.get(i).boundingRect().height, rects.get(i).boundingRect().width, CvType.CV_8U);
            Mat imgROI = new Mat(rgbMat, rects.get(i).boundingRect());
            imgROI.copyTo(imgDesc);
            Bitmap imgDescBitmap = Bitmap.createBitmap(imgDesc.width(), imgDesc.height(), Bitmap.Config.ARGB_8888);
            brectsBitmap.add(imgDescBitmap);*//*
        }*/

        // 在rects這個Vector<RotatedRect>中 訪問每個element並畫出邊線框

        LineMat = rgbMat.clone();
        for (RotatedRect rect : rects) {
            org.opencv.core.Point[] P = new org.opencv.core.Point[4];
            rect.points(P);
            for (int j = 0; j <= 3; j++) {
                Imgproc.line(LineMat, P[j], P[(j + 1) % 4], new Scalar(255, 0, 0, 255), 2);
            }
        }


        Log.d("畫線", "畫完了");
        //在TextRects這個Vector<Rect>中 用每一個element從原圖中擷取ROI區域 並放進ROIs這個Vector<Mat>中
        for (Rect rect : TextRects) {
            if (rect.x + rect.width > rgbMat.cols())
                continue;
            if (rect.y + rect.height > rgbMat.rows())
                continue;
            Mat roi_img = new Mat(rgbMat, rect);
            ROIs.add(roi_img);
        }
        Log.d("ROI做完沒", "做完了");

        return rgbMat;
    }

    private Mat preprocessText(Mat gray) {
        // Sobel, find border
  /*      Mat sbl = new Mat();
        Imgproc.Sobel(grayMat, sbl, CV_8U, 1, 0, 3, 1, 0);
        // Binary
        Mat bi = new Mat();
        Imgproc.threshold(sbl, bi, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
        Mat element1 = Imgproc.getStructuringElement(MORPH_RECT, new Size(45, 12));
        // 1st dilate
        Mat dlt1 = new Mat();
        Imgproc.dilate(bi, dlt1, element1);
        return  dlt1;*/
        Mat sobel = new Mat();
        Imgproc.Sobel(gray, sobel, CvType.CV_8U, 1, 0, 3, 1, 0);
        sobelMat = sobel.clone();

        //////////// See the Sobel /////////////
       /* Bitmap dstBitmap;
        dstBitmap = Bitmap.createBitmap(gray.width(), gray.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(sobel, dstBitmap);
        displayImage.setImageBitmap(dstBitmap);*/
        //////////////////////////////////////////

        Mat binary = new Mat();
        Imgproc.threshold(sobel, binary, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
        final Point anchor = new Point(-1, -1);
        final int iterations = 1;
        /*final Size kernelSize1 = new Size(30, 9);
        final Size kernelSize2 = new Size(24, 4);*/
/*
        final Size kernelSize1 = new Size(15, 7);
        final Size kernelSize2 = new Size(20, 5);
        final Size kernelSize3 = new Size(20,7);
        */
 /*       final Size kernelSize1 = new Size(20, 8);
        final Size kernelSize2 = new Size(15, 12);
        final Size kernelSize3 = new Size(10,9);*/

        /*final Size kernelSize1 = new Size(20, 8);
        final Size kernelSize2 = new Size(24, 7);
        final Size kernelSize3 = new Size(10,9);*/

        final Size kernelSize1 = new Size(20, 8);
        final Size kernelSize2 = new Size(24, 7);
        final Size kernelSize3 = new Size(10, 9);

        Mat element1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize1);
        Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize2);
        Mat element3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize3);

        Mat dilate1 = new Mat();
        Imgproc.dilate(binary, dilate1, element1);


        Mat erode1 = new Mat();
        Imgproc.erode(dilate1, erode1, element2);

        Mat dilate2 = new Mat();
        Imgproc.dilate(erode1, dilate2, element3, anchor, iterations);


        return dilate2;
    }

    private Vector<RotatedRect> findTextRegion(Mat img) {
    /*    Vector<RotatedRect> rects = new Vector<>();
        // find contours
        Vector<MatOfPoint> contours = new Vector<>();
        MatOfPoint2f contours2f = new MatOfPoint2f();
        MatOfInt4 hry = new MatOfInt4();
        Imgproc.findContours(ppedMat, contours, hry, RETR_CCOMP, CHAIN_APPROX_SIMPLE, new Point(0, 0));
        // find small area
        for(int i=0;i<contours.size();i++)
        {
            double area = Imgproc.contourArea(contours.get(i));
            if(area < 15000) continue;  //temp 15000
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
        return  rects;*/
        Vector<RotatedRect> rects = new Vector<>();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(img, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        //因為minAreaRect裡的parameter吃的是MatOfPoint2f
        List<org.opencv.core.MatOfPoint2f> contoursTemp = new ArrayList<>();
        for (MatOfPoint point : contours) {
            org.opencv.core.MatOfPoint2f newPoint = new org.opencv.core.MatOfPoint2f(point.toArray());
            contoursTemp.add(newPoint);
        }

        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area < 2000)
                continue;

            RotatedRect rect = Imgproc.minAreaRect(contoursTemp.get(i));

            int m_width = rect.boundingRect().width;
            int m_height = rect.boundingRect().height;

            if (m_height > m_width * 1.2 || m_width - m_height <= 1 || (Math.abs(rect.angle) >= 15.0 && Math.abs(rect.angle) <= 85.0))
                continue;

            System.out.println(rect.angle);
            rects.add(rect);


            Log.d("做完沒", "還沒");
            pts = new Point[4];
            rect.points(pts);   //提取RotatedRect中的四個頂點 放進pts[4]
            int xx = (int) rect.boundingRect().tl().x;
            int yy = (int) rect.boundingRect().tl().y;
            if (xx < 0) xx = 0;
            if (yy < 0) yy = 0;
            if (xx + m_width >= img.cols()) m_width = img.cols() - xx;
            if (yy + m_height >= img.rows()) m_height = img.rows() - yy;
            Rect TextRect = new Rect(xx, yy, m_width, m_height); //利用RotatedRect的左上頂點座標，做出Rect
            TextRects.add(TextRect); //做出的Rect 放進Vector<Rect>


        }


        return rects;
    }

    // CV Function Region

    public void processImageTail() {

        displayName.setFocusable(true);
        displayName.setEnabled(true);
        displayName.setFocusableInTouchMode(true);
        displayName.requestFocus();
        displayPhone.setFocusable(true);
        displayPhone.setEnabled(true);
        displayPhone.setFocusableInTouchMode(true);
        displayPhone.requestFocus();
        displayEmail.setFocusable(true);
        displayEmail.setEnabled(true);
        displayEmail.setFocusableInTouchMode(true);
        displayEmail.requestFocus();
/*
        //   抓出單一個ROI來辨識
        Bitmap dstBitmap = Bitmap.createBitmap(ROIs.get(2).width(), ROIs.get(2).height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(ROIs.get(2), dstBitmap);
        mTess.setImage(dstBitmap);
        OCRresult = mTess.getUTF8Text();
        displayText.setText(OCRresult);
        System.out.println(OCRresult);
                */

 /*
        // 掃全圖
        mTess.setImage(image);
        OCRresult = mTess.getUTF8Text();
        displayText.setText(OCRresult);
        System.out.println(OCRresult);
        */
        //ROIs.clear();
        //runOCR.setEnabled(false);
    }

    public void processImageCounting() {
        /*
        String OCRresult = null;
        mTess.setImage(image);
        OCRresult = mTess.getUTF8Text();
        //displayText.setText(OCRresult);
        extractName(OCRresult);
        extractEmail(OCRresult);
        extractPhone(OCRresult);
        System.out.println(OCRresult);
*/
        displayText.setText("");
        String OCRresult = "";
        String AllResult = "";

        //抓出每個ROI來辨識
        for (Mat rgbMat : ROIs) {
            Bitmap dstBitmap = Bitmap.createBitmap(rgbMat.width(), rgbMat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgbMat, dstBitmap);
            mTess.setImage(dstBitmap);
            OCRresult = mTess.getUTF8Text();
            //      displayImage.setImageBitmap(dstBitmap);
            System.out.println(OCRresult);
            AllResult = OCRresult + "\n" + AllResult;
        }
        //displayText.append(AllResult);
        extractName(AllResult);
        extractEmail(AllResult);
        extractPhone(AllResult);
    }

    public void extractName(String str) {
        System.out.println("Getting the Name");
        String NAME_REGEX = "^([A-Z]([a-z]*|\\.) *){1,2}([A-Z][a-z]+-?)+($|([,].+$))";
        if (hanyu_switchPref) {
            NAME_REGEX = "^([A-Z]([a-z]){1,4}([\\s-][A-Z]([a-z]){1,4})|[A-Z]([a-z]){1,4})\\s([A-Z]([a-z]){1,4}){1,3}$";
        } else {
            NAME_REGEX = "^([A-Z]([a-z]*|\\.) *){1,2}([A-Z][a-z]+-?)+($|([,].+$))";
        }
        Pattern p = Pattern.compile(NAME_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);
        if (m.find()) {
            System.out.println(m.group());
            displayName.setText(m.group());
            nameSuccess = true;
        } else {
            displayName.setText("None Name");
            nameSuccess = false;
            recognitionSuccess = false;
        }
    }

    public void extractEmail(String str) {
        System.out.println("Getting the email");
        //final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        // final String EMAIL_REGEX = "^[_a-z0-9-]+([.][_a-z0-9-]+)*@([a-z0-9-._]+)*$";
        final String EMAIL_REGEX = "^.*[@\\xc2\\xae].*$";
        Pattern p = Pattern.compile(EMAIL_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if (m.find()) {
            System.out.println(m.group());
            displayEmail.setText(m.group());
            emailSuccess = true;
            if (nameSuccess) {
                recognitionSuccess = true;
            }
        } else {
            displayEmail.setText("None Email");
            emailSuccess = false;
        }
    }

    public void extractPhone(String str) {
        System.out.println("Getting Phone Number");
        //final String PHONE_REGEX="(?:^|\\D)(\\d{3})[)\\-. ]*?(\\d{3})[\\-. ]*?(\\d{4})(?:$|\\D)";
        String PHONE_REGEX = "\\(?\\d{2,3}\\)?[\\s\\-\\x12\\x94\\x80]?\\d{3,4}[\\s\\-\\x12\\x94\\x80]?\\d{4}";

        if (countrycode_switchPref) {
            PHONE_REGEX = "(\\+)?(1-)?\\(?(\\+)?\\d{2,3}\\)?[\\s\\-\\x12\\x94\\x80]?\\d{1,2}\\)?[\\s\\-\\x12\\x94\\x80]\\d{3,4}[\\s\\-\\x12\\x94\\x80]?\\d{4}";
        } else {
            PHONE_REGEX = "\\(?\\d{2,3}\\)?[\\s\\-\\x12\\x94\\x80]?\\d{3,4}[\\s\\-\\x12\\x94\\x80]?\\d{4}";
        }

        Pattern p = Pattern.compile(PHONE_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if (m.find()) {
            System.out.println(m.group());
            displayPhone.setText(m.group());
            phoneSuccess = true;
            if (nameSuccess) {
                recognitionSuccess = true;
            }
        } else {
            displayPhone.setText("None Phone");
            phoneSuccess = false;
        }
    }

    public void createFolder(File dir) {
        //directory does not exist, we create it
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if (dir.exists()) {
            String datafilepath = datapath + "/" + trainLan;
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/" + trainLan;

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open(trainLan);
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

    public void addToContacts(View view) {

        // Creates a new Intent to insert a contact
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        // Sets the MIME type to match the Contacts Provider
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        //Checks if we have the name, email and phone number...
        if (!(displayName.getText().toString().equals("") || displayName.getText().toString().equals("None Name"))) {
            //Adds the name...
            intent.putExtra(ContactsContract.Intents.Insert.NAME, displayName.getText().toString());

            //Adds the email...
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, displayEmail.getText().toString());
            //Adds the email as Work Email
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);

            //Adds the phone number...
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, displayPhone.getText().toString());
            //Adds the phone number as Work Phone
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);

            //starting the activity...
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(), "No information to add to contacts!", Toast.LENGTH_LONG).show();
        }
    }

    public void openCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        timeSeed = System.currentTimeMillis();
        Log.d("名字", String.valueOf(timeSeed));
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(new File(picturepath, String.valueOf(timeSeed) + ".jpg")));
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                return;
            } else {
                startActivityForResult(intent, REQUEST_TO_CAMERA);
            }
        } else {
            startActivityForResult(intent, REQUEST_TO_CAMERA);
        }
    }

    public void openGallery(View view) {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                Log.d("權限問題", "是");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
                Log.d("權限問題", "是");
                return;
            } else {
                Log.d("權限問題", "否");
                startActivityForResult(intent, REQUEST_TO_GALLERY);
            }
        } else {
            Log.d("權限問題", "否");
            startActivityForResult(intent, REQUEST_TO_GALLERY);
        }
    }

    public void toExcel(View view) {
        if (!(displayName.getText().toString().equals("") || displayName.getText().toString().equals("None Name"))) {
            ExcelExport ee = new ExcelExport(picturepath + "BCRInfoOutput.xls", displayName.getText().toString(), displayEmail.getText().toString(), displayPhone.getText().toString());
            Toast.makeText(getApplicationContext(), "Adding Information to excel files was successful!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "No proper information to add to excel file!", Toast.LENGTH_LONG).show();
        }
    }

    public void checkPermission() {


        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }

    }

    public void createFile(View view) {                 //上傳檔案到GoogleDrive
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Create a file.");


            mDriveServiceHelper.createFile()
                    .addOnSuccessListener(fileId -> System.out.println(fileId))
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't create file.", exception));
            Toast.makeText(this, " Upload excel files was successful!", Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(this, " Upload excel files was failed!", Toast.LENGTH_LONG).show();
        }

    }

    public void signOut(View view) {
        if (mDriveServiceHelper == null) {
            requestSignIn();
        } else {
            client.signOut();
            handleSignInResult(null);
        }


    }
}