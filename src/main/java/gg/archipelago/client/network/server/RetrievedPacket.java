package gg.archipelago.client.network.server;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

import gg.archipelago.client.network.APPacket;
import gg.archipelago.client.network.APPacketType;

public class RetrievedPacket  extends APPacket {

    @SerializedName("keys")
    public Map<String, Object> keys;

    @SerializedName("request_id")
    public int requestID;

    public RetrievedPacket() {
        super(APPacketType.Retrieved);
    }
}

