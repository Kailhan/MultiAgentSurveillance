package World;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.Random;
import Agent.*;

import static Agent.Agent.SOUND_NOISE_STDEV;
import static Agent.Agent.angleBetweenTwoPointsWithFixedPoint;

/**
 * Main in game screen
 * @author Kailhan Hokstam
 */
public class GameScene extends BorderPane implements Runnable {

    private Settings settings;
    private Stage primaryStage;
    private HBox hBox;
    private Scene scene;
    private WorldMap worldMap;
    private GridPane grid;
    private int windowSize;
    private int tileSize;
    private Image[] tileImgArray;
    private Button goToMenuBut;
    private Button restartGameBut;
    private Button startGameBut;
    private Group agentGroup = new Group();

    private boolean gameStarted = false; //used for start and stop button
    private int mode; //modes for different gameModes e.g. multiple intruders/guards and what the end game conditions are
    public static final double SCALING_FACTOR = 1000/200; //ASSUMING WORLD IS ALWAYS 200 X 200 WHICH MEANS THAT IF WE HAVE A SMALLER MAP IN WORLDBUILDER THE INDIVIDUAL TILES ARE "BIGGER" AND THAT WINDOWSIZE IS 1000
    public static Random random = new Random();

    public GameScene(Stage primaryStage, Settings settings) {
        this.grid = new GridPane(); //main grid that shows the tiles
        this.windowSize = StartWorldBuilder.WINDOW_SIZE;
        this.mode = 0;
        this.settings = settings;
        this.primaryStage = primaryStage;
        this.primaryStage.setOnCloseRequest(we -> { //doesnt work properly right now, used to actually close threads when closing main window
            System.out.println("Stage is closing");
            worldMap.removeAllAgents();
            System.exit(0);
        });
        this.worldMap = new WorldMap(settings.getWorldMap()); //create world data structure
        this.tileSize = windowSize / worldMap.getSize();

        initTileImgArray();
        initGoToMenuButton();
//        initRestartButton();

        this.startGameBut = new Button("Start/Stop Game"); //should stop and start game, not properly working atm
        Agent.worldMap = worldMap;
        worldMap.addAgent(new Intruder(new Point2D.Double(50, 50), 270));
        worldMap.addAgent(new Intruder(new Point2D.Double(500, 600), 180));

        //Actual game "loop" in here
        startGameBut.setOnAction(e -> { //
            if(!gameStarted) {
                gameStarted = true;
                worldMap.startAgents();
                System.out.println("Started agents");
                new AnimationTimer() {
                    long currentTimeCalc = System.nanoTime();
                    long previousTime = currentTimeCalc;
                    @Override
                    public void handle(long currentTime) {
                        redrawBoard();
                        long delta = (currentTime - previousTime);
                        previousTime = currentTime;
                        generateRandomSound(delta);
                        haveGuardsCapturedIntruder(mode, delta);
                        haveIntrudersWon(mode, delta);
                    }
                }.start();
            } else {
                gameStarted = false;
                worldMap.removeAllAgents();
                redrawBoard();
            }
        });
        this.startGameBut.setWrapText(true);

        redrawBoard(); //redrawing board otherwise window that displays board and button is not properly sized
        initFullScreen();
    }

    public void haveIntrudersWon(int mode, long delta) {

//        boolean countDown = false;
//        boolean gameWon = false;
//        long currentTimeCountDown = System.nanoTime();
        if(worldMap.intruderInTarget()) {
            gameStarted = false;
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Finished");
            alert.setHeaderText(null);
            alert.setContentText("INTRUDER has reached TARGET");
            alert.showAndWait();
            goToMenuBut.fire();
//            countDown = true;
//            if(countDown) {
//                if((System.nanoTime() - currentTimeCountDown)/1e9 < 3) gameWon = true;
//            } else {
//                currentTimeCountDown = System.nanoTime();
//            }
//            if(gameWon) {
//                gameStarted = false;
//                Alert alert = new Alert(Alert.AlertType.INFORMATION);
//                alert.setTitle("Game Finished");
//                alert.setHeaderText(null);
//                alert.setContentText("INTRUDER has reached TARGET");
//                alert.showAndWait();
//                goToMenuBut.fire();
//            }
        }
    }

    /**
     * Checks if guard are in range to "capture" intruder and if so they have won the game, multiple modes need to be added
     * e.g. if "all" intruders need to be caught or only 1
     */
    public void haveGuardsCapturedIntruder(int mode, long delta) {
        for(Agent agentGuard : worldMap.getAgents()) {
            if(agentGuard instanceof Guard) {
                for(Agent agentIntruder : worldMap.getAgents()) {
                    if(agentIntruder instanceof Intruder) {
                        if(agentGuard.getPosition().distance(agentIntruder.getPosition()) < (0.5 * SCALING_FACTOR)) {
                            gameStarted = false;
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Game Finished");
                            alert.setHeaderText(null);
                            alert.setContentText("GUARDS have found INTRUDER");
                            alert.showAndWait();
                            goToMenuBut.fire();
                        }
                    }
                }
            }
        }
    }

    /**
     * Random sound according to sort of poisson process (more binomial with low probability which should approximate it probs&stat stuff
     */
    public void generateRandomSound(long delta){
        double occurenceRate = 0.1/1e9; //because delta is in nano seconds
        occurenceRate *= 8; //map is 200 so 8 times as big as 25
        if(random.nextDouble() < occurenceRate/(delta)) {
            Point2D.Double randomNoiseLocation = new Point2D.Double(random.nextInt(windowSize), random.nextInt(windowSize));
            for(Agent agent : worldMap.getAgents()) {
                if(randomNoiseLocation.distance(agent.getPosition())/SCALING_FACTOR < 5) {
                    Point2D tmpPoint = agent.getMove(1000, agent.getDirection());
                    double angleBetweenPoints = angleBetweenTwoPointsWithFixedPoint(tmpPoint.getX(), tmpPoint.getY(), agent.getPosition().getX(), agent.getPosition().getY(), randomNoiseLocation.getX(), randomNoiseLocation.getY());
                    angleBetweenPoints += new Random().nextGaussian()*SOUND_NOISE_STDEV;
                    agent.getAudioLogs().add(new AudioLog(System.nanoTime(), angleBetweenPoints, new Point2D.Double(agent.getPosition().getX(), agent.getPosition().getY())));
                    System.out.println("Agent heard sound");
                }
            }
        }
    }

    /**
     * Actually add AgentCircles to list which is drawn
     */
    public void createAgents() {
        agentGroup.getChildren().clear();
//        AgentCircle circleTmp = new AgentCircle(new Point2D.Double(1000, 1000));
//        circleTmp.setFill(Color.CORNFLOWERBLUE);
//        agentGroup.getChildren().add(circleTmp);
//        agentGroup.toFront();
        for(Agent agent : worldMap.getAgents()) {
            if(agent instanceof Guard) {
                Guard guard = (Guard) agent;
                AgentCircle circle = new AgentCircle(guard.getPosition());
                circle.setFill(Color.PEACHPUFF);
//                Pane tmpPane = new Pane();
//                tmpPane.getChildren().addAll(circle);
//                agentGroup.getChildren().add(tmpPane);
                agentGroup.getChildren().add(circle);
            }
            if(agent instanceof Intruder) {
                Intruder intruder = (Intruder) agent;
                AgentCircle circle = new AgentCircle(intruder.getPosition());
                circle.setFill(Color.DARKRED);
//                Pane tmpPane = new Pane();
//                tmpPane.getChildren().addAll(circle);
//                agentGroup.getChildren().add(tmpPane);
                agentGroup.getChildren().add(circle);
//                System.out.println("position in create agents: " + intruder.getPosition().toString());
            }
            agentGroup.toFront();
            //System.out.println("proceeding after while loop, agent on seperate thread");
        }
    }

    /**
     * Sets (size) and creates all containers and combines them
     */
    public void initFullScreen() {
        //setting box that contains buttons
        VBox vBox = new VBox();
        VBox.setVgrow(goToMenuBut, Priority.ALWAYS);
        //VBox.setVgrow(restartGameBut, Priority.ALWAYS);
        VBox.setVgrow(startGameBut, Priority.ALWAYS);
        goToMenuBut.setMaxHeight(Double.MAX_VALUE);
        //restartGameBut.setMaxHeight(Double.MAX_VALUE);
        startGameBut.setMaxHeight(Double.MAX_VALUE);
        goToMenuBut.setMaxWidth(Double.MAX_VALUE);
        //restartGameBut.setMaxWidth(Double.MAX_VALUE);
        startGameBut.setMaxWidth(Double.MAX_VALUE);
//        vBox.getChildren().addAll(goToMenuBut, restartGameBut, startGameBut);
        vBox.getChildren().addAll(goToMenuBut, startGameBut);


        hBox = new HBox(); //container that stores the box containing buttons and the stackpane with the world and the agents drawn ontop of it
        Pane worldPane = new Pane(); //allows us to draw agents on top of world
        worldPane.getChildren().addAll(grid, agentGroup);
        hBox.getChildren().addAll(worldPane, vBox);
        scene = new Scene(hBox); // allows us to actually display the world, agents and buttons
        hBox.setMinSize(windowSize + windowSize * 0.1, windowSize); //dont think this is done properly but it helps with sizing
        //hBox.setMinSize(windowSize, windowSize); //dont think this is done properly but it helps with sizing

    }

    /**
     * Loads all images for the world and stores them in an array
     */
    public void initTileImgArray(){
        Image emptyTileImg = new Image(new File("src/Assets/emptyTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        Image structureTileImg = new Image(new File("src/Assets/structureTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        Image doorTileImg = new Image(new File("src/Assets/doorTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        Image windowTileImg = new Image(new File("src/Assets/windowTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        Image targetTileImg = new Image(new File("src/Assets/targetTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        Image sentryTileImg = new Image(new File("src/Assets/sentryTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        Image decreasedVisRangeTileImg = new Image(new File("src/Assets/decreasedVisRangeTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        Image wallTileImg = new Image(new File("src/Assets/wallTile16.png").toURI().toString(), tileSize, tileSize, false, false, true);
        this.tileImgArray = new Image[]{emptyTileImg, structureTileImg, doorTileImg, windowTileImg, targetTileImg, sentryTileImg, decreasedVisRangeTileImg, wallTileImg};
    }

    /**
     * Inits button for going back to main menu where you can pick to start a world builder again, load custom map and simulate/play
     */
    public void initGoToMenuButton() {
        this.goToMenuBut = new Button("Menu");
        goToMenuBut.setOnAction(e -> { // Switch to settings
            SettingsScene settingsScene = new SettingsScene(primaryStage);
            this.primaryStage.setTitle("Multi-Agent Surveillance Settings");
            this.primaryStage.setScene(settingsScene.getSettingsScene());
            this.primaryStage.show();
        });
        this.goToMenuBut.setWrapText(true);
    }

    /**
     * Inits button for restarting World Builder with the same settings
     */
    public void initRestartButton(){
        this.restartGameBut = new Button("Restart Game");
        restartGameBut.setOnAction(e -> { // Create a new game with the same setings
            WorldBuilder worldBuilder = new WorldBuilder(primaryStage, settings);
            this.primaryStage.setTitle("Multi-Agent-Surveillance Game");
            this.primaryStage.setScene(worldBuilder.getWorldBuilder());
            this.primaryStage.show();
        });
        this.restartGameBut.setWrapText(true);
    }

    public void run(){
     redrawBoard();
    }

    /**
     * Updates tiles and general information displayed in the actual game screen
     */
    public void redrawBoard() {
        grid.getChildren().clear();
        createTiles();
        createAgents();
        agentGroup.toFront();
    }

    public void createTiles() {
        for (int r = 0; r < worldMap.getSize(); r++) {
            for (int c = 0; c < worldMap.getSize(); c++) {
                //System.out.println("r" + r + "c" + c);
                ImageView tmpImage = new ImageView(tileImgArray[worldMap.getTileState(r, c)]);
                tmpImage.setSmooth(false);
                grid.add((tmpImage), r, c);
            }
        }
    }

    public Scene getGameScene() {
        return scene;
    }
}