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

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import moe.maika.fmaptracker.TrackerController.Farm;

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

    private List<Label> farmLabels;

    public void initialize() {
        farmLabels = List.of(firstFarm, secondFarm, thirdFarm);
    }

    public void setDuelRank(String duelRank) {
        topLabel.setText(String.format("Top %s Farms:", duelRank));
    }

    public void setDuelistImage(Image image) {
        duelistImage.setImage(image);
    }

    public void updateTopFarms(List<Farm> orderedFarms, Image[] images) {
        for(int i = 0; i < farmLabels.size(); i++) {
            if(i < orderedFarms.size()) {
                Farm farm = orderedFarms.get(i);
                farmLabels.get(i).setText(String.format("%s\n%.2f%% (%s from %s)",
                        farm.duelist().name(),
                        farm.totalProbability() / 2048d * 100d,
                        farm.missingDrops(),
                        farm.totalDrops()));
            }
            else {
                farmLabels.get(i).setText("");
            }
        }
        if(!orderedFarms.isEmpty()) {
            setDuelistImage(images[orderedFarms.get(0).duelist().id()]);
        }
        else {
            setDuelistImage(images[0]);
        }
    }
}
