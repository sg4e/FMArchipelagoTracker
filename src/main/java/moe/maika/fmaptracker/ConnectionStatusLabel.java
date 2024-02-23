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

import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public class ConnectionStatusLabel {

    private final Label label;
    private Status currentStatus;

    public enum Status {
        CONNECTED, AWAITING_SLOT_DATA, CONNECTING, DISCONNECTED
    }
    
    public final void setStatus(Status status) {
        // there's a chance that the slot data is returned so fast that this is updated backwards
        if(status == Status.AWAITING_SLOT_DATA && currentStatus == Status.CONNECTED) {
            return;
        }
        currentStatus = status;
        switch (status) {
            case CONNECTED -> {
                label.setText("Connected to AP");
                label.setTextFill(Color.GREEN);
            }
            case AWAITING_SLOT_DATA -> {
                label.setText("Awaiting Slot Data...");
                label.setTextFill(Color.YELLOW);
            }
            case CONNECTING -> {
                label.setText("Connecting...");
                label.setTextFill(Color.ORANGE);
            }
            case DISCONNECTED -> {
                label.setText("Disconnected");
                label.setTextFill(Color.RED);
            }
        }
    }

    public ConnectionStatusLabel(Label label) {
        this.label = label;
        setStatus(Status.DISCONNECTED);
    }
}
