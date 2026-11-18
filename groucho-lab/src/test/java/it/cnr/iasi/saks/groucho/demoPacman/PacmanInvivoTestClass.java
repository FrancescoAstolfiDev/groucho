package it.cnr.iasi.saks.groucho.demoPacman;

import it.cnr.iasi.saks.groucho.common.Context;

/**
 * In-vivo test class referenced by demopacman/model.json: groucho-core invokes
 * invivoTestMethod when an annotated JPacman method (Game.move, Level.move) runs
 * and the governance manager activates the in-vivo session.
 */
public class PacmanInvivoTestClass {
    public boolean invivoTestMethod(Context c) {
        Object instrumentedObject = c.getInstrumentedObject();
        System.out.println("[demoPacman] in-vivo test on: " + instrumentedObject.getClass().getCanonicalName());
        return true;
    }
}
