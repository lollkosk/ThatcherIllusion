/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.lollkosk.flipplication;

import javafxdemo.ThatcherIllusionApp;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * Class handling mails sent with the results of the illusion.
 * @author Marek Zuzi
 */
public class Mailer {

    // constants
    private static final String CONFIG_FILENAME = "smtp.properties";
    private final Properties config = new Properties();

    /**
     * Initializes the mailer with its SMTP config.
     * @throws IOException if the config could not be loaded properly.
     */
    public Mailer() throws IOException {
        try (FileInputStream input = new FileInputStream(CONFIG_FILENAME);) {
            // load a properties file
            config.load(input);
        } catch (IOException ex) {
            throw new IOException("Could not load email config from " + CONFIG_FILENAME, ex);
        }
    }

    /**
     * Sends email with the original and flipped images to a given address using
     * initialized smtp config. Runs in separate thread.
     * @param emailAddress address to send email to
     * @param originalImage original image of the thatcher illusion
     * @param flippedImage upside down of the thatcher illusion
     */
    public void sendEmail(String emailAddress, Mat originalImage, Mat flippedImage) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Session session = Session.getDefaultInstance(config,
                        new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(config.getProperty("mail.smtp.user"), config.getProperty("mail.smtp.password"));
                    }
                });

                try {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(config.getProperty("FROM_ADDRESS")));
                    message.setRecipients(Message.RecipientType.TO,
                            InternetAddress.parse(emailAddress));
                    message.setSubject(Translation.T("EMAIL.SUBJECT", ThatcherIllusionApp.getApp().language()));

                    Multipart parts = new MimeMultipart();

                    // text
                    BodyPart body = new MimeBodyPart();
                    body.setText(Translation.T("EMAIL.BODY", ThatcherIllusionApp.getApp().language()));
                    parts.addBodyPart(body);

                    // files
                    BodyPart attachmentPart = new MimeBodyPart();
                    MatOfByte byteMat = new MatOfByte();
                    Imgcodecs.imencode(".jpg", prepareImage(flippedImage), byteMat);
                    DataSource src = new ByteArrayDataSource(byteMat.toArray(), "image/jpeg");
                    attachmentPart.setDataHandler(new DataHandler(src));
                    attachmentPart.setFileName("image.jpg");
                    parts.addBodyPart(attachmentPart);

                    message.setContent(parts);

                    Transport.send(message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread t = new Thread(r, "sendEmail");
        t.start();
    }

    /**
     * Prepares the image to send it in the email with side by side comparison.
     * @param frame
     * @return 
     */
    private Mat prepareImage(Mat frame) {
        Mat result = new Mat(frame.rows() * 2, frame.cols(), frame.type());
        Mat flipped = new Mat();
        Core.flip(frame, flipped, 0);
        flipped.copyTo(result.submat(new Rect(0, 0, frame.width(), frame.height())));
        frame.copyTo(result.submat(new Rect(0, frame.height(), frame.width(), frame.height())));
        return result;
    }
}
