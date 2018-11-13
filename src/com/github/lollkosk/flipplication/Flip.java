/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.lollkosk.flipplication;

import java.util.ArrayList;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

/**
 * Class that handles flipping of mouth and eyes in a face.
 * @author Marek Zuzi
 */
public class Flip {
    // constants
    private final static Size KERNEL_SIZE = new Size(9, 9);
    private final static Scalar SCALAR_0 = new Scalar(0, 0, 0);
    private final static Scalar SCALAR_255 = new Scalar(255, 255, 255);

    // flag decidint whether to use inpainting in the original image to make the
    // effect better
    private boolean useInpaint = true;

    /**
     * Flips eye in a face image.
     * @param face image of a face
     * @param eyeRegion region where the eye to be flipped is
     */
    public void FlipEye(Mat face, Rect eyeRegion) {
        Mat flipped = new Mat();
        Mat roi = new Mat(face, eyeRegion);
        Mat mask = roi.clone();

        // make an elliptic mask to estimate the eye shape
        mask.setTo(SCALAR_0);
        Point middle = new Point(eyeRegion.width / 2, eyeRegion.height / 2);
        Size maskSize = new Size(eyeRegion.width * 0.5, eyeRegion.height * 0.30);
        Imgproc.ellipse(mask, middle, maskSize, 0, 0, 360, SCALAR_255, -1);
        
        // blur the eye mask to make smooth edges after it is flipped
        Imgproc.GaussianBlur(mask, mask, KERNEL_SIZE, 0, 0);
        
        // flip the eye itself and blend it back to the face using smooth mask
        Core.flip(roi, flipped, 0);
        Mat blended = blendByMask(flipped, roi, mask);

        // if inpaint is used, use it to cover up the edges
        if (useInpaint) {
            flipped = roi.clone();
            ArrayList<Mat> maskChannels = new ArrayList<>(3);
            Core.split(mask, maskChannels);
            Photo.inpaint(roi, maskChannels.get(1), flipped, 3, Photo.INPAINT_NS);
            flipped.copyTo(roi);
        }
        
        // draw the flipped eye back to the original image
        blended.copyTo(roi);
    }

    /**
     * Flips mouth in a face image.
     * @param face image of a face
     * @param origMouthRegion region where the mouth to be flipped is
     */
    public void FlipMouth(Mat face, Rect origMouthRegion) {
        // enlarge the mouth region a bit to allow for smooth edges of the flipped mouth
        Rect mouthRegion = new Rect(origMouthRegion.x - 10, origMouthRegion.y - 5, origMouthRegion.width+20,(int) (origMouthRegion.height * 0.9));
        mouthRegion.x = Math.max(0, mouthRegion.x);
        mouthRegion.y = Math.max(0, mouthRegion.y);
        mouthRegion.width = Math.min(face.width() - mouthRegion.x, mouthRegion.width);
        mouthRegion.height = Math.min(face.height() - mouthRegion.x, mouthRegion.height);
        Mat flipped = new Mat();
        Mat roi = new Mat(face, mouthRegion);
        Mat mask = roi.clone();

        // make an elliptic mask to estimate the mouth shape
        mask.setTo(SCALAR_0);
        Point middle = new Point(mouthRegion.width / 2, mouthRegion.height / 2);
        Size maskSize = new Size(mouthRegion.width * 0.45, mouthRegion.height * 0.45);
        Imgproc.ellipse(mask, middle, maskSize, 0, 0, 360, SCALAR_255, -1);

        // blur the mouth mask to make smooth edges after it is flipped
        Imgproc.GaussianBlur(mask, mask, KERNEL_SIZE, 0, 0);
        // flip the mouth itself and blend it back to the face using smooth mask
        Core.flip(roi, flipped, 0);
        Mat blended = blendByMask(flipped, roi, mask);

        // if inpaint is used, use it to cover up the edges
        if (useInpaint) {
            flipped = roi.clone();
            ArrayList<Mat> maskChannels = new ArrayList<>(3);
            Core.split(mask, maskChannels);
            Photo.inpaint(roi, maskChannels.get(1), flipped, 3, Photo.INPAINT_NS);
            flipped.copyTo(roi);
        }
        
        // draw the flipped mouth back to the original image
        blended.copyTo(roi);
    }

    /**
     * Blends two images of the same size by given mask
     * @param img1 image to blend
     * @param img2 image to blend
     * @param mask mask to determine the ratio between two blended images
     * @return blended image
     */
    private Mat blendByMask(Mat img1, Mat img2, Mat mask) {
        Mat blend = new Mat();
        Mat maskNeg = new Mat();
        Core.bitwise_not(mask, maskNeg);
        Core.add(img1.mul(mask, 1.0 / 255.0), img2.mul(maskNeg, 1.0 / 255.0), blend);
        return blend;
    }
}
