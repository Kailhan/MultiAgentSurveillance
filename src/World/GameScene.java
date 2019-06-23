package World;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

import java.awt.*;
import javafx.geometry.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import Agent.*;
import Agent.Routine;


import static Agent.Agent.*;
import static World.StartWorldBuilder.WINDOW_SIZE;

/**
 * Main in game screen
 * @author Kailhan
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
    private ArrayList<TileView> tileViews = new ArrayList<>();

    private boolean gameStarted; //used for start and stop button
    private int mode; //modes for different gameModes e.g. multiple intruders/guards and what the end game conditions are
    public static final int ASSUMED_WORLDSIZE = 100;
    public static final double SCALING_FACTOR = WINDOW_SIZE/ASSUMED_WORLDSIZE; //ASSUMING WORLD IS ALWAYS 100 X 100 AND THAT WINDOWSIZE IS 1000
    public static Random random = new Random();
    private long currentTimeCountDown;
    private boolean countDown;
    private boolean visitedTarget;
    private long firstVisitTime;

    public GameScene(Stage primaryStage, Settings settings) {
        this.grid = new GridPane(); //main grid that shows the tiles
        this.windowSize = WINDOW_SIZE;
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
        this.gameStarted = false;

        initTileImgArray();
        initGoToMenuButton();
        initTiles();
//        initRestartButton();

        this.startGameBut = new Button("Start/Stop Game"); //should stop and start game, not properly working atm
        Agent.worldMap = worldMap;
        Guard guard  = new Guard(new Point2D(200, 300), 70);
        Intruder intruder = new Intruder(new Point2D(500, 500), 0);
        AreaOptimizer areaOptimzer = new AreaOptimizer(new Point2D(500, 400), 0);
//        worldMap.addAgent(guard);
//        worldMap.addAgent(intruder);
        //worldMap.addOnlyAgent(guard);
        worldMap.addOnlyAgent(intruder);
        //worldMap.addOnlyAgent(areaOptimzer);
        //worldMap.addOnlyAgent(areaOptimzer);
        //Actual game "loop" in here
        startGameBut.setOnAction(e -> { //
            currentTimeCountDown = System.nanoTime();
            if(!gameStarted) {
                gameStarted = true;
//                worldMap.startAgents();
                System.out.println("Started agents");
                new AnimationTimer() {
                    long currentTimeCalc = System.nanoTime();
                    long previousTime = currentTimeCalc;
                    @Override
                    public void handle(long currentTime) {
                        if(gameStarted){
//                        long beforeUpdatingAgents = System.nanoTime();
                            worldMap.forceUpdateAgents();
//                        long afterUpdatingAgents = System.nanoTime();
//                        System.out.println("updating agentstook: " + ((afterUpdatingAgents-beforeUpdatingAgents)/1e9));

//                        long beforeDrawingBoard = System.nanoTime();
                            redrawBoard();
//                        long afterDrawingBoard = System.nanoTime();
//                        System.out.println("redrawing board took: " + ((afterDrawingBoard-beforeDrawingBoard)/1e9));


                            long delta = (currentTime - previousTime);
//                        System.out.println("drawing tick in: " + (delta/1e9));
                            previousTime = currentTime;
                            generateRandomSound(delta);
                            haveGuardsCapturedIntruder(mode, delta);
                            haveIntrudersWon(mode, delta);
//                        System.out.println();
                        }
                    }
                }.start();
            } else {
                gameStarted = false;
                worldMap.removeAllAgents();
                initRedrawBoard();
            }
        });

        this.startGameBut.setWrapText(true);

        initRedrawBoard(); //redrawing board otherwise window that displays board and button is not properly sized
        initFullScreen();
    }

    public void run(){initRedrawBoard();
    }

    public void initRedrawBoard(){
        grid.getChildren().clear();
        createTiles();
    }

    /**
     * Updates tiles and general information displayed in the actual game screen
     */
    public void redrawBoard() {
        grid.getChildren().clear();
        createTiles();
        createAgents();
        drawCones();
//        drawTileShapes();
        agentGroup.toFront();
    }

    public void drawCones() {
        worldMap.createCones();
        agentGroup.getChildren().addAll(worldMap.getAgentsCones());
    }

    public void drawTileShapes() {
        worldMap.createWorldGridShapes();
        agentGroup.getChildren().addAll(worldMap.getWorldGridShapes());
    }

    public void initTiles() {
        for (int r = 0; r < worldMap.getSize(); r++) {
            for (int c = 0; c < worldMap.getSize(); c++) {
                TileView tmpView = new TileView(tileImgArray[worldMap.getTileState(r, c)], r, c, worldMap.getTileState(r, c));
                tileViews.add(c + (r * worldMap.getSize()), tmpView);
                grid.add(tmpView, c, r);tileViews.set(c + (r * worldMap.getSize()),  new TileView(tileImgArray[worldMap.getTileState(r, c)], r, c, worldMap.getTileState(r, c)));
            }
        }
    }

    public void createTiles() {
        for (int r = 0; r < worldMap.getSize(); r++) {
            for (int c = 0; c < worldMap.getSize(); c++) {
                if(tileViews.get(c + (r * worldMap.getSize())).getState() != worldMap.getTileState(r, c)) {
//                    tileViews.set(c + (r * worldMap.getSize()),  new TileView(tileImgArray[worldMap.getTileState(r, c)], r, c, worldMap.getTileState(r, c)));
                }
                TileView tmpView = new TileView(tileImgArray[worldMap.getTileState(r, c)], r, c, worldMap.getTileState(r, c));
                tileViews.set(c + (r * worldMap.getSize()), tmpView);
                grid.add(tmpView, c, r);
            }
        }
    }

//   he intruder wins if he is 3 seconds in any of the target areas or vists the target area twice with a time
//   difference of at least 3 seconds. The guards win if the intruder is no more than 0.5 meter away and in sight.
//   All intruders need to complete their objective or any of them.
//   If an intruder flees through the entry point before making the target it is a draw

    public void haveIntrudersWon(int mode, long delta) {
        boolean intrudersWon = false;
        if(!countDown) {
            currentTimeCountDown = System.nanoTime();
        }
        if(worldMap.intruderInTarget()) {
            if(!visitedTarget) {
                firstVisitTime = System.nanoTime();
                visitedTarget = true;
            }
            if((System.nanoTime() - currentTimeCountDown) > (3*1e9)) {
                intrudersWon = true;
            }
            countDown = true;
        } else {
            countDown = false;
        }
        if(visitedTarget && (System.nanoTime() - firstVisitTime) > (3*1e9)) {
            intrudersWon = true;
        }
        if(intrudersWon) {
            createAlert("INTRUDER has reached TARGET");
        }
    }

    /**
     * Checks if guard are in range to "capture" intruder and if so they have won the game, multiple modes need to be added
     * e.g. if "all" intruders need to be caught or only 1
     */
    public void haveGuardsCapturedIntruder(int mode, long delta) {
        Agent[] agentGuards = worldMap.getAgents().toArray(new Agent[worldMap.getAgents().size()]);
        Agent[] agentIntruders = worldMap.getAgents().toArray(new Agent[worldMap.getAgents().size()]);
        for(Agent agentGuard : agentGuards) {
            if(agentGuard instanceof Guard) {
                for(Agent agentIntruder : agentIntruders) {
                    if(agentIntruder instanceof Intruder) {
                        if(agentGuard.getPosition().distance(agentIntruder.getPosition()) < (DISTANCE_TO_CATCH * SCALING_FACTOR)) {
                            createAlert("GUARDS have found INTRUDER");
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
        occurenceRate *= (ASSUMED_WORLDSIZE/25); //map is ASSUMED_WORLDSIZE so ASSUMED_WORLDSIZE/25 times as big as 25
        if(random.nextDouble() < occurenceRate/(delta)) {
            Point2D randomNoiseLocation = new Point2D(random.nextInt(windowSize), random.nextInt(windowSize));
            for(Agent agent : worldMap.getAgents()) {
                if(randomNoiseLocation.distance(agent.getPosition())/SCALING_FACTOR < 5) {
                    double angleBetweenPoints = Math.toDegrees(Math.atan2((agent.getPosition().getY() - randomNoiseLocation.getY()), (agent.getPosition().getX() - randomNoiseLocation.getX())));
                    angleBetweenPoints += new Random().nextGaussian()*SOUND_NOISE_STDEV;
                    agent.getAudioLogs().add(new AudioLog(System.nanoTime(), angleBetweenPoints, new Point2D(agent.getPosition().getX(), agent.getPosition().getY())));
                    System.out.println("Agent heard sound");
                }
            }
        }
    }

    private void createAlert(String s) {
        gameStarted = false;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Finished");
        alert.setHeaderText(null);
        alert.setContentText(s);
        alert.show();
        goToMenuBut.fire();
    }

    /**
     * Actually add AgentCircles to list which is drawn
     */
    public void createAgents() {
        agentGroup.getChildren().clear();
//        AgentCircle circleTmp = new AgentCircle(new Point2D(1000, 1000));
//        circleTmp.setFill(Color.CORNFLOWERBLUE);
//        agentGroup.getChildren().add(circleTmp);
//        agentGroup.toFront();
        for(Agent agent : worldMap.getAgents()) {
            AgentCircle circle = new AgentCircle(agent);
            agentGroup.getChildren().add(circle);
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
            worldMap.removeAllAgents();
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

    public Scene getGameScene() {
        return scene;
    }
}
