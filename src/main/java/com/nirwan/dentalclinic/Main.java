package com.nirwan.dentalclinic;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import javafx.application.Application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;



public class Main  {


    public static void main(String[] args) {
        DatabaseConnection.initializeDatabase();
        Application.launch(JavaFXApplication.class, args);

    }
}