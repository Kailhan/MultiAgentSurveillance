package Agent;
import World.WorldMap;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.awt.Point;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static World.GameScene.ASSUMED_WORLDSIZE;
import static World.GameScene.SCALING_FACTOR;
import static World.WorldMap.*;

/**
 * This is the superclass of Intruder and Guard, which contains methods for actions
 * @author Benjamin, Kailhan Hokstam
 */

public class Agent implements Runnable {
    public static final double WALK_SPEED_SLOW = 0; //speed <0.5m/s
    public static final double WALK_SPEED_MEDIUM = 0.5; //speed >0.5 & <1 m/s
    public static final double WALK_SPEED_MEDIUMFAST = 1; //speed >1 & 2 m/s
    public static final double WALK_SPEED_FAST = 2; //speed >2m/s
    public static final double SOUNDRANGE_CLOSE = 1; //distance 1m
    public static final double SOUNDRANGE_MEDIUM = 3; //distance 3m
    public static final double SOUNDRANGE_MEDIUMFAR = 5; //distance 5m
    public static final double SOUNDRANGE_FAR = 10;  //distance 10m
    public static final double SOUND_NOISE_STDEV =  10;  //stndard dev of normal distributed noise
    public static final int AMOUNT_OF_VISION_TENTACLES = 100;
    public static final int TENTACLE_INCREMENTS = 1000;
    public static final double MAX_TURNING_PER_SECOND = 45;

    protected volatile Point2D position;
    protected double direction;
    protected int[][] knownTerrain = new int[worldMap.getSize()][worldMap.getSize()];
    protected List<AudioLog> audioLogs = new ArrayList<AudioLog>();
    protected volatile double xGoal;
    protected volatile double yGoal;
    protected double convertedDistance;
    protected double currentSpeed;
    protected Color color;

    protected Shape viewingCone;
    protected double viewingAngle;
    protected double[] visualRange;

    public static WorldMap worldMap;
    protected double currentTime;
    protected double delta;
    protected boolean exitThread;
    protected double previousTime;

    protected Point2D previousPosition;
    protected volatile Point goalPosition;

    protected boolean firstRun;

    protected boolean turning;
    protected boolean sprinting;
    protected double startingAngle;
    protected double endAngle;
    private boolean goalSet = false;


    /**
     * Constructor for Agent
     * @param position is a point containing the coordinates of an Agent
     * @param direction is the angle which the agent is facing, this spans from -180 to 180 degrees
     */

    public Agent(Point2D position, double direction) {
        System.out.println("agent constructor called");
        this.position = position;
        this.direction = direction;
        for(int i = 0; i < knownTerrain[0].length; i++)
        {
            for(int j = 0; j < knownTerrain.length; j++)
            {
                if(worldMap.worldGrid[i][j] == 4)
                {
                    goalPosition = new Point(j, i);
                    goalSet = true;
                }
            }
        }
        if(goalSet == false)
        {
            System.out.println("No Target");
        }
        //this.goalPosition = position;
        this.visualRange = new double[2];
        this.firstRun = true;
        for (int i = 0; i < knownTerrain.length; i++) {
            for (int j = 0; j < knownTerrain[0].length;j++) {
                knownTerrain[i][j] = 8;
            }
        }
    }

    public void run()
    {
    }

    public void forceUpdate() {
        if(firstRun) {
            previousTime = System.nanoTime();
            previousPosition = new Point2D(position.getX(), position.getY());
            firstRun = false;
        }
        executeAgentLogic();
    }

    public void executeAgentLogic()
    {
    }

    /**
     * to update the direction which an agent is facing
     * @param turningAngle is the turningAngle which the agent will turn, positive for turning to the right, negative for turning to the left
     */

    public void turn(double turningAngle) {
        direction = direction+turningAngle;
        //if(direction > 180) {
        //    direction = (direction-180)-180;
        //} else if(direction < 180) {
        //    direction = (direction+180)+180;
        //}
        while (direction > 360 || direction < 0)
        {
            if (direction > 360)
            {
                direction = direction-360;
            } else if (direction < 0)
            {
                direction = direction+360;
            }
            //System.out.println("direction: " + direction);
        }
    }

    public void updateDirection(double directionToGo) {
        if(!sprinting) {
            double maxTurn = MAX_TURNING_PER_SECOND * delta;
            double toTurn = Math.abs(directionToGo - direction);
            double turn = Math.min(maxTurn, toTurn);
            if(directionToGo > direction) {
                direction += turn;
            } else {
                direction -= turn;
            }
        } else {
            System.out.println("logic for turning while sprinting needs to be implemented rip");
        }

    }

    /**
     * find out where an Agent will end up after a potential move
     * @param distance is the distance to be moved by the agent
     *                 depending on the time-step, speeds will need to be divided before being used as parameter
     * @return a point at which the Agent would end up if this move were made
     */

    public Point2D getMove(double distance, double facingDirection) {
        //double xEnd = position.x + (distance * Math.cos(Math.toRadians(facingDirection)));
        //double yEnd = position.y + (distance * Math.sin(Math.toRadians(facingDirection)));
        //return new Point2D.Double(xEnd, yEnd);
        if (facingDirection >= 0 && facingDirection <= 90)
        {
            double angle = Math.toRadians(direction);
            double newXCoordinate = position.getX()+(distance*Math.sin(angle));
            double newYCoordinate = position.getY()-(distance*Math.cos(angle));
            Point2D newLocation = new Point2D(newXCoordinate, newYCoordinate);
            return newLocation;
        }
        else if (facingDirection >= 90 && facingDirection <= 180)
        {
            double angle = Math.toRadians(180-direction);
            double newXCoordinate = position.getX()+distance*Math.sin(angle);
            double newYCoordinate = position.getY()+distance*Math.cos(angle);
            Point2D newLocation = new Point2D(newXCoordinate, newYCoordinate);
            return newLocation;
        }
        else if (facingDirection >=180 && facingDirection <= 270)
        {
            double angle = Math.toRadians(facingDirection-180);
            double newXCoordinate = position.getX()-distance*Math.sin(angle);
            double newYCoordinate = position.getY()+distance*Math.cos(angle);
            Point2D newLocation = new Point2D(newXCoordinate, newYCoordinate);
            return newLocation;
        }
        else if (facingDirection >= 270 || facingDirection <= 360)
        {
            //System.out.println("4");
            double angle = Math.toRadians(360-facingDirection);
            double newXCoordinate = position.getX()-distance*Math.sin(angle);
            double newYCoordinate = position.getY()-distance*Math.cos(angle);
            Point2D newLocation = new Point2D(newXCoordinate, newYCoordinate);
            return newLocation;
        }
        else
        {
            return position;
        }

    }

    /**
     * moves the agent
     * @param distance is the distance to be moved by the agent
     *                 depending on the time-step, speeds will need to be divided before being used as parameters
     *                 angle is not required here because it is an instance variable of the Agent
     */

    public void move(double distance)
    {
        //System.out.println("direction before move: "+direction);
        //double y = position.getY();
        position = getMove(distance, direction);
        //System.out.println(position.getY()-y);
        //System.out.println("direction after move: "+direction);
        //System.out.println("location: " + this.position.toString() );
    }

    /**
     * checks whether there is an obstacle blocking the desired path
     * @param distance is the distance to be moved by the agent
     *                 depending on the time-step, speeds will need to be divided before being used as parameters
     * @return true if there are no obstacles in the way and false if there are obstacles in the way
     */

    public boolean legalMoveCheck(double distance) {
        Point2D positionToCheck = new Point2D(getMove(distance, direction).getX(), getMove(distance, direction).getY());
        int tileStatus;
        try {
            tileStatus = worldMap.coordinatesToCell(positionToCheck);
        }
        catch(Exception e) {
            System.out.println("Location accessed in array is out of bounds");
            return false;
        }
        if (tileStatus == STRUCTURE || tileStatus == SENTRY || tileStatus == WALL) {
            //System.out.println("detected wall, sentry or structure in legal move check");
            return false;
        } else {
            return true;
        }
    }

//    /**
//     * updates the internal map of an Agent based on their field of vision based on sounds of sightings of other agents or in the case of intruders, sighting of new terrain
//     * @param radius is the distance an Agent can see in front of them
//     * @param angle is the width of view of an Agent
//     */
//    public void updateKnownTerrain(double radius, double angle)
//    {
//        boolean stop = false;
//        int[][] actualTerrain = worldMap.getWorldGrid();
//        //setting search bounds
//        Point corner1 = new Point((int) position.getX(), (int) position.getY());
//        Point2D straight = getMove(radius, direction);
//        Point corner2 = new Point((int) straight.getX(), (int) straight.getY());
//        Point2D left = getMove(radius, direction-(angle/2));
//        Point corner3 = new Point((int) left.getX(), (int) left.getY());
//        Point2D right = getMove(radius, direction+(angle/2));
//        Point corner4 = new Point((int) right.getX(), (int) right.getY());
//        //possible to go < -180 and > 180 here, fix this in the future
//        int XMax = (Math.max((int) Math.max(corner1.getX(), corner2.getX()), (int) Math.max(corner3.getX(), corner4.getX()))/10);
//        int XMin = (Math.min((int) Math.min(corner1.getX(), corner2.getX()), (int) Math.min(corner3.getX(), corner4.getX()))/10);
//        int YMax = (Math.max((int) Math.max(corner1.getY(), corner2.getY()), (int) Math.max(corner3.getY(), corner4.getY()))/10);
//        int YMin = (Math.min((int) Math.min(corner1.getY(), corner2.getY()), (int) Math.min(corner3.getY(), corner4.getY()))/10);
//        if (XMax > 99)
//        {
//            XMax = 99;
//        }
//        if (XMin < 0)
//        {
//            XMin = 0;
//        }
//        if (YMax > 99)
//        {
//            YMax = 99;
//        }
//        if (YMin < 0)
//        {
//            YMin = 0;
//        }
//        //checking if the points (each corner of each square in the bounds) is within the circle of search
//        for (int i = YMin;i <= YMax;i++)
//        {
//            for (int j = XMin;j <= XMax;j++)
//            {
//                if (Math.abs(direction) < 90) //this seems like a wierd condition
//                {
//                    //top left corner
//                    //System.out.println                                                                                                                               (Math.abs(Math.abs(Math.abs(direction)-(180-Math.abs(Math.atan((Math.abs(position.getX()/10-j))/(Math.abs(position.getY()/10-i))))))));
//                    if (Math.sqrt(((position.getX()/10-j)*(position.getX()/10-j))+((position.getY()/10-i)*(position.getY()/10-i))) <= radius && Math.abs(Math.abs(direction)-(Math.abs(Math.toDegrees(Math.atan((Math.abs(position.getX()/10-j))/(Math.abs(position.getY()/10-i))))))) <= angle/2)
//                    {
//                        //System.out.println("1");
//                        knownTerrain[i][j] = worldMap.getTileState(i, j);
//                    }
//                    //top right corner
//                    else if (Math.sqrt(((position.getX()/10-(j+1))*(position.getX()/10-(j+1)))+((position.getY()/10-i)*(position.getY()/10-i))) <= radius && Math.abs(Math.abs(direction)-(Math.abs(Math.toDegrees(Math.atan((Math.abs(position.getX()/10-(j+1)))/(Math.abs(position.getY()/10-i))))))) <= angle/2)
//                    {
//                        //System.out.println("2");
//                        knownTerrain[i][j] = worldMap.getTileState(i, j);
//                    }
//                    //bottom right corner
//                    else if (Math.sqrt(((position.getX()/10-(j+1))*(position.getX()/10-(j+1)))+((position.getY()/10-(i+1))*(position.getY()/10-(i+1)))) <= radius && Math.abs(Math.abs(direction)-(Math.abs(Math.toDegrees(Math.atan((Math.abs(position.getX()/10-(j+1)))/(Math.abs(position.getY()/10-(i+1)))))))) <= angle/2)
//                    {
//                        //System.out.println("3");
//                        knownTerrain[i][j] = worldMap.getTileState(i, j);
//                    }
//                    //bottom left corner
//                    else if (Math.sqrt(((position.getX()/10-j) * (position.getX()/10-j))+((position.getY()/10-(i+1))*(position.getY()/10-(i+1)))) <= radius && Math.abs(Math.abs(direction)-(Math.abs(Math.toDegrees(Math.atan((Math.abs(position.getX()/10-j)) / (Math.abs(position.getY()/10-(i+1)))))))) <= angle/2)
//                    {
//                        //System.out.println("4");
//                        knownTerrain[i][j] = worldMap.getTileState(i, j);
//                    }
//                }
//                else
//                {
//                    //top left corner
//                    if (Math.sqrt(((position.getX() / 10 - j) * (position.getX() / 10 - j)) + ((position.getY() / 10 - i) * (position.getY() / 10 - i))) <= radius && Math.abs(Math.abs(direction) - (180-(Math.abs(Math.toDegrees(Math.atan((Math.abs(position.getX() / 10 - j)) / (Math.abs(position.getY() / 10 - i)))))))) <= angle / 2) {
//                        knownTerrain[i][j] = worldMap.getTileState(i, j);
//                    }
//                    //top right corner
//                    else if (Math.sqrt(((position.getX() / 10 - (j + 1)) * (position.getX() / 10 - (j + 1))) + ((position.getY() / 10 - i) * (position.getY() / 10 - i))) <= radius && Math.abs(Math.abs(direction) - (180-(Math.abs(Math.toDegrees(Math.atan((Math.abs(position.getX() / 10 - (j + 1))) / (Math.abs(position.getY() / 10 - i)))))))) <= angle / 2) {
//                        knownTerrain[i][j] = worldMap.getTileState(i, j);
//                    }
//                    //bottom right corner
//                    else if (Math.sqrt(((position.getX() / 10 - (j + 1)) * (position.getX() / 10 - (j + 1))) + ((position.getY() / 10 - (i + 1)) * (position.getY() / 10 - (i + 1)))) <= radius && Math.abs(Math.abs(direction) - (180-(Math.abs(Math.toDegrees(Math.atan((Math.abs(position.getX() / 10 - (j + 1))) / (Math.abs(position.getY() / 10 - (i + 1))))))))) <= angle / 2) {
//                        knownTerrain[i][j] = worldMap.getTileState(i, j);
//                    }
//                    //bottom left corner
//                    else if (Math.sqrt(((position.getX() / 10 - j) * (position.getX() / 10 - j)) + ((position.getY() / 10 - (i + 1)) * (position.getY() / 10 - (i + 1)))) <= radius && Math.abs(Math.abs(direction) - (180-(Math.abs(Math.toDegrees(Math.atan((Math.abs(position.getX() / 10 - j)) / (Math.abs(position.getY() / 10 - (i + 1))))))))) <= angle / 2) {
//                        knownTerrain[i][j] = worldMap.getTileState(i, j);
//                    }
//                }
//                if (j == 0 || j == 99 || i == 0 || i == 99)
//                {
//                    System.out.println("edge in vision");
//                    stop = true;
//                    break;
//                }
//            }
//            if (stop == true)
//            {
//                break;
//            }
//        }
//        stop = false;
//        //scan through and remove areas hidden by other structures
//        for (int i = YMin;i <= YMax;i++)
//        {
//            for (int j = XMin;j <= XMax;j++)
//            {
//                //System.out.println("i: "+i+" j: "+j+" x coordinate: "+position.getX()+" XMin: "+XMin);
//                if (actualTerrain[i][j] == 1 || actualTerrain[i][j] == 2 || actualTerrain[i][j] == 3 || actualTerrain[i][j] == 5 || actualTerrain[i][j] == 7)
//                {
//                    for (int k = YMin;k <= YMax;k++)
//                    {
//                        for (int l = XMin;l <= XMax;l++)
//                        {
//                            if (Math.sqrt(((position.getX()/10-l)*(position.getX()/10-l))+((position.getY()/10-k)*(position.getY()/10))-k) > Math.sqrt(((position.getX()/10-j)*(position.getX()/10-j))+((position.getY()/10-i)*(position.getY()/10))-i))
//                            {
//                                if (direction > 0 && direction <= 90 && Math.atan((l-position.getX()/10)/(k-position.getY()/10)) >= Math.atan((j-position.getX()/10)/(i-position.getY()/10)) && Math.atan(((l+1)-position.getX()/10)/((k-1)-position.getY()/10)) <= Math.atan(((j+1)-position.getX()/10)/((i-1)-position.getY()/10)))
//                                {
//                                    knownTerrain[k][l] = actualTerrain[k][l];
//                                }
//                                else if (direction > 90 && direction <= 180 && Math.atan((k-position.getY()/10)/((l+1)-position.getX()/10)) >= Math.atan((i-position.getY()/10)/((j+1)-position.getX()/10)) && Math.atan(((k-1)-position.getY()/10)/(l-position.getX()/10)) <= Math.atan(((i+1)-position.getY()/10)/(j-position.getX()/10)))
//                                {
//                                    knownTerrain[k][l] = actualTerrain[k][l];
//                                }
//                                else if (direction <= 0 && direction > -90 && Math.atan(((l+1)-position.getX()/10)/(k-position.getY()/10)) <= Math.atan(((j+1)-position.getX()/10)/(i-position.getY()/10)) && Math.atan((l-position.getX()/10)/((k-1)-position.getY()/10)) >= Math.atan((j-position.getX()/10)/((i-1)-position.getY()/10)))
//                                {
//                                    knownTerrain[k][l] = actualTerrain[k][l];
//                                }
//                                else if (direction <= -90 && direction > -180 && Math.atan((k-position.getY()/10)/(l-position.getX()/10)) <= Math.atan((i-position.getY()/10)/(j-position.getX()/10)) && Math.atan(((k-1)-position.getY()/10)/((l+1)-position.getX()/10)) >= Math.atan(((i-1)-position.getY()/10)/((j+1)-position.getX()/10)))
//                                {
//                                    knownTerrain[k][l] = actualTerrain[k][l];
//                                }
//                            }
//                        }
//                    }
//                }
//                if (j == 0 || j == 99 || i == 0 || i == 99)
//                {
//                    stop = true;
//                    break;
//                }
//            }
//            if (stop == true)
//            {
//                break;
//            }
//        }
//    }

    public void updateKnownTerrain(){
        for(int r = 0; r < worldMap.getSize(); r++) {
            for(int c = 0; c < worldMap.getSize(); c++){
                if(viewingCone.contains(worldMap.convertArrayToWorld(c) + 0.5 * worldMap.convertArrayToWorld(1), //changed from *0.5 to *1
                        worldMap.convertArrayToWorld(r) + 0.5 * worldMap.convertArrayToWorld(1))) { //changed from *0.5 to *1
                    knownTerrain[r][c] = worldMap.getTileState(r, c);
                    //System.out.println("r: "+r+" c: "+c);
                    //System.out.println(worldMap.getTileState(r, c)+" "+knownTerrain[r][c]);
//                    System.out.println("reward reset for r: " + r + " c: " + c);
                }
            }
        }
        //System.out.println();
    }
    /**
     * Checks if we can hear other agents and if so add it personal memory with noise
     */
    public void checkForAgentSound(){
        Point2D tmpPoint = getMove(1000, direction);
        for(Agent agent : worldMap.getAgents()) {
            if(position.distance(agent.getPosition()) != 0) { //to not add hearing "ourselves" to our log tho a path that we have taken might be something that we want store in the end
                boolean soundHeard = false;
                double angleBetweenPoints = angleBetweenTwoPointsWithFixedPoint(tmpPoint.getX(), tmpPoint.getY(), agent.getPosition().getX(), agent.getPosition().getY(), position.getX(), position.getY());
                angleBetweenPoints += new Random().nextGaussian()*SOUND_NOISE_STDEV;
                if(position.distance(agent.getPosition()) < SOUNDRANGE_FAR && agent.currentSpeed > WALK_SPEED_FAST) {
                    soundHeard = true;
                } else if(position.distance(agent.getPosition()) < SOUNDRANGE_MEDIUMFAR && agent.currentSpeed > WALK_SPEED_MEDIUMFAST) {
                    soundHeard = true;
                } else if(position.distance(agent.getPosition()) < SOUNDRANGE_MEDIUM && agent.currentSpeed > WALK_SPEED_MEDIUM) {
                    soundHeard = true;
                } else if(position.distance(agent.getPosition()) < SOUNDRANGE_CLOSE && agent.currentSpeed > WALK_SPEED_SLOW) {
                    soundHeard = true;
                }
                if(soundHeard){
                    audioLogs.add(new AudioLog(System.nanoTime(), angleBetweenPoints, new Point2D(position.getX(), position.getY())));
                    System.out.println("Agent heard sound");
                }
            }

        }
    }

    public boolean sees(int r, int c) {
        return viewingCone.contains((c*(ASSUMED_WORLDSIZE/(double)worldMap.getSize())*SCALING_FACTOR), (r*(ASSUMED_WORLDSIZE/(double)worldMap.getSize())*SCALING_FACTOR));
    }

    public void createCone() {
        double x = position.getX();
        double y = position.getY();
        double visualRangeMin = visualRange[0] * SCALING_FACTOR; //max visionRange
        double visualRangeMax = visualRange[1] * SCALING_FACTOR; //max visionRange
//        double xRightTop = x + (2 * visualRangeMax * Math.cos(Math.toRadians(direction + viewingAngle/2)));//truncatedtriangle works weird shape bounded by circle so making triangle bigger shouldn't matter
//        double yRightTop = y + (2 * visualRangeMax * Math.sin(Math.toRadians(direction + viewingAngle/2)));//truncatedtriangle works weird shape bounded by circle so making triangle bigger shouldn't matter
//        double xLeftTop = x +  (2 * visualRangeMax * Math.cos(Math.toRadians(direction - viewingAngle/2)));//truncatedtriangle works weird shape bounded by circle so making triangle bigger shouldn't matter
//        double yLeftTop = y +  (2 * visualRangeMax * Math.sin(Math.toRadians(direction - viewingAngle/2)));//truncatedtriangle works weird shape bounded by circle so making triangle bigger shouldn't matter
//        double xRightBot = (visualRangeMin != 0) ? (x + (visualRangeMin * Math.cos(Math.toRadians(direction + viewingAngle/2)))) : x;
//        double yRightBot = (visualRangeMin != 0) ? (y + (visualRangeMin * Math.sin(Math.toRadians(direction + viewingAngle/2)))) : y;
//        double xLeftBot = (visualRangeMin != 0) ? (x + (visualRangeMin * Math.cos(Math.toRadians(direction - viewingAngle/2)))) : x;
//        double yLeftBot = (visualRangeMin != 0) ? (y + (visualRangeMin * Math.sin(Math.toRadians(direction - viewingAngle/2)))) : y;
//        Circle circle = new Circle(x, y, visualRangeMax);
//        double[] points = new double[]{
//                xLeftBot, yLeftBot,
//                xRightBot, yRightBot,
//                xRightTop, yRightTop,
//                xLeftTop, yLeftTop,
//        };
//        Polygon truncatedTriangle = new Polygon(points);
//        Shape cone = Shape.intersect(circle, truncatedTriangle);
////        double[] collisionPoints = new double[(AMOUNT_OF_VISION_TENTACLES + 2) * 2];
//        boolean obstructed = false;
        double[] collisionPoints = new double[((AMOUNT_OF_VISION_TENTACLES) * 2) + 4];
        for(int i = 0; i < AMOUNT_OF_VISION_TENTACLES; i++) {
            tentacleincrementloop:
            for(int j = 0; j < TENTACLE_INCREMENTS; j++) {
                Line tentacle = new Line();
                double xLeftTopLine = x + (visualRangeMax * (double)j/(TENTACLE_INCREMENTS-1) * Math.cos(Math.toRadians((direction - viewingAngle/2) + (viewingAngle/(AMOUNT_OF_VISION_TENTACLES-1))*i)));
                double yLeftTopLine = y + (visualRangeMax * (double)j/(TENTACLE_INCREMENTS-1) * Math.sin(Math.toRadians((direction - viewingAngle/2) + (viewingAngle/(AMOUNT_OF_VISION_TENTACLES-1))*i)));
                if(visualRangeMin != 0) {
                    double xLeftBotLine = x + (visualRangeMin * Math.cos(Math.toRadians((direction - viewingAngle/2) + (viewingAngle/AMOUNT_OF_VISION_TENTACLES)*i)));
                    double yLeftBotLine = y + (visualRangeMin * Math.sin(Math.toRadians((direction - viewingAngle/2) + (viewingAngle/AMOUNT_OF_VISION_TENTACLES)*i)));
                    tentacle.setStartX(xLeftBotLine);
                    tentacle.setStartY(yLeftBotLine);
                } else {
                    tentacle.setStartX(x);
                    tentacle.setStartY(y);
                }
                tentacle.setEndX(xLeftTopLine);
                tentacle.setEndY(yLeftTopLine);
                if(worldMap.isVisionObscuring(worldMap.getWorldGrid()[locationToWorldgrid(yLeftTopLine)][locationToWorldgrid(xLeftTopLine)]) || j == TENTACLE_INCREMENTS-1) {
//                    if(j != TENTACLE_INCREMENTS-1) obstructed = true;
                    collisionPoints[(i*2)+0] = xLeftTopLine;
                    collisionPoints[(i*2)+1] = yLeftTopLine;
                    break tentacleincrementloop;
                }
            }
        }
        collisionPoints[collisionPoints.length-2] = x + (visualRangeMin * Math.cos(Math.toRadians(direction - viewingAngle/2)));
        collisionPoints[collisionPoints.length-1] = y + (visualRangeMin * Math.sin(Math.toRadians(direction - viewingAngle/2)));
        collisionPoints[collisionPoints.length-4] = x + (visualRangeMin * Math.cos(Math.toRadians(direction + viewingAngle/2)));
        collisionPoints[collisionPoints.length-3] = y + (visualRangeMin * Math.sin(Math.toRadians(direction + viewingAngle/2)));
        Polygon cutout = new Polygon(collisionPoints);
//        if(obstructed) cone = Shape.subtract(cone, cutout);
//        cone = cutout;
//        cone.setSmooth(true);
//        cone.setFill(color);
//        viewingCone = cone;
        viewingCone = cutout;
        viewingCone.setFill(color);
    }

    public Shape getCone() {
        return viewingCone;
    }

    public static int locationToWorldgrid(double toBeConverted) {
        int supposedWorldSize = ASSUMED_WORLDSIZE;
        return (int)(toBeConverted * (1/((supposedWorldSize/worldMap.getWorldGrid().length)*SCALING_FACTOR)));
    }

    public static double angleBetweenTwoPointsWithFixedPoint(double point1X, double point1Y,
                                                             double point2X, double point2Y,
                                                             double fixedX, double fixedY) {

        double angle1 = Math.atan2(point1Y - fixedY, point1X - fixedX);
        double angle2 = Math.atan2(point2Y - fixedY, point2X - fixedX);

        return angle1 - angle2;
    }

    public Point2D getPosition() {
        return position;
    }

    public Point getGoalPosition() {
        return goalPosition;
    }

    public boolean isThreadStopped() {
        return exitThread;
    }

    public void setThreadStopped(boolean exit) {
        this.exitThread = exit;
    }
    public synchronized void setPosition(Point2D position) {
        this.position = position;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public List<AudioLog> getAudioLogs() {
        return audioLogs;
    }

    public void setAudioLogs(List<AudioLog> audioLogs) {
        this.audioLogs = audioLogs;
    }

    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction;
    }

    public double getViewingAngle() {
        return viewingAngle;
    }

    public void setViewingAngle(double viewingAngle) {
        this.viewingAngle = viewingAngle;
    }

    public double[] getVisualRange() {
        return visualRange;
    }

    public void setVisualRange(double[] visualRange) {
        this.visualRange = visualRange;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int[][] aStarTerrain(int[][] terrain)
    {
        List<Point> walls = new ArrayList<Point>();
        for(int i = 0; i < terrain.length; i++)
        {
            for(int j = 0; j < terrain[0].length; j++)
            {
                if(terrain[i][j] == 1 || terrain[i][j] == 5 || terrain[i][j] == 7)
                {
                    Point wall = new Point(i, j);
                    walls.add(wall); //WALL
                }
            }
        }
        //if(walls.size() == 0)
        //{
        //    System.out.println("adding corner");
        //    Point corner = new Point(0, 0);
        //    walls.add(corner);
        //    System.out.println(walls.size());
        //}
        int[][] blocks = new int[walls.size()][2];
        for(int i = 0; i < walls.size(); i++)
        {
            blocks[i][0] = (int)walls.get(i).getY();
            blocks[i][1] = (int)walls.get(i).getX();
        }
        return blocks;
    }

    public void turnToFace(double angle)
    {
        direction = angle;
    }

}