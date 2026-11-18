package it.cnr.iasi.saks.groucho.demoPacman;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import it.cnr.iasi.saks.groucho.common.Context;
import nl.tudelft.jpacman.board.Board;
import nl.tudelft.jpacman.board.Unit;
import nl.tudelft.jpacman.game.Game;
import nl.tudelft.jpacman.level.Level;
import nl.tudelft.jpacman.npc.Ghost;

/**
 * In-vivo test class referenced by demoPacman/model.json: groucho-core invokes
 * invivoTestMethod when an annotated JPacman method (Game.move, Level.move) runs
 * and the governance manager activates the in-vivo session.
 *
 * For Level.move(Unit, Direction) the Context holds:
 *  - getInstrumentedObject(): the Level
 *  - getOtherReferencesInContext().get(0): the Unit that is about to move (a Ghost or the Player)
 *  - getOtherReferencesInContext().get(1): the Direction of the move
 *
 * This is where the in-vivo tests are invoked: InVivoLiveGameSuite runs on the live level,
 * i.e. on the state captured right before the move.
 */
public class PacmanInvivoTestClass {

    private static final List<String> GHOST_STATES = new ArrayList<String>();

    public boolean invivoTestMethod(Context c) {
        if (c.getInstrumentedObject() instanceof Game) {
            // Game.move: the same move reaches Level.move right after, where it is tested
            return true;
        }
        Level level = (Level) c.getInstrumentedObject();
        Object mover = c.getOtherReferencesInContext().get(0);

        // The ghosts keep moving on their own threads (pauseOtherThreads is false): holding the lock
        // of Level.move keeps the level still while the state is captured and the suite runs
        synchronized (moveLock(level)) {
            if (mover instanceof Ghost) {
                String name = mover.getClass().getSimpleName();
                String state = name + " moves " + c.getOtherReferencesInContext().get(1)
                        + " --> {" + name + "=" + carveGhostPositions(level.getBoard()).get(name) + "}";
                System.out.println("[demoPacman] captured state: " + state);
                recordGhostState(state);
                // the player and only the ghost that is moving
                new InVivoLiveGameSuite().runOnMover(c);
            } else {
                // a move of Pac-Man: the player and all the ghosts
                new InVivoLiveGameSuite().runAll(c);
            }
        }
        return true;
    }

    /**
     * @return the cell of every ghost on the board, by name (ordered), e.g. {Blinky=(13,11), Clyde=(15,14), ...}
     */
    private static Map<String, String> carveGhostPositions(Board board) {
        Map<String, String> positions = new TreeMap<String, String>();
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                for (Unit unit : board.squareAt(x, y).getOccupants()) {
                    if (unit instanceof Ghost) {
                        positions.put(unit.getClass().getSimpleName(), "(" + x + "," + y + ")");
                    }
                }
            }
        }
        return positions;
    }

    /** @return the private lock that Level.move holds while it moves a unit */
    private static Object moveLock(Level level) {
        try {
            Field lock = Level.class.getDeclaredField("moveLock");
            lock.setAccessible(true);
            return lock.get(level);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private synchronized static void recordGhostState(String state) {
        GHOST_STATES.add(state);
    }

    public synchronized static List<String> getGhostStates() {
        return new ArrayList<String>(GHOST_STATES);
    }

    public synchronized static void resetGhostStates() {
        GHOST_STATES.clear();
    }
}
