package Agent;
import javafx.scene.paint.Color;

import javafx.geometry.Point2D;
import java.awt.Point;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import static World.GameScene.SCALING_FACTOR;
import static World.WorldMap.OPEN_DOOR;

/**
 * A subclass of Agent for the Intruders
 * @author Benjamin, Kailhan
 */

public class Intruder extends Agent {
    protected boolean tired;
    protected double startTime = 0;
    protected final long createdMillis = System.currentTimeMillis();
    protected long blindMillis = 0;
    protected int sprintCounter = 5;
    protected int walkCounter = 10; //check if this is right (might be 10 sec not 15)



    /**
     * An Intruder constructor with an empty internal map
     *
     * @param position  is a point containing the coordinates of the Intruderq
     * @param direction is the angle which the agent is facing, this spans from -180 to 180 degrees
     */

    public Intruder(Point2D position, double direction) {
        super(position, direction);
        this.viewingAngle = 45;
        this.visualRange[0] = 0;
        this.visualRange[1] = 7.5;
        this.color = Color.LIGHTGOLDENRODYELLOW;
        this.tired = false;
    }

    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Guard)) return false;
        Intruder o = (Intruder) obj;
        if ((o.direction == this.direction) && (o.position.equals(this.position)) && (o.tired == this.tired))
            equals = true;
        return equals;
    }

    /**
     * A method for going through a window or door. It is important the the intruder is standing on the tile to be changed.
     * The time taken is consistent (3 seconds for a window and 5 for a door), unless a door is to be opened quietly, in which case a normal distribution is used.
     */

    public void executeAgentLogic() {
        try {
            gameTreeIntruder(delta);
        } catch (Exception e) {
            System.out.println("pls fix when intruder is on target");
        }
    }

    public void gameTreeIntruder(double timeStep) {
        //TODO fix corner issue
        //TODO add blur
        //TODO check for guards
        //TODO make noise
        //TODO test doors and windows
        //TODO add weights to flags and other types of squares, try manually an possibly with a genetic algorithm
        //this createCone should be redundant but it resolves some errors due to not being able to properly access the cones
        createCone();
        rePath = false;
        if (tempWalls.size() > 0) {
            for (int i = 0; i < tempWalls.size(); i++) {
                knownTerrain[tempWalls.get(i).y][tempWalls.get(i).x] = worldMap.getWorldGrid()[tempWalls.get(i).y][tempWalls.get(i).y];
                int[][] phaseDetectionBlocks = aStarTerrain(knownTerrain);
                Astar phaseDetectionPathFinder = new Astar(knownTerrain[0].length, knownTerrain.length, (int) (position.getX() / SCALING_FACTOR), (int) (position.getY() / SCALING_FACTOR), (int) goalPosition.getX(), (int) goalPosition.getY(), phaseDetectionBlocks);
                List<Node> phaseDetectionPath = phaseDetectionPathFinder.findPath();
                for (int j = 0; j < phaseDetectionPath.size(); j++) {
                    if (phaseDetectionPath.get(j).row == tempWalls.get(i).y && phaseDetectionPath.get(j).column == tempWalls.get(i).x) {
                        knownTerrain[tempWalls.get(i).y][tempWalls.get(i).x] = 7;
                    } else {
                        tempWalls.remove(i);
                        break;
                    }
                }
            }
        }
        if (!frozen) {
            //open door
            if (worldMap.worldGrid[(int) (position.getY() / SCALING_FACTOR)][(int) (position.getX() / SCALING_FACTOR)] == 2) {
                worldMap.worldGrid[(int) (position.getY() / SCALING_FACTOR)][(int) (position.getX() / SCALING_FACTOR)] = 0;
                worldMap.updateTile(locationToWorldgrid(position.getY()), locationToWorldgrid(position.getX()), OPEN_DOOR);

                knownTerrain[(int) (position.getY() / SCALING_FACTOR)][(int) (position.getX() / SCALING_FACTOR)] = 0;
                Random random = new Random();
                startTime = System.currentTimeMillis();
                if (Math.random() > 0.5) {
                    freezeTime = (random.nextGaussian() * 2 + 12);
                } else {
                    freezeTime = 5;
                    //HERE A NOISE MUST BE MADE!!!!!
                }
                frozen = true;
            }
            //go through window
            else if (worldMap.worldGrid[(int) (position.getY() / SCALING_FACTOR)][(int) (position.getX() / SCALING_FACTOR)] == 3) {
                startTime = System.currentTimeMillis();
                worldMap.worldGrid[(int) (position.getY() / SCALING_FACTOR)][(int) (position.getX() / SCALING_FACTOR)] = 0;
                freezeTime = 3;
                frozen = true;
            }
        }
        if (oldTempGoal != null) {
            checkChangedStatus();
        }
        double elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        if (elapsedTime > freezeTime) {
            frozen = false;
            startTime = 0;
            freezeTime = 0;
            if (!blind) {
                updateKnownTerrain();
                //System.out.println("nottttttttttttttttttttttttttttt blind");
            } else {
                //System.out.println("blinnnnnnnnnnnnnnnnnnnnnnnnnnd");
                long nowMillis = System.currentTimeMillis();
                int countSec = (int) ((nowMillis - this.blindMillis) / 1000);
                //System.out.println(countSec);
                if (countSec == 2) {
                    blind = false;
                }
            }

            //prints the known terrain every iteration
            //for(int i = 0; i < knownTerrain.length; i++)
            //{
            //    for(int j = 0; j < knownTerrain.length; j++)
            //    {
            //        System.out.print(knownTerrain[i][j]+" ");
            //    }
            //    System.out.println();
            //}
            //System.out.println();
            //System.out.println();

            oldTempGoal = tempGoal;
            int[][] blocks = aStarTerrain(knownTerrain);
            Astar pathFinder = new Astar(knownTerrain[0].length, knownTerrain.length, (int) (position.getX() / SCALING_FACTOR), (int) (position.getY() / SCALING_FACTOR), (int) goalPosition.getX(), (int) goalPosition.getY(), blocks);
            List<Node> path = pathFinder.findPath();
            if (!changed) {
                tempGoal = new Point2D((path.get(path.size() - 1).row * SCALING_FACTOR) + (SCALING_FACTOR / 2), (path.get(path.size() - 1).column * SCALING_FACTOR) + (SCALING_FACTOR / 2));
                if (path.size() > 1) {
                    previousTempGoal = new Point2D((path.get(path.size() - 2).row * SCALING_FACTOR) + (SCALING_FACTOR / 2), (path.get(path.size() - 2).column * SCALING_FACTOR) + (SCALING_FACTOR / 2));
                } else {
                    previousTempGoal = tempGoal;
                }
            }
            if (oldTempGoal != null) {
                wallPhaseDetection();
                if (rePath) {
                    blocks = aStarTerrain(knownTerrain);
                    pathFinder = new Astar(knownTerrain[0].length, knownTerrain.length, (int) (position.getX() / SCALING_FACTOR), (int) (position.getY() / SCALING_FACTOR), (int) goalPosition.getX(), (int) goalPosition.getY(), blocks);
                    path = pathFinder.findPath();
                    if (!changed) {
                        tempGoal = new Point2D((path.get(path.size() - 1).row * SCALING_FACTOR) + (SCALING_FACTOR / 2), (path.get(path.size() - 1).column * SCALING_FACTOR) + (SCALING_FACTOR / 2));
                        if (path.size() > 1) {
                            previousTempGoal = new Point2D((path.get(path.size() - 2).row * SCALING_FACTOR) + (SCALING_FACTOR / 2), (path.get(path.size() - 2).column * SCALING_FACTOR) + (SCALING_FACTOR / 2));
                        } else {
                            previousTempGoal = tempGoal;
                        }
                    }
                }
                cornerCorrection();
            }
            double divisor = Math.abs(tempGoal.getY() - position.getY());
            double preDivisor = Math.abs(previousTempGoal.getY() - tempGoal.getY());
            if (divisor == 0) {
                divisor++;
                System.out.println("divisor is zero");
            } else if (preDivisor == 0) {
                preDivisor++;
                //System.out.println("preDivisor is zero");
            }
            double turnAngle = Math.toDegrees(Math.atan(Math.abs(tempGoal.getX() - position.getX()) / divisor));
            double previousAngle = Math.toDegrees(Math.atan(Math.abs(previousTempGoal.getX() - tempGoal.getX()) / preDivisor));
            double walkingDistance = (BASE_SPEED * SCALING_FACTOR * timeStep);
            double sprintingDistance = (SPRINT_SPEED * SCALING_FACTOR * timeStep);
            double finalAngle = previousAngle - turnAngle;
            if (finalAngle > 45 || finalAngle < -45) {
                //System.out.println(turnAngle);
                blind = true;
                blindMillis = System.currentTimeMillis();
            }
            if (tempGoal.getX() >= position.getX() && tempGoal.getY() <= position.getY()) {
                turnToFace(turnAngle - 90);
            } else if (tempGoal.getX() >= position.getX() && tempGoal.getY() > position.getY()) {
                turnToFace(90 - turnAngle);
            } else if (tempGoal.getX() < position.getX() && tempGoal.getY() > position.getY()) {
                turnToFace(90 + turnAngle);
            } else if (tempGoal.getX() < position.getX() && tempGoal.getY() <= position.getY()) {
                turnToFace(270 - turnAngle);
            }
            if (!tired) {
                if (legalMoveCheck(sprintingDistance)) {
                    long nowMillis = System.currentTimeMillis();
                    int countSec = (int) ((nowMillis - this.createdMillis) / 1000);
                    if (countSec != sprintCounter) {
                        move(sprintingDistance);
                    } else {
                        tired = true;
                        sprintCounter = sprintCounter + 15;
                    }
                }
            }
            if (tired) {
                if (legalMoveCheck(walkingDistance)) {
                    long nowMillis = System.currentTimeMillis();
                    int countSec = (int) ((nowMillis - this.createdMillis) / 1000);
                    if (countSec != walkCounter) {
                        move(walkingDistance);
                    } else {
                        tired = false;
                        walkCounter += 10; //changed from 15
                    }
                }
            }
        }
    }
}
