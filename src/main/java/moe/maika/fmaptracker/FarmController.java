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

import java.util.List;
import java.util.logging.Level;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import moe.maika.fmaptracker.Farm;

public class FarmController {
    @FXML
    private ImageView duelistImage;
    @FXML
    private Label topLabel;
    @FXML
    private Label firstFarm;
    @FXML
    private Label secondFarm;
    @FXML
    private Label thirdFarm;
    @FXML
    private Label oneLabel;
    @FXML
    private Label twoLabel;
    @FXML
    private Label threeLabel;

    private static final int FARM_COUNT = 3;
    private Label[] farmLabels;
    private Label[] numberLabels;
    private Farm[] topFarms;
    private TrackerController tracker;

    public void initialize() {
        farmLabels = new Label[] {firstFarm, secondFarm, thirdFarm};
        numberLabels = new Label[] {oneLabel, twoLabel, threeLabel};
        topFarms = new Farm[FARM_COUNT];
    }

    public void setDuelRank(String duelRank) {
        topLabel.setText(String.format("Top %s Farms:", duelRank));
    }

    public void setDuelistImage(Image image) {
        duelistImage.setImage(image);
    }

    public void setTrackerController(TrackerController tracker) {
        this.tracker = tracker;
    }

    public void updateTopFarms(List<Farm> orderedFarms, Image[] images) {
        for(int i = 0; i < FARM_COUNT; i++) {
            Label label = farmLabels[i];
            if(i < orderedFarms.size()) {
                Farm farm = orderedFarms.get(i);
                label.setText(String.format("%s\n%.2f%% (%s from %s)",
                        farm.duelist().name(),
                        farm.totalProbability() / 2048d * 100d,
                        farm.missingDrops(),
                        farm.totalDrops()));
                label.setCursor(Cursor.HAND);
                numberLabels[i].setCursor(Cursor.HAND);
                topFarms[i] = farm;
            }
            else {
                label.setText("");
                label.setCursor(null);
                numberLabels[i].setCursor(null);
                topFarms[i] = null;
            }
        }
        if(!orderedFarms.isEmpty()) {
            setDuelistImage(images[orderedFarms.get(0).duelist().id()]);
            duelistImage.setCursor(Cursor.HAND);
            topLabel.setCursor(Cursor.HAND);
        }
        else {
            setDuelistImage(images[0]);
            duelistImage.setCursor(null);
            topLabel.setCursor(null);
        }
    }

    @FXML
    private void onClick(MouseEvent event) {
        int index;
        Object source = event.getSource();
        if(source == topLabel || source == firstFarm || source == oneLabel || source == duelistImage) {
            index = 0;
        }
        else if(source == secondFarm || source == twoLabel) {
            index = 1;
        }
        else if(source == thirdFarm || source == threeLabel) {
            index = 2;
        }
        else {
            tracker.log.log(Level.WARNING, "Unknown source for click on farm: " + source);
            return;
        }
        Farm selected = topFarms[index];
        if(selected != null) {
            tracker.setSelectedFarm(selected);
        }
    }
}
