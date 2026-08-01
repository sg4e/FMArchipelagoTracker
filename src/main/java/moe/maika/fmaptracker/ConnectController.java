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

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class ConnectController {

    @FXML
    private TextField serverAndPortField;

    @FXML
    private TextField playerField;

    @FXML
    private TextField passwordField;
    
    private TrackerController trackerController;

    @FXML
    private void onConnect() {
        trackerController.connect(serverAndPortField.getText().trim(), playerField.getText(), passwordField.getText());
    }

    @FXML
    private void onCancel() {
        trackerController.cancelConnectModal();
        serverAndPortField.clear();
        playerField.clear();
        passwordField.clear();
    }

    @FXML
    private void onTextEntry(KeyEvent event) {
        if(event.getCode() == KeyCode.ENTER)
            onConnect();
    }

    public void setTrackerController(TrackerController trackerController) {
        this.trackerController = trackerController;
    }

    public void setStoredFields(String serverAndPortField, String playerField) {
        this.serverAndPortField.setText(serverAndPortField);
        this.playerField.setText(playerField);
    }
}
