package World;
import Agent.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * WorldMap data structure
 * @author Kailhan Hokstam
 */

public class WorldMap implements Serializable {

    public int[][] worldGrid;
    public static final int EMPTY = 0;
    public static final int STRUCTURE = 1;
    public static final int DOOR = 2;
    public static final int OPEN_DOOR = 22;
    public static final int WINDOW = 3;
    public static final int OPEN_WINDOW = 33;
    public static final int TARGET = 4;
    public static final int SENTRY = 5;
    public static final int DECREASED_VIS_RANGE = 6;
    public static final int WALL = 7;
    public static final int UNEXPLORED = 8;
    public static final int GUARD = 9;
    public static final int INTRUDER = 10;
    public static final int SOUND = 11;
    private List<Agent> agents = new ArrayList<Agent>();
    private List<Thread> agentThreads = new ArrayList<Thread>();

    private int size;

    //public WorldMap() {
    //    this(200);
    //}

    public WorldMap(int size) {
        this(size, new ArrayList<Agent>());
    }

    public WorldMap(int size, ArrayList<Agent> agents) {
        this.size = size;
        this.worldGrid = new int[size][size];
        this.agents = agents;
        for(int i = 0; i < size; i++) {
            worldGrid[0][i] = WALL;
            worldGrid[i][0] = WALL;
            worldGrid[size-1][i] = WALL;
            worldGrid[i][size-1] = WALL;
        }
    }

    public WorldMap(WorldMap worldMap) {
        this.size = worldMap.getSize();
        this.worldGrid = new int[size][size];
        this.agents = worldMap.getAgents();
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                this.worldGrid[r][c] = worldMap.getWorldGrid()[r][c];
            }
        }
    }

    /**
     * Prints out the world for diagnostic purposes
     */
    public void displayWorldGrid() {
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                System.out.printf("%3d", worldGrid[r][c]);
            }
            System.out.println();
        }
    }

    public int[][] getWorldGrid()
    {
        return worldGrid;
    }

    public void setWorldGrid(int[][] worldGrid)
    {
        this.worldGrid = worldGrid;
    }

    public int getSize()
    {
        return size;
    }

    /**
     * updates tile at r, c to certain type e.g. WALL
     * @param r row
     * @param c column
     * @param state state to compare to
     */
    public void updateTile(int r, int c, int state) {
        worldGrid[r][c] = state;
    }

    public int getTileState(int r, int c) {
         return worldGrid[r][c];
    }

    /**
     * checks if a square has a certain state
     * @param r row
     * @param c column
     * @param state state to compare to
     * @return true if the square has the same state at state
     */
    public boolean checkTile(int r, int c, int state) {
        return (worldGrid[r][c] == state);
    }

    public List<Agent> getAgents() {
        return this.agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    /**
     * Removes an agent by exiting its while logic loop (where we determine what an agent does every tick)
     * And removes it from a worlds list of active agent and active threads
     * @param toBeRemoved
     * @return index of agent removed
     */
    public int removeAgent(Agent toBeRemoved) {
        int index = agents.indexOf(toBeRemoved);
        agents.get(index).setThreadStopped(true);
        agents.remove(index);
        agentThreads.remove(index);
        return index;
    }


    /**
     * Adds an agent and its (not yet running thread) to a world, needs to be started for it to actually start executing its logic
     * @param toBeAdded the agent that we want to add (probably want to at least specify its location before adding to world)
     */
    public void addAgent(Agent toBeAdded) {
        this.agents.add(toBeAdded);
        this.agentThreads.add(new Thread(toBeAdded));
        //startAgents();
    }

    /**
     * Starts all agents whos threads have been added to the world
     */
    public void startAgents() {
        for(Thread thread : this.agentThreads) {
            thread.start();
        }
    }

    /**
     * Removes all agents from the world, first stops their threads
     */
    public void removeAllAgents() {
        for(Agent agent : agents) {
            agent.setThreadStopped(true);
        }
        agents.clear();
        agentThreads.clear();
        System.out.println("Removed all agents");
    }
}