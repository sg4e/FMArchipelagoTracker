package gg.archipelago.client.parts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class Game implements Serializable {

    @SerializedName("version")
    public int version;

    @SerializedName("hash")
    public String hash;

    @SerializedName("item_name_to_id")
    public Map<String,Long> itemNameToId = new HashMap<>();

    @SerializedName("location_name_to_id")
    public Map<String,Long> locationNameToId = new HashMap<>();
}
