package World;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import Agent.*;
import javafx.stage.WindowEvent;

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

    private List<TileButton> toAdd = new ArrayList<>();

    private Image emptyTileImg;
    private Image structureTileImg;
    private Image doorTileImg;
    private Image windowTileImg;
    private Image targetTileImg;
    private Image sentryTileImg;
    private Image decreasedVisRangeTileImg;
    private Image wallTileImg;
    private Image[] tileImgArray;

    private Button goToMenuBut;
    private Button restartGameBut;
    private Button startGameBut;

    private Group agentGroup = new Group();

    public GameScene(Stage primaryStage, Settings settings) {
        this.grid = new GridPane();
        this.windowSize = 1000;
        this.settings = settings;
        this.primaryStage = primaryStage;
        this.primaryStage.setOnCloseRequest(we -> {
            System.out.println("Stage is closing");
            worldMap.removeAllAgents();
            System.exit(0);
        });
        this.worldMap = new WorldMap(settings.getWorldMap());
        this.tileSize = windowSize / worldMap.getSize();

        this.emptyTileImg = new Image(new File("src/Assets/emptyTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        this.structureTileImg = new Image(new File("src/Assets/structureTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        this.doorTileImg = new Image(new File("src/Assets/doorTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        this.windowTileImg = new Image(new File("src/Assets/windowTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        this.targetTileImg = new Image(new File("src/Assets/targetTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        this.sentryTileImg = new Image(new File("src/Assets/sentryTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        this.decreasedVisRangeTileImg = new Image(new File("src/Assets/decreasedVisRangeTile.png").toURI().toString(), tileSize, tileSize, false, false, true);
        this.wallTileImg = new Image(new File("src/Assets/wallTile16.png").toURI().toString(), tileSize, tileSize, false, false, true);
        this.tileImgArray = new Image[]{emptyTileImg, structureTileImg, doorTileImg, windowTileImg, targetTileImg, sentryTileImg, decreasedVisRangeTileImg, wallTileImg};

        this.goToMenuBut = new Button("Menu");
        goToMenuBut.setOnAction(e -> { // Switch to settings
            SettingsScene settingsScene = new SettingsScene(primaryStage);
            Node source = (Node) e.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();
            this.primaryStage = new Stage();
            this.primaryStage.setTitle("Multi-Agent Surveillance Settings");
            this.primaryStage.setScene(settingsScene.getSettingsScene());
            this.primaryStage.show();
        });
        this.goToMenuBut.setWrapText(true);

        this.restartGameBut = new Button("Restart Game");
        restartGameBut.setOnAction(e -> { // Create a new game with the same setings
            GameScene gameScene = new GameScene(primaryStage, settings);
            Node source = (Node) e.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();
            this.primaryStage = new Stage();
            this.primaryStage.setTitle("Multi-Agent-Surveillance Game");
            this.primaryStage.setScene(gameScene.getGameScene());
            this.primaryStage.show();
        });
        this.restartGameBut.setWrapText(true);

        this.startGameBut = new Button("Start Game");
        startGameBut.setOnAction(e -> { //

        });
        this.startGameBut.setWrapText(true);

        redrawBoard();
        grid.setGridLinesVisible(true);
        grid.setAlignment(Pos.CENTER);

        VBox vBox = new VBox();
        VBox.setVgrow(goToMenuBut, Priority.ALWAYS);
        VBox.setVgrow(restartGameBut, Priority.ALWAYS);
        VBox.setVgrow(startGameBut, Priority.ALWAYS);
        goToMenuBut.setMaxHeight(Double.MAX_VALUE);
        restartGameBut.setMaxHeight(Double.MAX_VALUE);
        startGameBut.setMaxHeight(Double.MAX_VALUE);
        goToMenuBut.setMaxWidth(Double.MAX_VALUE);
        restartGameBut.setMaxWidth(Double.MAX_VALUE);
        startGameBut.setMaxWidth(Double.MAX_VALUE);
        vBox.getChildren().addAll(goToMenuBut, restartGameBut, startGameBut);

        hBox = new HBox();
        StackPane worldPane = new StackPane();
        worldPane.getChildren().addAll(grid, agentGroup);
        hBox.getChildren().addAll(worldPane, vBox); //can directly create scene from grid if borderpane layout is not gonna be used
        scene = new Scene(hBox);
        hBox.setMinSize(windowSize + windowSize * 0.1, windowSize);

        Agent.worldMap = worldMap;
        worldMap.addAgent(new Intruder(new Point2D.Double(-200, -50), 0));
        worldMap.startAgents();
        System.out.println("Started agents");
        new AnimationTimer() {
            private long previousTime = 0;
            private float secondsElapsedSinceLastFpsUpdate = 0f;
            private int framesSinceLastFpsUpdate = 0;

            @Override
            public void handle(long currentTime)
            {
//                if (previousTime == 0) {
//                    previousTime = currentTime;
//                    return;
//                }
//
//                float secondsElapsed = (currentTime - previousTime) / 1e9f;
//                //float secondsElapsedCapped = Math.min(secondsElapsed, getMaximumStep());
//                previousTime = currentTime;
//
//                //updater.accept(secondsElapsedCapped);
                redrawBoard();
//                System.out.println("board is redrawn");
//
//                secondsElapsedSinceLastFpsUpdate += secondsElapsed;
//                framesSinceLastFpsUpdate++;
//                if (secondsElapsedSinceLastFpsUpdate >= 0.5f) {
//                    //int fps = Math.round(framesSinceLastFpsUpdate / secondsElapsedSinceLastFpsUpdate);
//                    //fpsReporter.accept(fps);
//                    secondsElapsedSinceLastFpsUpdate = 0;
//                    framesSinceLastFpsUpdate = 0;
//                }
//                System.out.println("handled");
            }
        }.start();
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
//        grid.getChildren().addAll(agentGroup);
        grid.setGridLinesVisible(true);
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

    public void createAgents() {
        agentGroup.getChildren().clear();
        for(Agent agent : worldMap.getAgents()) {
            if(agent instanceof Guard) {
                Guard guard = (Guard) agent;
                AgentCircle circle = new AgentCircle(guard.getPosition());
                circle.setFill(Color.PEACHPUFF);
                agentGroup.getChildren().add(new Pane(circle));
            }
            if(agent instanceof Intruder) {
                Intruder intruder = (Intruder) agent;
                AgentCircle circle = new AgentCircle(intruder.getPosition());
                circle.setFill(Color.DARKRED);
                Pane tmpPane = new Pane();
                tmpPane.getChildren().addAll(circle);
                agentGroup.getChildren().add(tmpPane);
            }
            //System.out.println("proceeding after while loop, agent on seperate thread");
        }
    }

    public void updateWorldMap(int r, int c, int state) {
        worldMap.updateTile(r, c, state);
        redrawBoard();
    }

    public Scene getGameScene() {
        return scene;
    }
}