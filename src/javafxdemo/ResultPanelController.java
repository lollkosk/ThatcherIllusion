/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javafxdemo;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

/**
 * FXML Controller class for the ResultPanel shown after a result image was
 * created.
 *
 * @author Marek Zuzi
 */
public class ResultPanelController implements Initializable {
    @FXML private TextField text_email;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // validate the email on any change in the text field
        text_email.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if(newValue.isEmpty()) {
                    text_email.setText("@");
                }
            }
        });
        text_email.setText("@");
    }
    
    //@FXML protected void onOk(ActionEvent e) {
    private void onOk(ActionEvent e) {
        // call the sendEmail method with entered arguments
        ThatcherIllusionApp.getApp().sendEmail(text_email.getText());
    }
    
    @FXML protected void onEnter(ActionEvent e) {
        // submit the form if entered email address is valid
        if(text_email.getText().matches("^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
            onOk(e);
        }
    }
}
