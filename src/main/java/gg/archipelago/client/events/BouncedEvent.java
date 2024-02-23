package gg.archipelago.client.events;

import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

public class BouncedEvent implements Event {

    @SerializedName("games")
    public Set<String> games;

    @SerializedName("slots")
    public Set<Integer> slots;

    @SerializedName("tags")
    public Set<String> tags;

    @SerializedName("data")
    private Map<String, Object> data;

    public BouncedEvent(Set<String> games, Set<String> tags, Set<Integer> slots, Map<String, Object> data) {
        this.games = games;
        this.tags = tags;
        this.slots = slots;
        this.data = data;
    }

    public int getInt(String key) {
        return ((Double)data.get(key)).intValue();
    }

    public float getFloat(String key) {
        return (Float) data.get(key);
    }

    public double getDouble(String key) {
        return (Double) data.get(key);
    }

    public String getString(String key) {
        return (String)data.get(key);
    }

    public boolean getBoolean(String key) {
        return (boolean)data.get(key);
    }

    public Object getObject(String key) {
        return data.get(key);
    }


    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

}
