package com.example.androidocr;

import android.os.Debug;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
    private Mat polyMat = null;

    public ImagePers(Mat org) {
        this.org = org;
        this.grayMat = org.clone();
        this.blurMat = org.clone();
        this.binMat = org.clone();
        this.cannyMat = org.clone();
        this.resultMat = org.clone();
        this.lineMat = org.clone();
    }

    public Mat returnImg() {
        Imgproc.cvtColor(this.org, this.grayMat, Imgproc.COLOR_RGB2GRAY);  //灰度化
        // Imgproc.medianBlur(this.grayMat, this.blurMat, 7);  //去除雜訊
        Imgproc.GaussianBlur(this.grayMat, this.blurMat, new Size(11, 11), 0, 0);
        Imgproc.adaptiveThreshold(this.blurMat, this.binMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 201, 0);  //區域特化型二值化
        Imgproc.Canny(this.binMat, this.cannyMat, 40, 50);  //邊緣檢測


        //膨脹
        final Size kernelSize1 = new Size(10, 10);
        Mat element1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize1);
        Imgproc.dilate(this.cannyMat, this.cannyMat, element1);

        // 找輪廓
        Vector<MatOfPoint> contours = new Vector<>();
        MatOfPoint2f contours2f = new MatOfPoint2f();
        Mat hry = new Mat();
        Imgproc.findContours(this.cannyMat, contours, hry, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        // 畫輪廓
        this.lineMat = Mat.zeros(this.cannyMat.rows(), this.cannyMat.cols(), CvType.CV_8UC3);

        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(this.lineMat, contours, i, new Scalar((new Random()).nextInt(256), (new Random()).nextInt(256), (new Random()).nextInt(256)), 1, 8, hry, 2147483647, new Point());
        }


        Vector<MatOfPoint2f> polyContours2f = new Vector<>(contours.size());
        Vector<MatOfPoint> polyContours = new Vector<>(contours.size());
        MatOfPoint2f temp_polyContours2f = new MatOfPoint2f();
        MatOfPoint temp_polyContours = new MatOfPoint();
        Log.d("polysize", String.valueOf(polyContours.size()));
        int maxArea = 0;
        for (int i = 0; i < contours.size(); i++) {
            if (Imgproc.contourArea(contours.get(i)) > Imgproc.contourArea(contours.get(maxArea))) {
                maxArea = i;
            }
            Log.d("area: ", String.valueOf(Imgproc.contourArea(contours.get(maxArea))));
        }
        for (int i = 0; i < contours.size(); i++) {
            contours.get(i).convertTo(contours2f, CvType.CV_32FC2);
            double arclen = Imgproc.arcLength(contours2f, true);
            Imgproc.approxPolyDP(contours2f, temp_polyContours2f, 0.1 * arclen, true);
            polyContours2f.add(temp_polyContours2f);
            temp_polyContours2f = new MatOfPoint2f();
        }
        Mat polyMat = Mat.zeros(org.size(), CvType.CV_8UC3);
        for (int i = 0; i < contours.size(); i++) {
            polyContours2f.get(i).convertTo(temp_polyContours, CvType.CV_32S);
            polyContours.add(temp_polyContours);
            temp_polyContours = new MatOfPoint();
        }
        Imgproc.drawContours(polyMat, polyContours, maxArea, new Scalar((new Random()).nextInt(256), (new Random()).nextInt(256), (new Random()).nextInt(256)), 2);
        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(polyContours.elementAt(maxArea), hull, false);

        for (int i = 0; i < hull.toArray().length; ++i) {
            Imgproc.circle(polyMat, polyContours.elementAt(maxArea).toArray()[i], 10, new Scalar((new Random()).nextInt(256), (new Random()).nextInt(256), (new Random()).nextInt(256)), 3);
        }


        Point srcPoints[] = new Point[4];
        Point dstPoints[] = new Point[4];

        dstPoints[0] = new Point(0, 0);
        dstPoints[1] = new Point(polyMat.cols(), 0);
        dstPoints[2] = new Point(polyMat.cols(), polyMat.rows());
        dstPoints[3] = new Point(0, polyMat.rows());

        for(int i=0;i<4;i++)
        {
            polyContours2f.elementAt(maxArea).toArray()[i] = new Point(polyContours2f.elementAt(maxArea).toArray()[i].x * 4, polyContours2f.elementAt(maxArea).toArray()[i].y * 4);
        }



        double point_list[] = new double[4];
        point_list[0] = polyContours2f.elementAt(maxArea).toArray()[0].x;
        point_list[1] = polyContours2f.elementAt(maxArea).toArray()[1].x;
        point_list[2] = polyContours2f.elementAt(maxArea).toArray()[2].x;
        point_list[3] = polyContours2f.elementAt(maxArea).toArray()[3].x;

        double point_list2[] = new double[4];
        point_list2[0] = polyContours2f.elementAt(maxArea).toArray()[0].y;
        point_list2[1] = polyContours2f.elementAt(maxArea).toArray()[1].y;
        point_list2[2] = polyContours2f.elementAt(maxArea).toArray()[2].y;
        point_list2[3] = polyContours2f.elementAt(maxArea).toArray()[3].y;

        double temp = 0.0;
        boolean sorted = false;
        int n = 4;
        while (!sorted) {
            for (int i = 1; i < n; i++) {
                sorted = true;

                if (point_list[i - 1] > point_list[i]) {
                    //swap
                    System.out.println(point_list[i]);
                    System.out.println(point_list[i - 1]);
                    temp = point_list[i - 1];
                    point_list[i - 1] = point_list[i];
                    point_list[i] = temp;
                    temp = point_list2[i - 1];
                    point_list2[i - 1] = point_list2[i];
                    point_list2[i] = temp;
                    System.out.println(point_list[i]);
                    System.out.println(point_list[i - 1]);
                    sorted = false;
                }
            }
            n--;
        }

        polyContours2f.elementAt(maxArea).toArray()[0].x = point_list[0];
        polyContours2f.elementAt(maxArea).toArray()[1].x = point_list[1];
        polyContours2f.elementAt(maxArea).toArray()[2].x = point_list[2];
        polyContours2f.elementAt(maxArea).toArray()[3].x = point_list[3];
        polyContours2f.elementAt(maxArea).toArray()[0].y = point_list2[0];
        polyContours2f.elementAt(maxArea).toArray()[1].y = point_list2[1];
        polyContours2f.elementAt(maxArea).toArray()[2].y = point_list2[2];
        polyContours2f.elementAt(maxArea).toArray()[3].y = point_list2[3];



        if (point_list2[0] < point_list2[1]) {
            srcPoints[0] = new Point(point_list[0], point_list2[0]);
            srcPoints[3] = new Point(point_list[1], point_list2[1]);
        } else {
            srcPoints[0] = new Point(point_list[1], point_list2[1]);
            srcPoints[3] = new Point(point_list[0], point_list2[0]);
        }

        if (point_list2[2] < point_list2[3]) {
            srcPoints[1] = new Point(point_list[2], point_list2[2]);
            srcPoints[2] = new Point(point_list[3], point_list2[3]);
        } else {
            srcPoints[1] = new Point(point_list[3], point_list2[3]);
            srcPoints[2] = new Point(point_list[2], point_list2[2]);
        }

        MatOfPoint2f src = new MatOfPoint2f(srcPoints[0], srcPoints[1], srcPoints[2], srcPoints[3]);
        MatOfPoint2f dst = new MatOfPoint2f(dstPoints[0], dstPoints[1], dstPoints[2], dstPoints[3]);

        Mat transMat = Imgproc.getPerspectiveTransform(src, dst);
        Mat outMat = new Mat();

        Imgproc.warpPerspective(org, outMat, transMat, org.size()); //座標轉換

        return outMat;
    }
}
