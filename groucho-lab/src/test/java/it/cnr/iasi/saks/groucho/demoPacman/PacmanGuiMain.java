package it.cnr.iasi.saks.groucho.demoPacman;

import it.cnr.iasi.saks.groucho.performanceOverheadTest.TestGovernanceManager_ActivationWithProbability;
import nl.tudelft.jpacman.Launcher;

/**
 * Starts JPacman with its Swing UI, to play a game under the GROUCHO agents (see demo-pacman-gui.sh).
 * Every move goes through the annotated Level.move: with the given probability it activates an
 * in-vivo session on PacmanInvivoTestClass, which runs InVivoLiveGameSuite on the live game.
 *
 * Argument: the activation probability of the in-vivo session (default 0.1).
 */
public class PacmanGuiMain {

	private static final float DEFAULT_PROBABILITY = 0.1f;

	public static void main(String[] args) {
		float probability = args.length > 0 ? Float.parseFloat(args[0]) : DEFAULT_PROBABILITY;
		TestGovernanceManager_ActivationWithProbability.setActivationProbability(probability);
		System.out.println("[demoPacman] activation probability of the in-vivo session: " + probability);

		new Launcher().launch();
	}
}
