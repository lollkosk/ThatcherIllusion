# ThatcherIllusion
Simple app developed for an expo of optical illusions

This app is based on JavaFX and OpenCV and performs real-time face detection via haar cascade filters. Subsequently it detects eyes and mouth in the face and if all parts are detected, the app creates a [Margaret Thatcher Illusion](https://www.theguardian.com/science/head-quarters/2016/sep/19/the-thatcher-illusion-are-faces-special) from this by flipping the eyes and mouth upside down.

This app was designed to be an exhibit on the [Illusorium Exhibition](http://klamarium.cz/). The specific setup of the exhibit required a computer with a webcam available to guests and a keyboard to allow guests to type their email address if they want the resulting illusion to be sent to them. Because of this the app runs in fullscreen and it includes special scripts to disable control keys so that users can't exit the application (e.g. windows key or CTRL+ALT+DEL) and a secret mechanism to exit the app by the staff of the expo.
