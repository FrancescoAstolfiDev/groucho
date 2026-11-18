#!/bin/bash
#
# Plays JPacman with its Swing UI under the GROUCHO agents. Every move goes through the annotated
# Level.move: with probability p it activates an in-vivo session on PacmanInvivoTestClass, which
# captures the state and runs InVivoLiveGameSuite on the live game.
#
# Order of the agents (as in the pacmanProfile of the pom):
#   1. CROCHET       checkpoint/rollback
#   2. groucho-lab   reads demoPacman/model.json and adds @TestableInVivo
#   3. groucho-core  sees the annotation and injects the call to the in-vivo test
#
#   ./demo-pacman-gui.sh [p]      p = activation probability of the in-vivo session (default 0.1)
#
# Prerequisites (from the root of the repository, with JDK 8):
#   mvn clean install
#   mvn -PpacmanProfile verify -pl groucho-lab     (compiles the demoPacman classes in target/test-classes)
#
# The whole output goes to target/demo-pacman-gui.log; the terminal only shows the
# captured states and the outcomes of the suite.

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
CROCHET="$ROOT/groucho-crochet/target"
CORE="$ROOT/groucho-core/target"
LAB="$HERE/target"
CRIJ_JAR="$CROCHET/CRIJ-0.0.1-SNAPSHOT.jar"
CORE_JAR="$CORE/groucho-core-1.0-SNAPSHOT.jar"
LAB_JAR="$LAB/groucho-lab-1.0-SNAPSHOT.jar"
CONFIG="$HERE/src/test/resources/demoPacman/testingConfPacman.properties"
LOG="$LAB/demo-pacman-gui.log"
PROBABILITY="${1:-0.1}"

for f in "$CROCHET/jre-inst/bin/java" "$CRIJ_JAR" "$CORE_JAR" "$LAB_JAR" "$CONFIG" \
	"$LAB/dependency/jpacman-framework-8.1.1.jar" \
	"$LAB/test-classes/it/cnr/iasi/saks/groucho/demoPacman/PacmanGuiMain.class"; do
	if [ ! -e "$f" ]; then
		echo "Missing file: $f (see the prerequisites at the top of this script)" >&2
		exit 1
	fi
done

"$CROCHET/jre-inst/bin/java" \
	-Xmx4G -agentpath:"$CROCHET/libtagging.so" \
	-javaagent:"$CRIJ_JAR" \
	-Xbootclasspath/p:"$CRIJ_JAR" \
	-javaagent:"$LAB_JAR" \
	-javaagent:"$CORE_JAR" \
	-Xbootclasspath/p:"$CORE_JAR:$CORE/asm-all-5.0.3.jar" \
	-Dgroucho.config.file.location="$CONFIG" \
	-cp "$LAB/test-classes:$LAB/classes:$LAB/dependency/*" \
	it.cnr.iasi.saks.groucho.demoPacman.PacmanGuiMain "$PROBABILITY" 2>&1 \
	| tee "$LOG" \
	| grep --line-buffered -E '^\[demoPacman\]|^\[InvivoSuite\]|^Exception in|^Caused by'
