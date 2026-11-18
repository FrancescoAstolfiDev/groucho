package it.cnr.iasi.saks.groucho.demoPacman;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import it.cnr.iasi.saks.groucho.annotation.TestableInVivo;
import nl.tudelft.jpacman.board.Direction;
import nl.tudelft.jpacman.board.Unit;
import nl.tudelft.jpacman.game.Game;
import nl.tudelft.jpacman.level.Level;
import nl.tudelft.jpacman.level.Player;

/**
 * Checks that the groucho-lab agent has annotated with @TestableInVivo the JPacman
 * methods listed in src/test/resources/demopacman/model.json, and only those.
 * It must run with the agents of the pacmanProfile (mvn -PpacmanProfile verify).
 */
public class PacmanAnnotationTest_IT {
    private static final String ORACLE_INVIVO_TEST_CLASS = "it.cnr.iasi.saks.groucho.demoPacman.PacmanInvivoTestClass";
    private static final String ORACLE_INVIVO_TEST = "invivoTestMethod";
    private static final int ORACLE_INVIVO_CARVING_DEPTH = 1;
    private static final boolean ORACLE_INVIVO_PAUSE_OTHER_THREADS = false;

    private TestableInVivo retrieveInvivoAnnotation(Class<?> c, String methodName, Class<?>... paramTypes) {
        Method reflectedMethod = null;
        try {
            reflectedMethod = c.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            Assert.fail(e.getMessage());
        }
        return reflectedMethod.getAnnotation(TestableInVivo.class);
    }

    private void checkAnnotation(Class<?> c, String methodName, Class<?>... paramTypes) {
        TestableInVivo invivoAnnotation = this.retrieveInvivoAnnotation(c, methodName, paramTypes);

        String errMessage = "the method:" + c.getCanonicalName() + "->" + methodName + " has not been tagged";
        Assert.assertNotNull(errMessage, invivoAnnotation);

        Assert.assertEquals(ORACLE_INVIVO_TEST_CLASS, invivoAnnotation.invivoTestClass());
        Assert.assertEquals(ORACLE_INVIVO_TEST, invivoAnnotation.invivoTest());
        Assert.assertEquals(ORACLE_INVIVO_CARVING_DEPTH, invivoAnnotation.carvingDepth());
        Assert.assertEquals(ORACLE_INVIVO_PAUSE_OTHER_THREADS, invivoAnnotation.pauseOtherThreads());
    }

    @Test
    public void gameMoveIsAnnotated() {
        this.checkAnnotation(Game.class, "move", Player.class, Direction.class);
    }

    @Test
    public void levelMoveIsAnnotated() {
        this.checkAnnotation(Level.class, "move", Unit.class, Direction.class);
    }

    @Test
    public void methodNotInTheModelIsNotAnnotated() {
        TestableInVivo invivoAnnotation = this.retrieveInvivoAnnotation(Game.class, "start");
        Assert.assertNull("Game.start is not in model.json but it has been tagged", invivoAnnotation);
    }
}
