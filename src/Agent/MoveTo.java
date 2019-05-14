package Agent;

import World.WorldMap;

import java.awt.geom.Point2D;

import static World.GameScene.SCALING_FACTOR;
/**
 * this routine makes the agent move from a point a to a target
 * @author Thibaut Donis
 */
public class MoveTo extends Routine {
     static double destX;
     static double destY;
    Routine routine;
    WorldMap worldMap;
    Guard guard;
    public MoveTo(double destX, double destY) {
        super();
        this.destX = destX;
        this.destY = destY;
    }

    public void reset() {
        start();
    }
    @Override
    public void act(Guard guard, WorldMap worldMap) {
        if(isWalking()){
            if(!isAtDestination(guard)){
                Move(guard);
            }
        }
    }
    private void Move(Guard guard) {
        guard.run();
        if (isAtDestination(guard)) {
            succeed();
        }
    }
    private boolean isAtDestination(Guard guard){
        return destX == guard.getPosition().getX() && destY == guard.getPosition().getY();
    }
    public void update() {
        if (routine.getState() == null) {
            // hasn't started yet so we start it
            routine.start();
        }
        routine.act(guard, worldMap);
    }

}