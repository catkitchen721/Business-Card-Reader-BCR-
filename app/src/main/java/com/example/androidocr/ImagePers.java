package com.example.androidocr;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * Image Perspective
 */

public class ImagePers {

    private double cannyThr = 300;
    private double cannyFactor = 1.0001;
    private Mat org = null;
    private Mat grayMat = null;
    private Mat blurMat = null;
    private Mat binMat = null;
    private Mat cannyMat = null;
    private Mat resultMat = null;
    private Mat lineMat = null;

    public ImagePers(Mat org)
    {
        this.org = org;
        this.grayMat = org.clone();
        this.blurMat = org.clone();
        this.binMat = org.clone();
        this.cannyMat = org.clone();
        this.resultMat = org.clone();
        this.lineMat = org.clone();
    }

    public Mat returnImg()
    {
        Imgproc.cvtColor(this.org, this.grayMat, Imgproc.COLOR_RGB2GRAY);  //灰度化
        Imgproc.medianBlur(this.grayMat, this.blurMat, 7);  //去除雜訊
        Imgproc.adaptiveThreshold(this.blurMat, this.binMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 41, 0);  //區域特化型二值化
        Imgproc.Canny(this.binMat, this.cannyMat, cannyThr, cannyThr * cannyFactor);  //邊緣檢測

        // 找輪廓
        Vector<MatOfPoint> contours = new Vector<>();
        MatOfPoint2f contours2f = new MatOfPoint2f();
        Mat hry = new Mat();
        Imgproc.findContours(this.cannyMat, contours, hry, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        // 畫輪廓
        this.lineMat = Mat.zeros(this.cannyMat.rows(), this.cannyMat.cols(), CvType.CV_8UC3);

        for(int i=0; i<contours.size(); i++)
        {
            Imgproc.drawContours(this.lineMat, contours, i, new Scalar((new Random()).nextInt(256), (new Random()).nextInt(256), (new Random()).nextInt(256)), 1, 8, hry, 2147483647, new Point());
        }

        Vector<MatOfPoint2f> polyContours2f = new Vector<>(contours.size());
        Vector<MatOfPoint> polyContours = new Vector<>(contours.size());
        MatOfPoint2f temp_polyContours2f = new MatOfPoint2f();
        MatOfPoint temp_polyContours = new MatOfPoint();

        Log.d("polysize", String.valueOf(polyContours.size()));

        int maxArea = 0;

        for(int i=0; i<contours.size(); i++)
        {
            if (Imgproc.contourArea(contours.get(i)) > Imgproc.contourArea(contours.get(maxArea))) {
                maxArea = i;
            }
            Log.d("area: ", String.valueOf(Imgproc.contourArea(contours.get(maxArea))));
        }

        for(int i=0; i<contours.size(); i++)
        {
            contours.get(i).convertTo(contours2f, CvType.CV_32FC2);
            //Log.d("polyinfo", String.valueOf(contours.get(i).dump()));
            double arclen = Imgproc.arcLength(contours2f, true);
            Imgproc.approxPolyDP(contours2f, temp_polyContours2f, 0.01*arclen, false);
            //Log.d("polyinfo", String.valueOf(temp_polyContours2f.dump()));
            polyContours2f.add(temp_polyContours2f);
            temp_polyContours2f = new MatOfPoint2f();
        }

        Mat polyMat = Mat.zeros(org.size(), CvType.CV_8UC3);
        for(int i=0; i<contours.size(); i++)
        {
            polyContours2f.get(i).convertTo(temp_polyContours, CvType.CV_32S);
            polyContours.add(temp_polyContours);
            temp_polyContours = new MatOfPoint();
            //Log.d("polyinfo", String.valueOf(polyContours.get(i).dump()));
        }
        Imgproc.drawContours(polyMat, polyContours, maxArea, new Scalar((new Random()).nextInt(256), (new Random()).nextInt(256), (new Random()).nextInt(256)), 2);

        return binMat;
    }
}
