package gg.archipelago.client.network.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import gg.archipelago.client.network.APPacket;
import gg.archipelago.client.network.APPacketType;
import gg.archipelago.client.parts.NetworkPlayer;

public class ConnectedPacket extends APPacket {

    @SerializedName("team")
    public int team = -1;
    @SerializedName("slot")
    public int slot = -1;
    @SerializedName("players")
    public ArrayList<NetworkPlayer> players;
    @SerializedName("missing_locations")
    public Set<Long> missingLocations = new HashSet<>();
    @SerializedName("checked_locations")
    public Set<Long> checkedLocations = new HashSet<>();

    public ConnectedPacket() {
        super(APPacketType.Connected);
    }
}
