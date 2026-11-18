package it.cnr.iasi.saks.groucho.demoPacman;

import it.cnr.iasi.saks.groucho.common.Context;
import nl.tudelft.jpacman.board.Board;
import nl.tudelft.jpacman.board.Direction;
import nl.tudelft.jpacman.board.Square;
import nl.tudelft.jpacman.board.Unit;
import nl.tudelft.jpacman.game.Game;
import nl.tudelft.jpacman.level.Level;
import nl.tudelft.jpacman.level.Pellet;
import nl.tudelft.jpacman.level.Player;
import nl.tudelft.jpacman.npc.Ghost;
import nl.tudelft.jpacman.npc.ghost.Blinky;
import nl.tudelft.jpacman.npc.ghost.Inky;

import java.lang.reflect.Field;
import java.util.*;

/**
 * In-vivo suite for the rules of the game. It is not a JUnit test: GROUCHO invokes it by reflection,
 * as configured in lab-jpacman/model.json and lab-jpacman/model-fantasmi.json.
 *
 * The suite only reads the live level: it makes no moves and no copies. The rules about a move are
 * checked on the next move of the ghosts, the only units that can tell it in advance. The move is
 * asked for but not made, so these tests check where it leads, not what happens once it is made.
 *
 * Each test prints one PASS, SKIP or FAIL line. SKIP means that nothing was verified.
 *
 * Two entry points: runAll checks the player and all the ghosts; runOnMover checks the player and
 * only the ghost that called the intercepted Level.move (the first reference in the context), so
 * that the outcome depends on the same ghost that the state of the session describes.
 */
public class InVivoLiveGameSuite {

    private static final String TAG = "[InvivoSuite] ";
    private static final int PELLET_POINTS = 10;

    /** How far Inky looks ahead of the player, as in Inky.SQUARES_AHEAD. */
    private static final int INKY_SQUARES_AHEAD = 2;

    /**
     * Above this distance from Blinky to its target, the next move of Inky is not asked for. The search
     * that ignores terrain grows exponentially, and here it would stop the game: the session holds
     * the lock on the moves.
     */
    private static final int INKY_MAX_DISTANCE = 8;

    /**
     * Pellets of the whole game, fixed the first time each level is seen. Kept per level because
     * several games may run in the same JVM; weak keys let finished levels be collected.
     */
    private static final Map<Level, Integer> TOTAL_PELLETS = new WeakHashMap<>();

    private int totalPellets;

    private Level level;
    private Board board;
    private Player player;
    private List<Ghost> ghosts;
    /** The ghosts whose next move is checked: all of them, or only the one that is moving. */
    private List<Ghost> tested;
    private List<Unit> units;
    private Map<Ghost, Direction> nextMoves;


    /** Outcomes of all the tests run so far in this JVM (see outcomes()). */
    private static int passed;
    private static int skipped;
    private static int failed;
    /**
     * Entry point of the suite.
     *
     * @param context
     *            The context of the intercepted method, either Game.move or Level.move.
     */
    public void runAll(Context context) {
        System.out.println(TAG + "suite invocata da " + context.getInstrumentedClassName()
                + "." + context.getInstrumentedMethodName());
        load(context.getInstrumentedObject());
        runTests();
    }

    /**
     * Entry point of the suite restricted to the moving ghost.
     *
     * @param context
     *            The context of the intercepted Level.move: its first reference is the moving unit.
     */
    public void runOnMover(Context context) {
        Object mover = context.getOtherReferencesInContext().isEmpty() ? null
                : context.getOtherReferencesInContext().get(0);
        System.out.println(TAG + "suite invocata da " + context.getInstrumentedClassName()
                + "." + context.getInstrumentedMethodName()
                + (mover instanceof Ghost ? " per " + name((Ghost) mover) : ""));
        load(context.getInstrumentedObject());
        if (mover instanceof Ghost && ghosts.contains(mover)) {
            this.tested = new ArrayList<>();
            this.tested.add((Ghost) mover);
            this.units = new ArrayList<>();
            this.units.add(player);
            this.units.add((Ghost) mover);
        }
        runTests();
    }

    private void runTests() {
        synchronized (TOTAL_PELLETS) {
            Integer total = TOTAL_PELLETS.get(level);
            if (total == null) {
                total = level.remainingPellets() + player.getScore() / PELLET_POINTS;
                TOTAL_PELLETS.put(level, total);
            }
            totalPellets = total;
        }

        run("nonSuMuro", this::neverOnWall);
        run("neiLimiti", this::withinGrid);
        run("unitaUniche", this::unitsAreUnique);
        run("mangiaPuntino", this::playerTileIsEmpty);
        run("conservazione", this::pelletsAreConserved);
        run("muroDavanti", this::nextMoveAvoidsWalls);
        run("spostamentoLimitato", this::nextMoveIsOneTile);
        run("wrapTunnel", this::nextMoveWrapsTunnel);
    }

    /* Current state of all the units */

    private String neverOnWall() {
        for (Unit unit : units) {
            check(unit.getSquare().isAccessibleTo(unit), name(unit) + " su un muro in " + describe(unit.getSquare()));
        }
        return units.size() + " unita'";
    }

    private String withinGrid() {
        for (Unit unit : units) {
            int[] tile = tile(unit.getSquare());
            check(tile != null, name(unit) + " su una casella fuori dalla griglia");
            check(unit.getSquare().getOccupants().contains(unit), name(unit) + " non risulta tra gli occupanti di "
                    + describe(unit.getSquare()));
        }
        return units.size() + " unita'";
    }

    /**
     * Scans the whole board: among the occupants there is exactly the player of the game, exactly the
     * ghosts of the level, and each of them is on one square only. withinGrid checks that the square
     * of each unit contains it; this checks the other way round, so it also finds a unit left behind
     * on a square it has moved away from (a copy) or a unit that does not belong to the level. It is
     * a check on the whole board, so it does not depend on the moving ghost.
     */
    private String unitsAreUnique() {
        Map<Unit, Square> seen = new IdentityHashMap<>();
        int players = 0;
        int ghostCount = 0;
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Square square = board.squareAt(x, y);
                for (Unit unit : square.getOccupants()) {
                    if (unit instanceof Player) {
                        players++;
                        check(unit == player, "in " + describe(square) + " un giocatore che non e' quello della partita");
                    } else if (unit instanceof Ghost) {
                        ghostCount++;
                        check(ghosts.contains(unit), "in " + describe(square) + " un " + name(unit)
                                + " che non e' tra i fantasmi del livello");
                    } else {
                        continue;
                    }
                    Square other = seen.put(unit, square);
                    check(other == null, name(unit) + " compare in due caselle: " + describe(other) + " e "
                            + describe(square));
                }
            }
        }
        check(players == 1, players + " giocatori sulla board invece di 1");
        check(ghostCount == ghosts.size(), ghostCount + " fantasmi sulla board invece di " + ghosts.size());
        return players + " giocatore, " + ghostCount + " fantasmi";
    }

    /** The tile the player entered has been emptied. The change in pellets and score is in pelletsAreConserved. */
    private String playerTileIsEmpty() {
        check(!hasPellet(player.getSquare()), "puntino non mangiato nel tile del giocatore "
                + describe(player.getSquare()));
        return "tile " + describe(player.getSquare()) + " vuoto";
    }

    private String pelletsAreConserved() {
        int remaining = level.remainingPellets();
        int eaten = player.getScore() / PELLET_POINTS;
        check(remaining + eaten == totalPellets, "rimasti " + remaining + " + mangiati " + eaten
                + " != totale " + totalPellets);
        return remaining + " + " + eaten + " = " + totalPellets;
    }

    /* Next move of the ghosts */

    private String nextMoveAvoidsWalls() {
        Map<Ghost, Direction> moves = nextMoves();
        for (Map.Entry<Ghost, Direction> move : moves.entrySet()) {
            Square origin = move.getKey().getSquare();
            Square target = origin.getSquareAt(move.getValue());
            check(target.isAccessibleTo(move.getKey()), name(move.getKey()) + " in " + describe(origin) + " sceglie "
                    + move.getValue() + " verso il muro in " + describe(target));
        }
        return moves.size() + " fantasmi";
    }

    private String nextMoveIsOneTile() {
        Map<Ghost, Direction> moves = nextMoves();
        for (Map.Entry<Ghost, Direction> move : moves.entrySet()) {
            Square origin = move.getKey().getSquare();
            Square target = origin.getSquareAt(move.getValue());
            int distance = distance(tile(origin), tile(target));
            check(distance == 1, name(move.getKey()) + " verso " + move.getValue() + " da " + describe(origin)
                    + " arriverebbe a " + distance + " tile, in " + describe(target));
        }
        return moves.size() + " fantasmi";
    }

    private String nextMoveWrapsTunnel() {
        int crossings = 0;
        for (Map.Entry<Ghost, Direction> move : nextMoves().entrySet()) {
            Square origin = move.getKey().getSquare();
            int[] from = tile(origin);
            boolean leavesWest = from[0] == 0 && move.getValue() == Direction.WEST;
            boolean leavesEast = from[0] == board.getWidth() - 1 && move.getValue() == Direction.EAST;
            if (leavesWest || leavesEast) {
                Square target = origin.getSquareAt(move.getValue());
                int[] to = tile(target);
                check(to[0] == board.getWidth() - 1 - from[0] && to[1] == from[1], name(move.getKey()) + " uscendo da "
                        + describe(origin) + " verso " + move.getValue() + " arriverebbe in " + describe(target));
                crossings++;
            }
        }
        requireThat(crossings > 0, "nessun fantasma sta per uscire dal bordo");
        return crossings + " attraversamenti";
    }


    /* Support */
    private void run(String test, Check body) {
        try {
            System.out.println(TAG + "PASS " + test + " - " + body.run());
            count(1, 0, 0);
        } catch (Skip e) {
            System.out.println(TAG + "SKIP " + test + " - " + e.getMessage());
            count(0, 1, 0);
        } catch (AssertionError | RuntimeException e) {
            System.out.println(TAG + "FAIL " + test + " - "
                    + (e instanceof AssertionError && e.getMessage() != null ? e.getMessage() : e));
            count(0, 0, 1);
        }
    }

    private static synchronized void count(int pass, int skip, int fail) {
        passed += pass;
        skipped += skip;
        failed += fail;
    }

    /** @return the outcomes so far: { passed, skipped, failed } */
    public static synchronized int[] outcomes() {
        return new int[] { passed, skipped, failed };
    }

    public static synchronized void resetOutcomes() {
        passed = 0;
        skipped = 0;
        failed = 0;
    }



    /** @return the next move of the ghosts that can tell it; asked once, because it can be random. */
    private Map<Ghost, Direction> nextMoves() {
        if (nextMoves == null) {
            Map<Ghost, Direction> moves = new LinkedHashMap<>();
            for (Ghost ghost : tested) {
                Direction direction = isTooSlowToAsk(ghost) ? null : ghost.nextMove();
                if (direction != null) {
                    moves.put(ghost, direction);
                }
            }
            nextMoves = moves;
        }
        requireThat(!nextMoves.isEmpty(), "nessun fantasma con una mossa successiva");
        return nextMoves;
    }

    private boolean isTooSlowToAsk(Ghost ghost) {
        if (!(ghost instanceof Inky)) {
            return false;
        }
        for (Ghost other : ghosts) {
            if (other instanceof Blinky) {
                Square target = player.squaresAheadOf(INKY_SQUARES_AHEAD);
                return distance(tile(other.getSquare()), tile(target)) > INKY_MAX_DISTANCE;
            }
        }
        return false;
    }

    private boolean hasPellet(Square square) {
        for (Unit unit : square.getOccupants()) {
            if (unit instanceof Pellet) {
                return true;
            }
        }
        return false;
    }

    /** @return the coordinates of the square, which does not know them, or null if it is not on the board. */
    private int[] tile(Square square) {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                if (board.squareAt(x, y) == square) {
                    return new int[] { x, y };
                }
            }
        }
        return null;
    }

    /** @return the distance in tiles between two positions, counting a wrap around the grid as one step. */
    private int distance(int[] from, int[] to) {
        int dx = Math.abs(from[0] - to[0]);
        int dy = Math.abs(from[1] - to[1]);
        return Math.min(dx, board.getWidth() - dx) + Math.min(dy, board.getHeight() - dy);
    }

    private String describe(Square square) {
        int[] tile = tile(square);
        return tile == null ? "una casella sconosciuta" : "(" + tile[0] + "," + tile[1] + ")";
    }

    private static String name(Unit unit) {
        return unit.getClass().getSimpleName();
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError(failure);
        }
    }

    private static void requireThat(boolean condition, String reasonForSkipping) {
        if (!condition) {
            throw new Skip(reasonForSkipping);
        }
    }

    /** Level does not expose its players and ghosts, and scanning the board would miss a lost unit. */
    @SuppressWarnings("unchecked")
    private void load(Object instrumented) {
        this.level = instrumented instanceof Game ? ((Game) instrumented).getLevel() : (Level) instrumented;
        this.board = level.getBoard();
        try {
            Field players = Level.class.getDeclaredField("players");
            players.setAccessible(true);
            this.player = ((List<Player>) players.get(level)).get(0);
            Field npcs = Level.class.getDeclaredField("npcs");
            npcs.setAccessible(true);
            this.ghosts = new ArrayList<>(((Map<Ghost, ?>) npcs.get(level)).keySet());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        this.tested = ghosts;
        this.units = new ArrayList<>(ghosts);
        this.units.add(0, player);
    }

    /** A test: it returns what it verified, or fails with an AssertionError, or gives up with a Skip. */
    private interface Check {
        String run();
    }

    private static final class Skip extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Skip(String reason) {
            super(reason);
        }
    }
}
