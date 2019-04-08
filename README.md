# ThatcherIllusion

Developed in 2016 for an expo of optical illusions organized by Czech Academy of Sciences.

This app is based on JavaFX and OpenCV and performs real-time face detection via haar cascade filters. Subsequently it detects eyes and mouth in the face and if all parts are detected, the app creates a [Margaret Thatcher Illusion](https://www.theguardian.com/science/head-quarters/2016/sep/19/the-thatcher-illusion-are-faces-special) from this by flipping the eyes and mouth upside down.

This app was designed to be an exhibit on the [Illusorium Exhibition](http://klamarium.cz/). The specific setup of the exhibit required a computer with a webcam available to guests and a keyboard to allow guests to type their email address if they want the resulting illusion to be sent to them. Because of this the app runs in fullscreen and it includes [special scripts to disable control keys](disableKeys.reg) so that users can't exit the application (e.g. windows key or CTRL+ALT+DEL) and a secret mechanism to exit the app by the staff of the expo.

## Requirements

To successfully run and build the project you will need OpenCV and JavaMail JARs in `lib` folder and the OpenCV DLL file for runtime. Additionally, the app uses haar cascade files from OpenCV that should be in `cascades` folder. Finally, the app needs [SMTP settings](smtp.properties) to be able to send emails. Other options for the app are stored in [config.properties](config.properties).
