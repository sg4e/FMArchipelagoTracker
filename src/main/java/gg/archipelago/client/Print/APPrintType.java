package gg.archipelago.client.Print;

import com.google.gson.annotations.SerializedName;

public enum APPrintType {
    @SerializedName("text")
    TEXT,
    @SerializedName("player_id")
    PLAYER_ID,
    @SerializedName("player_name")
    PLAYER_NAME,
    @SerializedName("item_id")
    ITEM_ID,
    @SerializedName("item_name")
    ITEM_NAME,
    @SerializedName("location_id")
    LOCATION_ID,
    @SerializedName("location_name")
    LOCATION_NAME,
    @SerializedName("entrance_name")
    ENTRANCE_NAME,
    @SerializedName("color")
    COLOR

}
