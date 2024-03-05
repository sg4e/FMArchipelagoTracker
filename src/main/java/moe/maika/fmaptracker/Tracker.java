/*
 * The MIT License (MIT)
 * Copyright (c) 2024 sg4e
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package moe.maika.fmaptracker;

import java.io.IOException;
import java.util.List;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Tracker extends Application {

    public static final String VERSION = "v1.1.2-SNAPSHOT";

    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle(String.format("Forbidden Memories AP Tracker %s", VERSION));

        Parameters params = getParameters();
        ConnectInfo connectInfo = null;
        List<String> rawParams = params.getRaw();
        if(rawParams.size() >= 2) {
            connectInfo = new ConnectInfo(rawParams.get(0), rawParams.get(1), rawParams.size() > 2 ? rawParams.get(2) : "");
        }
        else {
            // manual hack for quick testing
            // connectInfo = new ConnectInfo("localhost:63636", "FuwawaAbyssgard", "");
        }

        FXMLLoader rootLoader = new FXMLLoader(getClass().getResource("tracker.fxml"));
        VBox root = rootLoader.load();
        TrackerController trackerController = rootLoader.getController();
        trackerController.setConnectInfo(connectInfo);
        stage.getIcons().add(trackerController.getIconForApplication());
        trackerController.setHostServices(getHostServices());

        FXMLLoader connectLoader = new FXMLLoader(getClass().getResource("connect.fxml"));
        GridPane connectModal = connectLoader.load();
        ConnectController connectController = connectLoader.getController();
        connectController.setTrackerController(trackerController);

        Stage connectStage = new Stage();
        connectStage.setTitle("Connect to Archipelago");
        connectStage.initOwner(stage);
        connectStage.initModality(Modality.APPLICATION_MODAL);
        connectStage.setResizable(false);
        connectStage.setScene(new Scene(connectModal));
        Image connectIcon = new Image(getClass().getResourceAsStream("connect_icon.png"));
        connectStage.getIcons().add(connectIcon);
        trackerController.setConnectModalStage(connectStage);
        trackerController.setPrimaryStage(stage);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setOnCloseRequest((e) -> System.exit(0));
        stage.show();
        trackerController.loadFMWorld();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
