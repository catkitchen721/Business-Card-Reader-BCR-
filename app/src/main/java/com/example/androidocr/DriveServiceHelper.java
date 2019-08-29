package com.example.androidocr;



import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


public class DriveServiceHelper {

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive driveService;
    private static final String TAG = "DriveServiceHelper";

    public DriveServiceHelper(Drive googleDriveService) {
        driveService = googleDriveService;
    }

    // application/vnd.ms-excel
    public Task<String> createFile() {
        return Tasks.call(mExecutor, () -> {
            java.io.File UPLOAD_FILE = new java.io.File(Environment.getExternalStorageDirectory().getPath() + "/ocrPic/BCRInfoOutput.xls");//找到excel
            File metadata = new File();                 //設定要上傳的檔案的metadata
            metadata.setName(UPLOAD_FILE.getName());
            FileContent mediaContent = new FileContent("application/vnd.ms-excel", UPLOAD_FILE);    //設定要上傳的檔案的content

            int exist = 0;
            String filename;
            String fileId = "";
            String pageToken = null;
            /*
             * 不同類型的檔案有不同的mimeType 要去找一下定義
             * */
            do {                                            //掃描整個雲端硬碟
                FileList result = driveService.files().list()
                        .setQ("mimeType='application/vnd.ms-excel'")        //設置過濾條件:mimeType='application/vnd.ms-excel',找出屬於excel類型的檔案
                        .setQ("trashed = false")                            //設置過濾條件:trashed=false,不找垃圾桶裡的檔案
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
                for (File file : result.getFiles()) {
                    filename = file.getName();
                    fileId = file.getId();
                    if (filename.equals("BCRInfoOutput.xls"))               //如果找到就退出
                        exist = 1;
                }
                if (exist == 1)
                    break;
                pageToken = result.getNextPageToken();
            } while (pageToken != null);

            if (exist == 0) {                                       //檔案不存在,新建立一個
                File googleFile = driveService.files().create(metadata, mediaContent).execute();
                Log.d(TAG, "The file doesn't exist.But I create it.");
                if (googleFile == null) {
                    throw new IOException("Null result when requesting file creation.");
                }
                return googleFile.getId();
            } else {                                                //檔案已存在,做更新
                File googleFile = driveService.files().update(fileId, metadata, mediaContent).execute();
                Log.d(TAG, "The file has existed.And I update it");
                if (googleFile == null) {
                    throw new IOException("Null result when requesting file creation.");
                }
                return googleFile.getId();
            }
        });
    }
}
