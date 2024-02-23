package gg.archipelago.client.network.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import gg.archipelago.client.network.APPacket;
import gg.archipelago.client.network.APPacketType;

public class BouncedPacket extends APPacket {

    @SerializedName("games")
    public Set<String> games = new HashSet<>();

    @SerializedName("slots")
    public Set<Integer> slots = new HashSet<>();

    @SerializedName("tags")
    public Set<String> tags = new HashSet<>();

    @SerializedName("data")
    public final Map<String, Object> data = new HashMap<>();

    public BouncedPacket() {
        super(APPacketType.Bounced);
    }
}
