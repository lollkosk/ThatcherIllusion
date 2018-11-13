/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javafxdemo;

import com.github.lollkosk.flipplication.Detector;
import com.github.lollkosk.flipplication.Flip;
import com.github.lollkosk.flipplication.Mailer;
import com.github.lollkosk.flipplication.Translation;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

/**
 *
 * @author Marek Zuzi
 */
public class ThatcherIllusionApp extends Application {

    // constants
    private static Properties config;
    private static int SUCCESSES_NEEDED = 2;
    private static float CLOSE_TRESHOLD = 50;
    private static String EXIT_SECRET = "exit";
    private static KeyCode EXIT_CHAR = KeyCode.PERIOD;
    private static int CLOSE_PANEL_DELAY = 4;
    private static boolean FIT_WHOLE_SCREEN = true;
    private static int WEBCAM = 0;

    private static boolean DEBUG_MODE = false;

    private static String LANGUAGE = "cs";

    // primary stage variables
    private Stage primaryStage;
    private Scene primaryScene;
    private StackPane primaryRoot;
    private ImageView imgView;
    private Label infoLabel;

    // panel variables
    private Parent panelRoot;

    // main loop thread
    private Thread mainLoopThread;

    // application logic objects
    private static ThatcherIllusionApp app;
    private final Detector detector = new Detector();
    private final Flip flip = new Flip();
    private Mailer mailer;

    // application state variables
    private boolean showFlip = false;
    private boolean drawRectangles = false;
    private Rect prevFaceRect = null;
    private Mat busyWithFrame = null;
    private Mat flippedFrame = null;
    private int successes = 0;
    private int exitKeypressCount = 0;
    private String currentLanguage = "cs";

    public String language() {
        return currentLanguage;
    }

    public void setLanguage(String code) {
        currentLanguage = code;

        infoLabel.setText(Translation.T("INSTRUCTIONS", currentLanguage));
        Label labelDescription = (Label) panelRoot.lookup("#label_description");
        labelDescription.setText(Translation.T("PANEL.DESCRIPTION", currentLanguage));
    }

    private static void initConfig() {
        config = new Properties();
        File configFile = new File("." + File.separator + "config.properties");

        try (FileInputStream input = new FileInputStream(configFile);) {
            config.load(input);
        } catch (IOException ex) {
            Logger.getLogger(ThatcherIllusionApp.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (config.containsKey("SUCCESSES_NEEDED")) {
            SUCCESSES_NEEDED = Integer.parseInt(config.getProperty("SUCCESSES_NEEDED"));
        }
        if (config.containsKey("EXIT_SECRET")) {
            EXIT_SECRET = config.getProperty("EXIT_SECRET");
        }
        if (config.containsKey("EXIT_CHAR")) {
            EXIT_CHAR = KeyCode.valueOf(config.getProperty("EXIT_CHAR"));
        }
        if (config.containsKey("CLOSE_PANEL_DELAY")) {
            CLOSE_PANEL_DELAY = Integer.parseInt(config.getProperty("CLOSE_PANEL_DELAY"));
        }
        if (config.containsKey("DEBUG_MODE")) {
            DEBUG_MODE = config.getProperty("DEBUG_MODE").equals("true");
        }
        if (config.containsKey("LANGUAGE")) {
            LANGUAGE = config.getProperty("LANGUAGE");
        }
        if (config.containsKey("CLOSE_TRESHOLD")) {
            CLOSE_TRESHOLD = Float.parseFloat(config.getProperty("CLOSE_TRESHOLD"));
        }
        if (config.containsKey("FIT_WHOLE_SCREEN")) {
            FIT_WHOLE_SCREEN = config.getProperty("FIT_WHOLE_SCREEN").equals("true");
        }
        if (config.containsKey("WEBCAM")) {
            WEBCAM = Integer.parseInt(config.getProperty("WEBCAM"));
        }
    }

    public void openPanel(Mat originalFrame, Rect face_roi, Rect mouth, Rect eye_left, Rect eye_right) {
        // set busy flag so that panel would not appear again
        busyWithFrame = originalFrame.clone();

        flippedFrame = originalFrame.clone();
        Mat face = new Mat(flippedFrame, face_roi);
        flip.FlipMouth(face, mouth);
        flip.FlipEye(face, eye_left);
        flip.FlipEye(face, eye_right);

        // setup left image
        ImageView iv = (ImageView) panelRoot.lookup("#imview_left");
        iv.setImage(convertToFxImage(flippedFrame));

        // setup right image
        Mat flipped = new Mat();
        Core.flip(flippedFrame, flipped, 0);
        iv = (ImageView) panelRoot.lookup("#imview_right");
        iv.setImage(convertToFxImage(flipped));

        // add panel to the main stack pane
        primaryRoot.getChildren().add(panelRoot);
        
        // request focus in panel
        TextField input = (TextField)panelRoot.lookup("#text_email");
        input.requestFocus();
    }

    public void closePanel(boolean closeAfterDelay) {
        // throw the panel away
        primaryRoot.getChildren().remove(panelRoot);

        TextField emailField = (TextField) panelRoot.lookup("#text_email");
        emailField.setText("");

        // reset busy flag
        if (closeAfterDelay) {
            resetBusyLater();
        } else {
            busyWithFrame = null;
        }
    }

    public void sendEmail(String emailAddress) {
        // send email using mailer
        mailer.sendEmail(emailAddress, busyWithFrame, flippedFrame);

        // close the panel
        closePanel(true);
    }

    private void resetBusyLater() {
        Runnable r = () -> {
            try {
                Thread.sleep(CLOSE_PANEL_DELAY * 1000);
                busyWithFrame = null;
            } catch (InterruptedException ex) {
                busyWithFrame = null;
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // try to initialize Mailer, it can throw an exception
        mailer = new Mailer();

        app = this;
        this.primaryStage = primaryStage;

        // construct root stackPane
        primaryRoot = new StackPane();

        // construct and add image view for webcam stream
        imgView = new ImageView();
        primaryRoot.getChildren().add(imgView);

        // construct and add information overlay label
        infoLabel = new Label("");
        primaryRoot.getChildren().add(infoLabel);
        infoLabel.setStyle("-fx-font-size: 12px;-fx-font-weight: bold;-fx-effect: dropshadow( gaussian , rgba(255,255,255,0.5) , 0,0,0,1 );");

        // load panel layout
        panelRoot = FXMLLoader.load(ThatcherIllusionApp.class.getResource("ResultPanel.fxml"));
        panelRoot.setTranslateY(50);

        // setup scene and the primary stage
        primaryScene = new Scene(primaryRoot);
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(primaryScene);

        // fire change of language to the default one
        setLanguage(LANGUAGE);

        // set key handler
        primaryScene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                KeyCode kc = event.getCode();

                // watch the special exit backdoor
                if (kc == EXIT_CHAR) {
                    exitKeypressCount++;
                    if (exitKeypressCount >= 5) {
                        TextInputDialog dialog = new TextInputDialog();
                        dialog.setTitle(Translation.T("ENTER_PASSWORD", currentLanguage));
                        dialog.setGraphic(null);
                        dialog.setHeaderText(null);
                        dialog.setContentText(Translation.T("ENTER_PASSWORD_TO_EXIT", currentLanguage));
                        dialog.showAndWait();
                        if (dialog.getEditor().getText().equals(EXIT_SECRET)) {
                            Platform.exit();
                        } else {
                            exitKeypressCount = 0;
                            return;
                        }
                    }
                } else {
                    exitKeypressCount = 0;
                }

                // handle some special keys
                if (DEBUG_MODE == true) {
                    switch (kc) {
                        case R:
                            drawRectangles = !drawRectangles;
                            break;
                        case S:
                            if (mainLoopThread == null) {
                                startMainLoop();
                            } else {
                                stopMainLoop();
                            }
                            break;
                        case F11:
                            primaryStage.setFullScreen(!primaryStage.isFullScreen());
                            if (!primaryStage.isFullScreen()) {
                                primaryStage.setWidth(800);
                                primaryStage.setHeight(600);
                            }
                            break;
                        case ESCAPE:
                            if (busyWithFrame != null) {
                                closePanel(false);
                            } else {
                                Platform.exit();
                            }
                            break;
                        case F:
                            showFlip = !showFlip;
                            break;
                    }
                } else if (kc == KeyCode.ESCAPE) {
                    if (busyWithFrame != null) {
                        closePanel(false);
                    }
                }
            }
        });

        // finally, show the main window
        primaryStage.show();

        // hack the position of info label
        infoLabel.setTranslateY(20 - (primaryStage.getHeight() / 2.0));
        //infoLabel.setStyle("-fx-background-color: aliceblue;");

        // run the webcam thread
        startMainLoop();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // load the OpenCV
        File f = new File(".");
        System.load(f.getAbsolutePath() + File.separator + "opencv_java310.dll");

        // initialize translations
        Translation.init();

        // initialize app using config file
        initConfig();

        // start the app
        launch(args);
    }

    public static ThatcherIllusionApp getApp() {
        return app;
    }

    private void startMainLoop() {
        if (mainLoopThread != null) {
            throw new IllegalStateException("Main loop is already running!");
        }

        Runnable mainLoopTask = new Runnable() {
            @Override
            public void run() {
                captureAndProcess();
            }
        };
        mainLoopThread = new Thread(mainLoopTask, "Main Loop Thread");
        mainLoopThread.start();
    }

    private void stopMainLoop() {
        if (mainLoopThread == null) {
            throw new IllegalStateException("Main loop is not running!");
        }

        mainLoopThread.interrupt();
        mainLoopThread = null;
    }

    private void captureAndProcess() {
        VideoCapture webcam = new VideoCapture(WEBCAM);

        Mat frame = new Mat();
        Mat flipped = new Mat();
        while (!Thread.interrupted()) {
            if (webcam.read(frame)) {
                // do a horizontal flip to make frames appear more natural
                Core.flip(frame, flipped, 1);
                detectAndDisplay(flipped);
            }
        }
        // clean up
        webcam.release();
    }

    private void detectAndDisplay(Mat frame) {
        if (frame == null || frame.empty()) {
            return;
        }
        Mat originalFrame = frame.clone();

        int partsFound = 0;
        Mat frame_preprocessed = detector.preprocessImage(frame);

        List<Rect> faces = detector.detectFace(frame_preprocessed);
        Rect faceRect = getLargestResult(faces);

        if (faceRect != null && areCloseEnough(faceRect, prevFaceRect, CLOSE_TRESHOLD)) {
            prevFaceRect = faceRect;
            partsFound = 0;
            Mat face = new Mat(frame, faceRect);

            List<Rect> eyes_l = detector.detectLeftEye(face);
            Rect rect_el = getLargestResult(eyes_l);
            if (rect_el != null) {
                if (showFlip) {
                    flip.FlipEye(face, rect_el);
                }
                if (drawRectangles) {
                    Imgproc.rectangle(face, rect_el.tl(), rect_el.br(), new Scalar(0, 255, 0));
                }
                partsFound++;
            }
            List<Rect> eyes_r = detector.detectRightEye(face);
            Rect rect_er = getLargestResult(eyes_r);
            if (rect_er != null) {
                if (showFlip) {
                    flip.FlipEye(face, rect_er);
                }
                if (drawRectangles) {
                    Imgproc.rectangle(face, rect_er.tl(), rect_er.br(), new Scalar(0, 255, 0));
                }
                partsFound++;
            }
            List<Rect> mouths = detector.detectMouth(face);
            Rect rect_m = getLargestResult(mouths);
            if (rect_m != null) {
                if (showFlip) {
                    flip.FlipMouth(face, rect_m);
                }
                if (drawRectangles) {
                    Imgproc.rectangle(face, rect_m.tl(), rect_m.br(), new Scalar(0, 255, 0));
                }
                partsFound++;
            }

            if (drawRectangles) {
                Imgproc.rectangle(frame, faceRect.tl(), faceRect.br(), new Scalar(255, 0, 0));
            }
            if (busyWithFrame == null && partsFound == 3) {
                successes++;
                if (successes >= SUCCESSES_NEEDED) {
                    prevFaceRect = null;
                    busyWithFrame = frame;
                    successes = 0;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            openPanel(originalFrame.clone(), faceRect, rect_m, rect_el, rect_er);
                        }
                    });
                }
            } else {
                successes = 0;
            }
        }

        // show frame which may contain rectangles/shown flips
        Mat result_frame = resizeToScreen(frame);
        imgView.setImage(convertToFxImage(result_frame));
    }

    private Mat resizeToScreen(Mat image) {
        Mat resized = new Mat();
        double ratio = 1;
        if(FIT_WHOLE_SCREEN) {
            ratio = Math.max(primaryStage.getWidth() / image.width(), primaryStage.getHeight() / image.height());
        } else {
            ratio = Math.min(primaryStage.getWidth() / image.width(), primaryStage.getHeight() / image.height());
        }
        Imgproc.resize(image, resized, new Size(image.width() * ratio, image.height() * ratio), 0, 0, Imgproc.INTER_LINEAR);
        return resized;
    }

    private Image convertToFxImage(Mat image) {
        // this way try to convert Mat into Java-usable image
        MatOfByte byteMat = new MatOfByte();
        Imgcodecs.imencode(".bmp", image, byteMat);
        return new Image(new ByteArrayInputStream(byteMat.toArray()));
    }

    private Rect getLargestResult(List<Rect> detectedRects) {
        Rect largest = null;
        for (Rect r : detectedRects) {
            if (largest == null) {
                largest = r;
            }
            if (largest.area() < r.area()) {
                largest = r;
            }
        }
        return largest;
    }

    private boolean areCloseEnough(Rect r1, Rect r2, double treshold) {
        if (r1 == null || r2 == null) {
            return true;
        }
        int dx1 = r1.x - r2.x;
        int dy1 = r1.y - r2.y;
        int dx2 = (r1.x + r1.width) - (r2.x + r2.width);
        int dy2 = (r1.y + r1.height) - (r2.y + r2.height);
        return Math.sqrt(dx1 * dx1 + dy1 * dy1) < treshold && Math.sqrt(dx2 * dx2 + dy2 * dy2) < treshold;
    }

    @Override
    public void stop() throws Exception {
        stopMainLoop();
        super.stop();
    }
}
