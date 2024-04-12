package moe.maika.fmaptracker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Security;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import com.google.gson.JsonElement;

import gg.archipelago.client.ArchipelagoClient;
import gg.archipelago.client.Print.APPrint;
import gg.archipelago.client.parts.NetworkItem;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import net.lingala.zip4j.ZipFile;


public class TrackerController {

    private final BlockingQueue<WorkUnit> workQueue;
    public final Logger log;

    @FXML
    private Label topLabel;
    @FXML
    private Label labelImpl;
    @FXML
    private Button connectButton;
    @FXML
    private ComboBox<Farm> duelistBox;
    @FXML
    private TableView<Drop> dropTable;
    @FXML
    private TableColumn<Drop, String> cardNameColumn;
    @FXML
    private TableColumn<Drop, String> duelRankColumn;
    @FXML
    private TableColumn<Drop, Integer> probabilityColumn;
    @FXML
    private TableColumn<Drop, String> inLogicColumn;
    @FXML
    private GridPane farmPane;

    private FarmController saPowFarm, bcdFarm, saTecFarm;
    private final Image[] duelistImages;

    private ConnectionStatusLabel connectionLabel;

    private Stage connectModalStage;
    private Stage primaryStage;

    private static final String DEFAULT_AP_INSTALL_LOCATION = "C:\\ProgramData\\Archipelago";
    private static final String SETTINGS_FOLDER = ".FMAPTracker";
    private static final String FM_AP_WORLD_FILENAME = "fm.apworld";
    private static final String FM_AP_WORLD_RELEASES_URL = "https://github.com/sg4e/Archipelago/releases";
    private static final String TRACKER_RELEASES_URL = "https://github.com/sg4e/FMArchipelagoTracker/releases";
    private static final String PROPERTIES_SERVER_LITERAL = "ServerAndPort";
    private static final String PROPERTIES_PLAYER_LITERAL = "PlayerName";
    private static final long SLEEP_BETWEEN_UI_UPDATE_MILLIS = 500L;
    private static final long COALESCE_WORK_UNIT_SLEEP_MILLIS = 100L;

    private Future<Context> pythonInitializer;
    private final ExecutorService executor;
    private final DirectoryChooser directoryChooser;
    private ConnectInfo connectInfo = null;
    private HostServices hostServices;
    private final FarmChangeListener farmChangeListener;

    public TrackerController() {
        workQueue = new LinkedBlockingQueue<>(1);
        log = initLogger();
        log.info(String.format("Tracker version: %s", Version.getAsString()));
        executor = Executors.newSingleThreadExecutor();
        directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Archipelago Data folder");
        duelistImages = new Image[40];
        farmChangeListener = new FarmChangeListener();
    }

    @FXML
    private void initialize() {
        connectionLabel = new ConnectionStatusLabel(labelImpl);
        cardNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().cardName()));
        duelRankColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().duelRank()));
        probabilityColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().probability()).asObject());
        inLogicColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().inLogic() ? "Yes" : "No"));
        duelistBox.setConverter(new StringConverter<Farm>() {
            @Override
            public String toString(Farm object) {
                if(object == null) {
                    return null;
                }
                return String.format("%s %s: %.2f%% (Missing: %s Total: %s)",
                        object.duelist().name(),
                        object.duelRank(),
                        object.totalProbability() / 2048d * 100d,
                        object.missingDrops(),
                        object.totalDrops());
            }

            @Override
            public Farm fromString(String string) {
                // Implement this method if needed (e.g., for two-way binding)
                return null;
            }
        });
        try {
            for(int i = 0; i < 40; i++) {
                duelistImages[i] = new Image(getClass().getResourceAsStream(String.format("duelists/%s.png", i)));
            }
            saPowFarm = makeFarmUi("SA POW", 0);
            bcdFarm = makeFarmUi("BCD", 1);
            saTecFarm = makeFarmUi("SA TEC", 2);
        }
        catch(IOException e) {
            log.log(Level.SEVERE, "Failed to make farm UI", e);
        }
        duelistBox.getSelectionModel().selectedItemProperty().addListener(farmChangeListener);
        probabilityColumn.setSortType(TableColumn.SortType.DESCENDING);
        dropTable.getSortOrder().add(probabilityColumn);
    }

    private FarmController makeFarmUi(String label, int index) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("farm.fxml"));
        farmPane.add(loader.load(), index, 0);
        FarmController controller = loader.getController();
        controller.setTrackerController(this);
        controller.setDuelRank(label);
        controller.setDuelistImage(duelistImages[0]); // build deck image
        return controller;
    }

    private class FarmChangeListener implements ChangeListener<Farm> {

        Map<Farm, List<Drop>> farms;

        public void setFarms(Map<Farm, List<Drop>> farms) {
            this.farms = farms;
        }

        @Override
        public void changed(ObservableValue<? extends Farm> obs, Farm oldVal, Farm newVal) {
            if(newVal == null) {
                log.warning("Farm was null when setting selection model");
            }
            else {
                dropTable.getItems().setAll(farms.get(newVal));
                dropTable.sort();
            }
        }

    }

    private void updateFarms(Map<Farm, List<Drop>> farms, List<Farm> sortedFarmList, Map<String, List<Farm>> topFarmsForDuelRank) {
        // Find ComboBox selected Farm in the updated sortedFarmList (same Duelist and Duel Rank)
        Farm selectedFarm = getPreviouslySelectedFarm(sortedFarmList);
        saPowFarm.updateTopFarms(topFarmsForDuelRank.getOrDefault("SAPOW", Collections.emptyList()), duelistImages);
        bcdFarm.updateTopFarms(topFarmsForDuelRank.getOrDefault("BCD",  Collections.emptyList()), duelistImages);
        saTecFarm.updateTopFarms(topFarmsForDuelRank.getOrDefault("SATEC",  Collections.emptyList()), duelistImages);
        duelistBox.getItems().setAll(sortedFarmList);
        farmChangeListener.setFarms(farms);
        duelistBox.setDisable(false);
        if(selectedFarm != null)
            duelistBox.getSelectionModel().select(selectedFarm);
    }

    private Farm getPreviouslySelectedFarm(List<Farm> sortedFarmList) {
        Farm selectedInput = duelistBox.getValue();
        if(selectedInput != null) {
            return sortedFarmList.stream().filter(
                    farm -> farm.duelist().equals(selectedInput.duelist()) && farm.duelRank().equals(selectedInput.duelRank())
            ).findFirst().orElse(null);
        }
        return null;
    }

    public void setSelectedFarm(Farm selected) {
        duelistBox.getSelectionModel().select(selected);
    }

    public Image getIconForApplication() {
        //images are loaded by this controller, so it's easier to query the controller for it
        return duelistImages[0];
    }

    public void setConnectInfo(ConnectInfo connectInfo) {
        this.connectInfo = connectInfo;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public void loadFMWorld() {
        Path settingsPath = getSettingsDirectory();
        Properties settings = loadSettingsProperties();
        String apProgramDataLocation = settings.getProperty("APProgramData", DEFAULT_AP_INSTALL_LOCATION);
        if(!isAPProgramData(apProgramDataLocation)) {
            showAlertDialog("Could not find your Archipelago Program Data folder. Please locate it.", "No Archipelago install", AlertType.ERROR);
            File selectedFile = directoryChooser.showDialog(primaryStage);
            if(selectedFile == null || !isAPProgramData(selectedFile.getAbsolutePath())) {
                showAlertDialog("No Archipelago detected in that folder. Please relaunch the tracker and try again.", "No Archipelago install", AlertType.ERROR);
                System.exit(0);
            }
            apProgramDataLocation = selectedFile.getAbsolutePath();
        }
        settings.setProperty("APProgramData", apProgramDataLocation);
        Path fmWorldPath = Paths.get(apProgramDataLocation, "lib", "worlds", FM_AP_WORLD_FILENAME);
        if(!fmWorldPath.toFile().exists()) {
            showAlertDialog(String.format("There is no %s in your Archipelago installation. Please download a compatible version from " + 
                            "the official Releases page and place it inside \"lib/worlds\" in your Archipelago installation, then relaunch the tracker:", FM_AP_WORLD_FILENAME), 
                    String.format("No %s in AP installation", FM_AP_WORLD_FILENAME),
                    AlertType.ERROR,
                    FM_AP_WORLD_RELEASES_URL,
                    FM_AP_WORLD_RELEASES_URL);
            System.exit(0);
        }
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        final Properties settingsForSubthread = settings;
        executor.submit(() -> {
            // check if apworld and the extracted version have the same checksum
            boolean copyFilesFromAPWorld = true;
            String fmWorldSha256String = null;
            String propertiesShaKey = "apworldsha256";
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(Files.readAllBytes(fmWorldPath));
                byte[] sha256Bytes = md.digest();
                StringBuilder sb = new StringBuilder();
                for(byte b : sha256Bytes) {
                    sb.append(String.format("%02x", b));
                }
                fmWorldSha256String = sb.toString();
                String lastApWorldSha = settingsForSubthread.getProperty(propertiesShaKey);
                if(fmWorldSha256String == null || fmWorldSha256String.isBlank())
                    throw new RuntimeException("SHA256 is blank");
                if(fmWorldSha256String.equals(lastApWorldSha)) {
                    copyFilesFromAPWorld = false;
                    log.log(Level.INFO, "Current and previous AP world checksums match. Skipping file extraction.");
                }
                else {
                    copyFilesFromAPWorld = true;
                    log.log(Level.INFO, "Checksum mismatch. Extracting new apworld for logic.");
                }
            }
            catch(Exception e) {
                log.log(Level.WARNING, "Error reading APWorld file and comparing checksums. APWorld will be re-validated", e);
            }
            if(copyFilesFromAPWorld) {
                //look for the FM APWorld file
                try {
                    Path tempPythonPath = Paths.get(settingsPath.toString(), "newPython");
                    if(Files.exists(tempPythonPath)) {
                        deleteDirectory(tempPythonPath.toFile());
                    }
                    Path validatedPythonPath = Paths.get(settingsPath.toString(), "python");
                    if(Files.exists(validatedPythonPath)) {
                        deleteDirectory(validatedPythonPath.toFile());
                    }
                    Files.createDirectories(tempPythonPath);
                    try(var zip = new ZipFile(fmWorldPath.toString())) {
                        zip.extractAll(tempPythonPath.toString());
                    }
                    InputStream keyIn = TrackerController.this.getClass().getResourceAsStream("sg4e_public_key.pem");
                    PGPPublicKeyRingCollection pgpPubRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator());
                    boolean verified = false;
                    try {
                        //non-recursive
                        verified = Files.walk(tempPythonPath)
                                .filter(f -> Files.isRegularFile(f) && !f.getFileName().toString().endsWith(".sig"))
                                .map(p -> {
                                    log.info("Checking signature for " + p);
                                    Path sigFile = p.resolveSibling(p.getFileName().toString() + ".sig");
                                try {
                                    if(!verifySignature(p.toAbsolutePath().toString(), sigFile.toAbsolutePath().toString(), pgpPubRingCollection)) {
                                        log.log(Level.SEVERE, "Failed to verify signature for " + p);
                                        return false;
                                    }
                                }
                                catch(Exception e) {
                                    log.log(Level.SEVERE, "Failed to verify signature for " + p, e);
                                    return false;
                                }
                                try {
                                    // preprocess python file
                                    preprocessPythonFile(p);
                                }
                                catch(Exception e) {
                                    log.log(Level.SEVERE, "Failed to preprocess python file", e);
                                    return false;
                                }
                                return true;
                                }).allMatch(Boolean::booleanValue);
                    }
                    catch(Exception e) {
                        log.log(Level.SEVERE, "Failed to verify signatures", e);
                        showFailedSignatureNotifAndExit();
                    }
                    if(verified) {
                        Files.move(tempPythonPath, validatedPythonPath);
                    }
                    else {
                        showFailedSignatureNotifAndExit();
                    }
                }
                catch(Exception e) {
                    log.log(Level.SEVERE, "Failed to extract FM world", e);
                    Platform.runLater(() -> {
                        showAlertDialog("Failed to extract FM world", "Error", AlertType.ERROR);
                        System.exit(0);
                    });
                }
            }
            settingsForSubthread.setProperty(propertiesShaKey, fmWorldSha256String);
            storeSettingsProperties(settingsForSubthread);
            // init graal python
            pythonInitializer = executor.submit(() -> {
                Context ctx = Context.newBuilder()
                .allowIO(IOAccess.newBuilder()
                .allowHostFileAccess(true).build())
                .allowHostAccess(HostAccess.newBuilder(HostAccess.EXPLICIT).allowListAccess(true).build())
                .option("python.PythonPath", Paths.get(settingsPath.toAbsolutePath().toString(), "python", "fm").toAbsolutePath().toString())
                .build();
                InputStreamReader versionCheckReader = new InputStreamReader(TrackerController.this.getClass().getResourceAsStream("version-check.py"));
                Source versionCheckSource = Source.newBuilder("python", versionCheckReader, "version-check.py").build();
                ctx.eval(versionCheckSource);
                String worldVersion = ctx.eval("python", "get_apworld_version()").asString();
                log.info(String.format("apworld version: %s", worldVersion));
                String topLabelUpdate = " | FM APWorld v" + worldVersion;
                boolean versionCompatible = Version.isCompatible(worldVersion);
                Platform.runLater(() -> {
                    topLabel.setText(topLabel.getText() + topLabelUpdate);
                    if(!versionCompatible) {
                        showAlertDialog(String.format("This tracker is not compatible with the installed Forbidden Memories apworld.\n\n" +
                                "Tracker version: %s\n" +
                                "apworld version: v%s\n\n" +
                                "The major version (leftmost number of the version) must match between the tracker and the apworld to ensure compatibility. " +
                                "Please download the latest %s.x.x release from the tracker's releases page to use this apworld.",
                                Version.getAsString(), worldVersion, Version.getMajorFromString(worldVersion)),
                                "Tracker not compatible with apworld version",
                                AlertType.ERROR,
                                TRACKER_RELEASES_URL,
                                TRACKER_RELEASES_URL);
                        System.exit(0);
                    }
                });
                InputStreamReader reader = new InputStreamReader(TrackerController.this.getClass().getResourceAsStream("java-wrapper.py"));
                Source src = Source.newBuilder("python", reader, "java-wrapper.py").build();
                ctx.eval(src);
                return ctx;
            });
            Platform.runLater(() -> {
                connectButton.setDisable(false);
                connectButton.requestFocus();
            });
        });
    }

    private static void preprocessPythonFile(Path pythonFile) throws IOException {
        List<String> lines = Files.readAllLines(pythonFile);
        List<String> processed = lines.stream()
                .map(line -> line.replaceAll("from \\.(\\w+) import", "from $1 import"))
                .toList();
        Files.write(pythonFile, processed);
    }

    private void showFailedSignatureNotifAndExit() {
        log.log(Level.SEVERE, "Signature failure");
        Platform.runLater(() -> {
            showAlertDialog("The Forbidden Memories apworld file in your Archipelago installation has been tampered with. " +
                "You did not download this apworld file from the official releases page and are potentially exposing yourself " +
                "to security vulnerabilities.\n\napworlds contain executable files, and when placed inside the Archipelago installation, " +
                "they may even gain full administrative access to your computer when you run Archipelago. Only install apworlds from " +
                "sources you trust.\n\nThe developer of this tracker is also responsible for making official releases of the Forbidden " +
                "Memories apworld. Every apworld release contains a non-forgeable signature from the developer. This tracker has confirmed " +
                "that the signature is missing in the currently installed apworld. For your protection, this tracker will not import " +
                "the apworld and will exit. You are greatly encouraged to delete the currently installed FM apworld and download the " +
                "latest version from the official releases page:",
                "Signature Validation Failure", AlertType.ERROR, FM_AP_WORLD_RELEASES_URL, FM_AP_WORLD_RELEASES_URL);
            System.exit(0);
        });
    }

    /**
    * Verify the signature in in against the file fileName.
    *
    * Taken nearly verbatim from the Bouncy Castle PGP examples:
    * https://github.com/bcgit/bc-java/blob/main/pg/src/main/java/org/bouncycastle/openpgp/examples/DetachedSignatureProcessor.java
    */
    private static boolean verifySignature(
        String          fileName,
        String       sigFileName,
        PGPPublicKeyRingCollection pgpPubRingCollection)
        throws GeneralSecurityException, IOException, PGPException {
        InputStream in = PGPUtil.getDecoderStream(new BufferedInputStream(new FileInputStream(sigFileName)));
        JcaPGPObjectFactory    pgpFact = new JcaPGPObjectFactory(in);
        PGPSignatureList    p3;
        Object    o = pgpFact.nextObject();
        if(o instanceof PGPCompressedData) {
            PGPCompressedData             c1 = (PGPCompressedData)o;
            pgpFact = new JcaPGPObjectFactory(c1.getDataStream());
            p3 = (PGPSignatureList)pgpFact.nextObject();
        }
        else {
            p3 = (PGPSignatureList)o;
        }
        InputStream                 dIn = new BufferedInputStream(new FileInputStream(fileName));

        PGPSignature                sig = p3.get(0);
        PGPPublicKey                key = pgpPubRingCollection.getPublicKey(sig.getKeyID());

        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), key);

        int ch;
        while ((ch = dIn.read()) >= 0) {
            sig.update((byte)ch);
        }
        dIn.close();
        in.close();
        return sig.verify();
    }

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private boolean isAPProgramData(String path) {
        try {
            Path worldFolder = Paths.get(path, "lib", "worlds");
            return Files.exists(worldFolder) && Files.isDirectory(worldFolder);
        }
        catch(Exception e) {
            return false;
        }
    }

    private Properties loadSettingsProperties() {
        Path settingsPath = getSettingsDirectory();
        Properties settings;
        try(var settingsStream = Files.newInputStream(Paths.get(settingsPath.toString(), "settings.properties"))) {
            settings = new Properties();
            settings.load(settingsStream);
        }
        catch(IOException e) {
            log.log(Level.WARNING, "Failed to load settings", e);
            settings = new Properties();
        }

        return settings;
    }

    private void storeSettingsProperties(final Properties settings) {
        Path settingsPath = getSettingsDirectory();
        try(var settingsWriter = Files.newBufferedWriter(Paths.get(settingsPath.toString(), "settings.properties"))) {
            settings.store(settingsWriter, "Forbidden Memories AP Tracker settings");
        }
        catch(IOException e) {
            log.log(Level.WARNING, "Failed to save settings", e);
        }
    }

    private Path getSettingsDirectory() {
        String userHome = System.getProperty("user.home");
        Path settingsPath = Paths.get(userHome, SETTINGS_FOLDER);
        if(!Files.exists(settingsPath)) {
            try {
                Files.createDirectories(settingsPath);
            }
            catch(IOException e) {
                log.log(Level.SEVERE, "Failed to create settings directory", e);
            }
        }
        return settingsPath;
    }

    public Logger initLogger() {
        // Create a Logger instance
        Logger logger = Logger.getLogger(TrackerController.class.getName());

        // Set global logging level to DEBUG
        logger.setLevel(Level.FINE);

        // Create a console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL); // Set console handler level
        consoleHandler.setFormatter(new SimpleFormatter()); // Set formatter

        // Add console handler to the root logger
        logger.addHandler(consoleHandler);

        // Create a file handler
        try {
            new File("FMAPTrackerData").mkdirs();
            FileHandler fileHandler = new FileHandler("FMAPTrackerData/Tracker.log", 5 * 1024 * 1024, 1, true);
            fileHandler.setLevel(Level.INFO); // Set file handler level
            fileHandler.setFormatter(new SimpleFormatter()); // Set formatter
            // Add file handler to the root logger
            logger.addHandler(fileHandler);
        }
        catch(IOException e) {
            logger.log(Level.SEVERE, "Failed to create file logger", e);
        }

        return logger;
    }

    public void loadStoredConnectInfo(ConnectController connectController) {
        Properties settings = loadSettingsProperties();
        String serverAndPort = settings.getProperty(PROPERTIES_SERVER_LITERAL, "");
        String playerName = settings.getProperty(PROPERTIES_PLAYER_LITERAL, "");

        connectController.setStoredFields(serverAndPort, playerName);
    }

    @FXML
    private void onConnectButtonPressed() {
        if (connectInfo != null) {
            connect(connectInfo.serverAndPort(), connectInfo.player(), connectInfo.password());
        }
        else {
            connectModalStage.show();
        }
    }

    void connect(String url, String slotName, String password) {
        storeConnectInfo(url, slotName);
        connectModalStage.close();
        connectButton.setDisable(true);
        connectionLabel.setStatus(ConnectionStatusLabel.Status.CONNECTING);
        new ConnectionTask(url, slotName, password).start();
    }

    private void storeConnectInfo(String serverAndPort, String playerName) {
        Properties settings = loadSettingsProperties();
        settings.setProperty(PROPERTIES_SERVER_LITERAL, serverAndPort);
        settings.setProperty(PROPERTIES_PLAYER_LITERAL, playerName);

        storeSettingsProperties(settings);
    }

    void cancelConnectModal() {
        connectModalStage.close();
    }

    void setConnectModalStage(Stage connectModalStage) {
        this.connectModalStage = connectModalStage;
    }

    private class ConnectionTask extends Thread {

        private final String url;
        private final String slotName;
        private final String passworld;

        public ConnectionTask(String url, String slotName, String password) {
            super("ConnectionTask");
            setDaemon(true);
            this.url = url;
            this.slotName = slotName;
            this.passworld = password;
        }

        @Override
        public void run() {
            ApClient client = new ApClient();
            try {
                client.setName(slotName);
                //client.setGame(Constants.GAME_NAME);
                if(passworld != null && !passworld.isBlank()) {
                    client.setPassword(passworld);
                }
                client.addTag("Tracker");
                client.connect(url);

                // success
                Platform.runLater(() -> connectionLabel.setStatus(ConnectionStatusLabel.Status.AWAITING_SLOT_DATA));
            }
            catch(Exception e) {
                log.log(Level.SEVERE, "Failed to connect to Archipelago", e);
                Platform.runLater( () -> {
                    showAlertDialog("Failed to connect to Archipelago", "Connection Error", AlertType.ERROR);
                    connectionLabel.setStatus(ConnectionStatusLabel.Status.DISCONNECTED);
                    connectButton.setDisable(false);
                });
            }
        }

    }

    private void showAlertDialog(String message, String title, AlertType type) {
        showAlertDialog(message, title, type, null, null);
    }

    private void showAlertDialog(String message, String title, AlertType type, String hyperlinkText, String hyperlinkLink) {
        // JOptionPane.showMessageDialog(TrackerController.this, message, title, JOptionPane.ERROR_MESSAGE);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if(hyperlinkText == null || "".equals(hyperlinkText.trim())) {
            alert.setContentText(message);
        }
        else {
            // append a hyperlink at the end of the alert
            Hyperlink hyperlink = new Hyperlink(hyperlinkText);
            hyperlink.setOnAction(event -> hostServices.showDocument(hyperlinkLink));

            Label label = new Label(message);
            label.setWrapText(true);
            VBox vbox = new VBox(label, hyperlink);
            vbox.setSpacing(10);
            vbox.setPrefWidth(300);
            alert.getDialogPane().setContent(vbox);
        }
        alert.showAndWait();
    }

    private class ApClient extends ArchipelagoClient {

        @Override
        public void onPrint(String print) {
            // do nothing
        }

        @Override
        public void onPrintJson(APPrint apPrint, String type, int sending, NetworkItem item) {
            if(item != null && item.playerID == getSlot()) {
                getLocationManager().markAsChecked(item.locationID);
                updateTracker();
            }
        }

        @Override
        public void onError(Exception ex) {
            log.log(Level.SEVERE, "Error in Archipelago connection", ex);
        }

        @Override
        public void onClose(String reason, int attemptingReconnect) {
            log.log(Level.WARNING, "Archipelago connection closed: " + reason);
            Platform.runLater(() -> {
                connectionLabel.setStatus(ConnectionStatusLabel.Status.DISCONNECTED);
                // I've found that the AP Java client doesn't dispose of disconnected clients properly
                // so it's better to have the user relaunch the application than re-enable the "Connect" button
                showAlertDialog("Connection to the AP server has been closed. " + 
                        "Please close the tracker and relaunch to reconnect.", "Disconnected", AlertType.ERROR);
            });
        }

        @Override
        public void onSlotData(JsonElement data) {
            Platform.runLater(() -> connectionLabel.setStatus(ConnectionStatusLabel.Status.CONNECTED));
            TrackerUpdater trackerUpdater = new TrackerUpdater(data.toString());
            updateTracker();
            trackerUpdater.start();
        }

        @Override
        public void onReceivedItems() {
            updateTracker();
        }

        @Override
        public void onLocationsUpdate() {
            updateTracker();
        }

        public void updateTracker() {
            // the websocket thread isn't well isolated
            // it's important to limit processing in these callback methods
            // otherwise, the websocket times out due to not responding to pings
            WorkUnit wu = new WorkUnit(getLocationManager().getMissingLocations(), getItemManager().getReceivedItemIDs());
            if(!workQueue.offer(wu)) {
                workQueue.poll();
                workQueue.offer(wu);
            }
        }

    }

    private static class WorkUnit {
        private final Instant timestamp;
        private final Set<Long> missingLocationIds;
        private final List<Long> receivedItemIds;

        public WorkUnit(Collection<Long> missingLocationIds, Collection<Long> receivedItemIds) {
            this.timestamp = Instant.now();
            this.missingLocationIds = new HashSet<>(missingLocationIds);
            this.receivedItemIds = new ArrayList<>(receivedItemIds);
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public Set<Long> getMissingLocationIds() {
            return missingLocationIds;
        }

        public List<Long> getReceivedItemIds() {
            return receivedItemIds;
        }
    }

    public static record Duelist(int id, String name) {}

    private static record Drop(String cardName, String duelRank, int probability, boolean inLogic) {}

    public static record Farm(Duelist duelist, String duelRank, int totalProbability, int missingDrops, int totalDrops) {}

    private static record Pool(Duelist duelist, String duelRank) {}

    private class TrackerUpdater extends Thread {

        private final String slotData;
        private Context ctx;
        private volatile boolean cancelled;
        private final Map<Pool, Integer> poolToTotalCardCount;

        public TrackerUpdater(String slotData) {
            super("TrackerUpdater");
            setDaemon(true);
            this.slotData = slotData;
            cancelled = false;
            poolToTotalCardCount = new HashMap<>();
        }

        public void cancel() {
            cancelled = true;
        }

        @Override
        public void run() {
            if(!pythonInitializer.isDone()) {
                log.log(Level.WARNING, "Connection established and Python VM is still not initialized. Waiting...");
            }
            try {
                // pythonInitializer is never null because it is set before the Connect button is enabled
                ctx = pythonInitializer.get();
                // at this point, the executor is no longer needed
                executor.shutdown();
                ctx.getBindings("python").putMember("slot_data", slotData);
                ctx.eval("python", "load_slot_data(slot_data)");
                // (is_equal, generator_version, installed_apworld_version)
                Value versionCheck = ctx.eval("python", "check_client_and_generator_versions_match()");
                if(!versionCheck.getArrayElement(0).asBoolean()) {
                    String generatorVersion = versionCheck.getArrayElement(1).asString();
                    String installedApworldVersion = versionCheck.getArrayElement(2).asString();
                    log.log(Level.WARNING, String.format("Mismatched generator version (%s) and installed apworld version (%s)",
                            generatorVersion, installedApworldVersion));
                    Platform.runLater(() -> showAlertDialog(String.format("The apworld version that was used to generate this multiworld is different from the apworld version you have installed. " + 
                            "For the best compatibility, please install the same apworld version from the Releases page.\n\nGenerated with apworld: %s\nInstalled apworld: %s",
                            generatorVersion, installedApworldVersion), 
                            "apworld version mismatch",
                            AlertType.WARNING,
                            FM_AP_WORLD_RELEASES_URL,
                            FM_AP_WORLD_RELEASES_URL));
                }
                ctx.eval("python", "initialize()");
                // typing.Dict[typing.Tuple[Duelist, str], int]: (duelist, duel_rank) -> total_card_locations
                Map<Value, Integer> poolCardsDictValue = ctx.eval("python", "get_total_cards_per_farm()")
                        .as(new TypeLiteral<Map<Value, Integer>>() {});
                for(Map.Entry<Value, Integer> entry : poolCardsDictValue.entrySet()) {
                    Value duelistValue = entry.getKey().getArrayElement(0);
                    Duelist duelist = new Duelist(duelistValue.getMember("id").asInt(), duelistValue.getMember("_name").asString());
                    poolToTotalCardCount.put(new Pool(duelist, entry.getKey().getArrayElement(1).asString()), entry.getValue());
                }
            }
            catch(Exception e) {
                log.log(Level.SEVERE, "Error initializing Python VM", e);
                Platform.runLater(() -> {
                    showAlertDialog("Failed to initialize Python VM. Please make sure you have the latest APWorld file.",
                            "Error", AlertType.ERROR);
                    System.exit(0);
                });
            }
            while(!cancelled) {
                try {
                    WorkUnit wu;
                    do {
                        WorkUnit localWu = workQueue.take();
                        log.fine(() -> String.format("Processing server message from %s: %s missing locations and %s items collected",
                                localWu.getTimestamp(), localWu.getMissingLocationIds().size(), localWu.getReceivedItemIds().size()));
                        sleep(COALESCE_WORK_UNIT_SLEEP_MILLIS);
                        wu = localWu;
                    } while(workQueue.peek() != null);
                    ctx.getBindings("python").putMember("items_received_ids", wu.getReceivedItemIds());
                    ctx.getBindings("python").putMember("missing_locations_ids", wu.getMissingLocationIds());
                    Value value = ctx.eval("python", "get_tracker_info(items_received_ids, missing_locations_ids)");
                    Map<Value, List<Value>> trackerInfo = value.as(new TypeLiteral<Map<Value, List<Value>>>() {});
                    Map<Duelist, List<Drop>> dropMap = new HashMap<>();
                    for(Map.Entry<Value, List<Value>> entry : trackerInfo.entrySet()) {
                        Duelist duelist = new Duelist(entry.getKey().getArrayElement(0).asInt(), entry.getKey().getArrayElement(1).asString());
                        for(Value drop : entry.getValue()) {
                            dropMap.computeIfAbsent(duelist, k -> new ArrayList<>()).add(new Drop(drop.getArrayElement(0).asString(), drop.getArrayElement(1).asString(), drop.getArrayElement(2).asInt(), drop.getArrayElement(3).asBoolean()));
                        }
                    }
                    Map<Farm, List<Drop>> farms = new HashMap<>();
                    for(Map.Entry<Duelist, List<Drop>> entry : dropMap.entrySet()) {
                        Duelist duelist = entry.getKey();
                        Map<String, List<Drop>> duelRankToDrops = entry.getValue().stream().collect(
                                Collectors.groupingBy(Drop::duelRank));
                        for(Map.Entry<String, List<Drop>> duelRankEntry : duelRankToDrops.entrySet()) {
                            Pool pool = new Pool(duelist, duelRankEntry.getKey());
                            int totalCardCount = poolToTotalCardCount.get(pool);
                            Farm farm = new Farm(duelist,
                                    duelRankEntry.getKey(),
                                    duelRankEntry.getValue().stream().mapToInt(Drop::probability).sum(),
                                    (int)duelRankEntry.getValue().stream().count(),
                                    totalCardCount);
                            farms.put(farm, duelRankEntry.getValue());
                        }
                    }
                    Map<String, List<Farm>> topFarmsForDuelRank = farms.keySet().stream().collect(
                            Collectors.groupingBy(Farm::duelRank));
                    for(Map.Entry<String, List<Farm>> entry : topFarmsForDuelRank.entrySet()) {
                        entry.getValue().sort((f1, f2) -> f2.totalProbability() - f1.totalProbability());
                    }
                    List<Farm> sortedFarmList = new ArrayList<>(farms.keySet());
                    sortedFarmList.sort((f1, f2) -> f2.totalProbability() - f1.totalProbability());
                    Platform.runLater(() -> updateFarms(farms, sortedFarmList, topFarmsForDuelRank));
                    sleep(SLEEP_BETWEEN_UI_UPDATE_MILLIS);
                }
                catch(Exception e) {
                    log.log(Level.WARNING, "Error in tracker updater", e);
                }
            }

        }
    }

}
