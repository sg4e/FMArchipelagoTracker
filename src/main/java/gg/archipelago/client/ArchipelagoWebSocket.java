package gg.archipelago.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.net.ssl.SSLException;

import org.apache.hc.core5.net.URIBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import gg.archipelago.client.Print.APPrint;
import gg.archipelago.client.Print.APPrintType;
import gg.archipelago.client.events.BouncedEvent;
import gg.archipelago.client.events.CheckedLocationsEvent;
import gg.archipelago.client.events.ConnectionAttemptEvent;
import gg.archipelago.client.events.ConnectionResultEvent;
import gg.archipelago.client.events.InvalidPacketEvent;
import gg.archipelago.client.events.LocationInfoEvent;
import gg.archipelago.client.events.RetrievedEvent;
import gg.archipelago.client.events.SetReplyEvent;
import gg.archipelago.client.helper.DeathLink;
import gg.archipelago.client.network.APPacket;
import gg.archipelago.client.network.ConnectionResult;
import gg.archipelago.client.network.client.ConnectPacket;
import gg.archipelago.client.network.client.GetDataPackagePacket;
import gg.archipelago.client.network.client.LocationScouts;
import gg.archipelago.client.network.client.SayPacket;
import gg.archipelago.client.network.server.BouncedPacket;
import gg.archipelago.client.network.server.ConnectedPacket;
import gg.archipelago.client.network.server.ConnectionRefusedPacket;
import gg.archipelago.client.network.server.InvalidPacket;
import gg.archipelago.client.network.server.LocationInfoPacket;
import gg.archipelago.client.network.server.PrintPacket;
import gg.archipelago.client.network.server.ReceivedItemsPacket;
import gg.archipelago.client.network.server.RetrievedPacket;
import gg.archipelago.client.network.server.RoomInfoPacket;
import gg.archipelago.client.network.server.RoomUpdatePacket;
import gg.archipelago.client.network.server.SetReplyPacket;
import gg.archipelago.client.parts.DataPackage;
import gg.archipelago.client.parts.NetworkItem;
import gg.archipelago.client.parts.NetworkPlayer;

public class ArchipelagoWebSocket extends WebSocketClient {

    private final static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(ArchipelagoWebSocket.class.getName());

    private final ArchipelagoClient archipelagoClient;

    private final Gson gson = new Gson();

    private boolean authenticated = false;

    private int reconnectAttempt = 0;

    private JsonElement slotData;

    private String seedName;
    private static Timer reconnectTimer;
    private boolean downgrade = false;

    public ArchipelagoWebSocket(URI serverUri, ArchipelagoClient archipelagoClient) {
        super(serverUri);
        this.archipelagoClient = archipelagoClient;
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
        }
        reconnectTimer = new Timer();
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
    }

    @Override
    public void onMessage(String message) {
        try {
            LOGGER.finest(() -> "Got Packet: " + message);
            JsonElement element = JsonParser.parseString(message);

            JsonArray cmdList = element.getAsJsonArray();

            for (int i = 0; cmdList.size() > i; ++i) {
                JsonElement packet = cmdList.get(i);
                //parse the packet first to see what command has been sent.
                APPacket cmd = gson.fromJson(packet, APPacket.class);


                //check if room info packet
                LOGGER.fine(String.format("Received %s packet", cmd.getCmd()));
                switch (cmd.getCmd()) {
                    case RoomInfo:
                        RoomInfoPacket roomInfo = gson.fromJson(packet, RoomInfoPacket.class);

                        //save room info
                        archipelagoClient.setRoomInfo(roomInfo);

                        checkDataPackage(roomInfo.datapackageVersions);

                        seedName = roomInfo.seedName;

                        ConnectPacket connectPacket = new ConnectPacket();
                        connectPacket.version = ArchipelagoClient.protocolVersion;
                        connectPacket.name = archipelagoClient.getMyName();
                        connectPacket.password = (archipelagoClient.getPassword() == null) ? "" : archipelagoClient.getPassword();
                        connectPacket.uuid = archipelagoClient.getUUID();
                        connectPacket.game = archipelagoClient.getGame();
                        connectPacket.tags = archipelagoClient.getTags();
                        connectPacket.itemsHandling = archipelagoClient.getItemsHandlingFlags();

                        //send reply
                        sendPacket(connectPacket);
                        archipelagoClient.setRoomInfo(roomInfo);
                        break;
                    case Connected:
                        ConnectedPacket connectedPacket = gson.fromJson(packet, ConnectedPacket.class);

                        archipelagoClient.setTeam(connectedPacket.team);
                        archipelagoClient.setSlot(connectedPacket.slot);

                        archipelagoClient.getRoomInfo().networkPlayers.addAll(connectedPacket.players);
                        archipelagoClient.getRoomInfo().networkPlayers.add(new NetworkPlayer(connectedPacket.team, 0, "Archipelago"));
                        archipelagoClient.setAlias(archipelagoClient.getRoomInfo().getPlayer(connectedPacket.team, connectedPacket.slot).alias);

                        ConnectionAttemptEvent attemptConnectionEvent = new ConnectionAttemptEvent(connectedPacket.team, connectedPacket.slot, seedName, slotData);
                        archipelagoClient.getEventManager().callEvent(attemptConnectionEvent);

                        if (!attemptConnectionEvent.isCanceled()) {
                            authenticated = true;
                            //only send locations if the connection is not canceled.
                            archipelagoClient.getLocationManager().addCheckedLocations(connectedPacket.checkedLocations);
                            archipelagoClient.getLocationManager().setMissingLocations(connectedPacket.missingLocations);
                            archipelagoClient.getLocationManager().sendIfChecked(connectedPacket.missingLocations);

                            ConnectionResultEvent connectionResultEvent = new ConnectionResultEvent(ConnectionResult.Success, connectedPacket.team, connectedPacket.slot, seedName, slotData);
                            archipelagoClient.getEventManager().callEvent(connectionResultEvent);
                            slotData = packet.getAsJsonObject().get("slot_data");
                            archipelagoClient.onSlotData(slotData);
                        } else {
                            this.close();
                            //close out of this loop because we are no longer interested in further commands from the server.
                            break;
                        }
                        break;
                    case ConnectionRefused:
                        ConnectionRefusedPacket error = gson.fromJson(cmdList.get(i), ConnectionRefusedPacket.class);
                        archipelagoClient.getEventManager().callEvent(new ConnectionResultEvent(error.errors[0]));
                        break;
                    case Print:
                        archipelagoClient.onPrint(gson.fromJson(packet, PrintPacket.class).getText());
                        break;
                    case DataPackage:
                        JsonElement data = packet.getAsJsonObject().get("data");
                        DataPackage dataPackage = gson.fromJson(data, DataPackage.class);
                        dataPackage.uuid = archipelagoClient.getUUID();
                        archipelagoClient.updateDataPackage(dataPackage);
                        if (dataPackage.getVersion() != 0) {
                            archipelagoClient.saveDataPackage();
                        }
                        break;
                    case PrintJSON:
                        APPrint print = gson.fromJson(packet, APPrint.class);
                        StringBuilder sb = new StringBuilder();

                        //filter though all player IDs and replace id with alias.
                        for (int p = 0; print.parts.length > p; ++p) {
                            if (print.parts[p].type == APPrintType.PLAYER_ID) {
                                int playerID = Integer.parseInt((print.parts[p].text));
                                NetworkPlayer player = archipelagoClient.getRoomInfo().getPlayer(archipelagoClient.getTeam(), playerID);

                                print.parts[p].text = player.alias;
                            } else if (print.parts[p].type == APPrintType.ITEM_ID) {
                                long itemID = Long.parseLong((print.parts[p].text));
                                print.parts[p].text = archipelagoClient.getDataPackage().getItem(itemID);
                            } else if (print.parts[p].type == APPrintType.LOCATION_ID) {
                                long locationID = Long.parseLong((print.parts[p].text));
                                print.parts[p].text = archipelagoClient.getDataPackage().getLocation(locationID);
                            }
                            sb.append(print.parts[p].text);
                        }
                        LOGGER.fine("PrintJSON: " + sb);
                        archipelagoClient.onPrintJson(print, print.type, print.receiving, print.item);
                        break;
                    case RoomUpdate:
                        RoomUpdatePacket updatePacket = gson.fromJson(packet, RoomUpdatePacket.class);
                        updateRoom(updatePacket);
                        break;
                    case ReceivedItems:
                        ReceivedItemsPacket items = gson.fromJson(packet, ReceivedItemsPacket.class);
                        ItemManager itemManager = archipelagoClient.getItemManager();
                        itemManager.receiveItems(items.items, items.index);
                        archipelagoClient.onReceivedItems();
                        break;
                    case Bounced:
                        BouncedPacket bounced = gson.fromJson(packet, BouncedPacket.class);
                        if (DeathLink.isDeathLink(bounced))
                            DeathLink.receiveDeathLink(bounced);
                        else
                            archipelagoClient.getEventManager().callEvent(new BouncedEvent(bounced.games, bounced.tags, bounced.slots, bounced.data));
                        break;
                    case LocationInfo:
                        LocationInfoPacket locations = gson.fromJson(packet, LocationInfoPacket.class);
                        for (NetworkItem item : locations.locations) {
                            item.itemName = archipelagoClient.getDataPackage().getItem(item.itemID);
                            item.locationName = archipelagoClient.getDataPackage().getLocation(item.locationID);
                            item.playerName = archipelagoClient.getRoomInfo().getPlayer(archipelagoClient.getTeam(), item.playerID).alias;
                        }
                        archipelagoClient.getEventManager().callEvent(new LocationInfoEvent(locations.locations));
                        break;
                    case Retrieved:
                        RetrievedPacket retrievedPacket = gson.fromJson(packet, RetrievedPacket.class);
                        archipelagoClient.getEventManager().callEvent(new RetrievedEvent(retrievedPacket.keys, packet.getAsJsonObject().get("keys").getAsJsonObject(), retrievedPacket.requestID));
                        break;
                    case SetReply:
                        SetReplyPacket setReplyPacket = gson.fromJson(packet, SetReplyPacket.class);
                        archipelagoClient.getEventManager().callEvent(new SetReplyEvent(setReplyPacket.key, setReplyPacket.value, setReplyPacket.original_Value, packet.getAsJsonObject().get("value"), setReplyPacket.requestID));
                        break;
                    case InvalidPacket:
                        InvalidPacket invalidPacket = gson.fromJson(packet, InvalidPacket.class);
                        archipelagoClient.getEventManager().callEvent(new InvalidPacketEvent(invalidPacket.type, invalidPacket.Original_cmd, invalidPacket.text));
                        LOGGER.log(Level.INFO, "Invalid packet received: " + packet);
                    default:

                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error proccessing incoming packet: ");
            e.printStackTrace();
        }
    }

    private void updateRoom(RoomUpdatePacket updateRoomPacket) {
        if (updateRoomPacket.networkPlayers.size() != 0) {
            archipelagoClient.getRoomInfo().networkPlayers = updateRoomPacket.networkPlayers;
            archipelagoClient.getRoomInfo().networkPlayers.add(new NetworkPlayer(archipelagoClient.getTeam(), 0, "Archipelago"));
        }

        checkDataPackage(updateRoomPacket.datapackageVersions);

        archipelagoClient.setHintPoints(updateRoomPacket.hintPoints);
        archipelagoClient.setAlias(archipelagoClient.getRoomInfo().getPlayer(archipelagoClient.getTeam(), archipelagoClient.getSlot()).alias);

        archipelagoClient.getEventManager().callEvent(new CheckedLocationsEvent(updateRoomPacket.checkedLocations));
        archipelagoClient.onLocationsUpdate();
    }

    private void checkDataPackage(Map<String, Integer> versions) {
        HashSet<String> exclusions = new HashSet<>();
        for (Map.Entry<String, Integer> game : versions.entrySet()) {
            //the game does NOT need updating add it to the exclusion list.
            int myGameVersion = archipelagoClient.getDataPackage().getVersions().getOrDefault(game.getKey(), 0);
            int newGameVersion = game.getValue();
            if (newGameVersion <= myGameVersion && newGameVersion != 0) {
                exclusions.add(game.getKey());
            }
        }

        if (exclusions.size() != versions.size()) {
            fetchDataPackageFromAP(exclusions);
        }
    }

    private void fetchDataPackageFromAP(Set<String> exclusions) {
        sendPacket(new GetDataPackagePacket(exclusions));
    }

    public void sendPacket(APPacket packet) {
        sendManyPackets(new APPacket[]{packet});
    }

    private void sendManyPackets(APPacket[] packet) {
        if (!isOpen())
            return;
        String json = gson.toJson(packet);
        LOGGER.fine("Sent Packet: " + json);
        send(json);
    }

    @Override
    public void onClose(int code, String wsReason, boolean remote) {
        LOGGER.log(Level.WARNING, String.format("Connection closed by %s Code: %s Reason: %s", (remote ? "remote peer" : "us"), code, wsReason));
        String reason = (wsReason.isEmpty()) ? "Connection refused by the Archipelago server." : wsReason;
        if (code == -1) {
            reconnectTimer.cancel();

            // attempt to reconnect using non-secure web socket if we are failing to connect with a secure socket.
            if (uri.getScheme().equalsIgnoreCase("wss") && downgrade) {
                try {
                    archipelagoClient.connect(new URIBuilder(uri).setScheme("ws").build());
                } catch (URISyntaxException ignored) {
                    archipelagoClient.onClose(reason, 0);
                }
                return;
            }
            archipelagoClient.onClose(reason, 0);
            return;
        }
        if (code == 1000) {
            reconnectTimer.cancel();
            archipelagoClient.onClose("Disconnected.", 0);
        }

        if (code == 1006) {
            reason = "Lost connection to the Archipelago server.";
            if (reconnectAttempt <= 10) {
                int reconnectDelay = (int) (5000 * Math.pow(2, reconnectAttempt));
                reconnectAttempt++;
                TimerTask reconnectTask = new TimerTask() {
                    @Override
                    public void run() {
                        archipelagoClient.reconnect();
                    }
                };

                reconnectTimer.cancel();
                reconnectTimer = new Timer();
                reconnectTimer.schedule(reconnectTask, reconnectDelay);
                archipelagoClient.onClose(reason, reconnectDelay / 1000);
                return;
            }
        }

        reconnectTimer.cancel();
        archipelagoClient.onClose(reason, 0);
    }

    @Override
    public void onError(Exception ex) {
        if (ex instanceof SSLException) return;
        archipelagoClient.onError(ex);
        LOGGER.log(Level.SEVERE, "Error in websocket connection", ex);
        ex.printStackTrace();
    }

    public void connect(boolean allowDowngrade) {
        super.connect();
        reconnectTimer.cancel();
        reconnectAttempt = 0;
        this.downgrade = allowDowngrade;
    }

    public void sendChat(String message) {
        SayPacket say = new SayPacket(message);
        sendPacket(say);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void scoutLocation(ArrayList<Long> locationIDs) {
        LocationScouts packet = new LocationScouts(locationIDs);
        sendPacket(packet);
    }

    public JsonElement getSlotData() {
        return slotData;
    }
}
