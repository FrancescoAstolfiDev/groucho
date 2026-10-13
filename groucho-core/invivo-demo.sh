#!/bin/bash
#
# Lancia it.cnr.iasi.saks.invivoDemo.InvivoDemoMain sulla JRE di CROCHET con
# gli stessi argomenti usati da failsafe nel pom radice.
#
#   ./invivo-demo.sh           esecuzione normale
#   ./invivo-demo.sh debug     la JVM si ferma e attende un debugger su localhost:5005
#                              (IntelliJ: Run > Edit Configurations > Remote JVM Debug)
#
# Prerequisito: mvn -o -pl groucho-core verify (o almeno package + test-compile)

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CROCHET="$ROOT/groucho-crochet/target"
CORE="$ROOT/groucho-core/target"
CRIJ_JAR="$CROCHET/CRIJ-0.0.1-SNAPSHOT.jar"
CORE_JAR="$CORE/groucho-core-1.0-SNAPSHOT.jar"

DEBUG_OPTS=""
if [ "$1" == "debug" ]; then
	DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
fi

exec "$CROCHET/jre-inst/bin/java" $DEBUG_OPTS \
	-Xmx4G -agentpath:"$CROCHET/libtagging.so" \
	-javaagent:"$CRIJ_JAR" \
	-Xbootclasspath/p:"$CRIJ_JAR" \
	-javaagent:"$CORE_JAR" \
	-Xbootclasspath/p:"$CORE_JAR:$CORE/asm-all-5.0.3.jar:$CRIJ_JAR" \
	-Dgroucho.config.file.location="$ROOT/groucho-core/src/test/resources/performanceOverheadTest_IT.properties" \
	-cp "$CORE/test-classes:$CORE/classes" \
	it.cnr.iasi.saks.invivoDemo.InvivoDemoMain
