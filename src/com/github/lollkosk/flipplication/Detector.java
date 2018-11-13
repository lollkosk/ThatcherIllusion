/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.lollkosk.flipplication;

import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 * Detector that handles detection of a face and its eyes and mouth in an image.
 * @author Marek Zuzi
 */
public class Detector {
    private final CascadeClassifier cascade_face;
    private final CascadeClassifier cascade_eye_left;
    private final CascadeClassifier cascade_eye_right;
    private final CascadeClassifier cascade_mouth;
    
    /**
     * Initializes the detectors including loading required haar cascades from
     * files.
     * @throws NullPointerException if a cascade file was not successfully loaded.
     */
    public Detector() {
        // Initialization of detectors from suitable haar cascade files.
        cascade_face = new CascadeClassifier("cascades/haarcascade_frontalface_alt.xml");
        if(cascade_face.empty()) {
            throw new NullPointerException("face cascade could not be loaded.");
        }
        
        cascade_eye_left = new CascadeClassifier("cascades/haarcascade_eye_tree_eyeglasses.xml");
        if(cascade_eye_left.empty()) {
            throw new NullPointerException("left eye cascade could not be loaded.");
        }
        
        cascade_eye_right = new CascadeClassifier("cascades/haarcascade_eye_tree_eyeglasses.xml");
        if(cascade_eye_right.empty()) {
            throw new NullPointerException("right eye cascade could not be loaded.");
        }
        
        cascade_mouth = new CascadeClassifier("cascades/Mouth.xml");
        if(cascade_mouth.empty()) {
            throw new NullPointerException("mouth cascade could not be loaded.");
        }
    }
    
    /**
     * Preprocesses the image for detection.
     * @param image image to preprocess for detection
     * @return image that is preprocessed (converted to grayscale and histogram
     *     equalized).
     */
    public Mat preprocessImage(Mat image) {
        Mat result = new Mat();
        Imgproc.cvtColor(image, result, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(result, result);
        return result;
    }
    
    /**
     * Detects face within an image.
     * @param frame image to detect face in.
     * @return list of detected Rectangles with faces in the image.
     */
    public List<Rect> detectFace(Mat frame) {
        return detectWith(frame, cascade_face);
    }
    
    /**
     * Detects left eye within an image. Searches only in left top quarter of
     * the image.
     * @param frame image to detect left eye in.
     * @return list of detected Rectangles with left eyes in the image.
     */
    public List<Rect> detectLeftEye(Mat frame) {
        Rect leftROI = new Rect(0, 0, frame.width()/2, frame.height()/2);
        Mat leftFrame = new Mat(frame, leftROI);
        return detectWith(leftFrame, cascade_eye_left);
    }
    
    /**
     * Detects right eye within an image. Searches only in right top quarter of
     * the image.
     * @param frame image to detect right eye in.
     * @return list of detected Rectangles with right eyes in the image.
     */
    public List<Rect> detectRightEye(Mat frame) {
        Rect rightROI = new Rect(frame.width()/2, 0, frame.width()/2, frame.height()/2);
        Mat rightFrame = new Mat(frame, rightROI);
        List<Rect> result = detectWith(rightFrame, cascade_eye_right);
        for (Rect r : result) {
            r.x += frame.width()/2;
	}
        return result;
    }
    
    /**
     * Detects mouth within an image. Searches only in a region in the bottom
     * center of the image.
     * the image.
     * @param frame image to detect mouth in.
     * @return list of detected Rectangles with mouths in the image.
     */
    public List<Rect> detectMouth(Mat frame) {
        Rect mouthROI = new Rect(frame.width()/4, (frame.height()/3)*2, frame.width()/2, frame.height()/3);
        Mat mouthFrame = new Mat(frame, mouthROI);
        List<Rect> result = detectWith(mouthFrame, cascade_mouth);
        for (Rect r : result) {
            r.x += frame.width()/4;
            r.y += (frame.height()/3)*2;
	}
        return result;
    }
    
    /**
     * Helper function that applies cascade detector to an image and returns the
     * results of the detection.
     * @param frame image to detect in.
     * @param cascade properly initialized detector to use.
     * @return list of regions that were detected by the detector in the image.
     */
    private List<Rect> detectWith(Mat frame, CascadeClassifier cascade) {
        MatOfRect result = new MatOfRect();
        cascade.detectMultiScale(frame, result);
        return result.toList();
    }
}
