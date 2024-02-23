package gg.archipelago.client.network.client;

import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import gg.archipelago.client.events.Event;
import gg.archipelago.client.network.APPacket;
import gg.archipelago.client.network.APPacketType;

public class BouncePacket extends APPacket implements Event {

    @Expose
    @SerializedName("games")
    public String[] games = new String[]{};

    @Expose
    @SerializedName("slots")
    public int[] slots = new int[]{};

    @Expose
    @SerializedName("tags")
    public String[] tags = new String[]{};

    @Expose
    @SerializedName("data")
    private Map<String, Object> data;

    public BouncePacket() {
        super(APPacketType.Bounce);
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
