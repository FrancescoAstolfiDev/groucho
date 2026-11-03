#!/bin/bash
#
# Lancia it.cnr.iasi.saks.invivoDemoByAnnotation.InvivoDemoMain sulla JRE di CROCHET con
# gli stessi argomenti usati da failsafe nel pom radice.
#
#   ./invivo-demo.sh           esecuzione normale
#   ./invivo-demo.sh debug     la JVM si ferma e attende un debugger su localhost:5005
#                              (IntelliJ: Run > Edit Configurations > Remote JVM Debug)
#
# Prerequisito (con Java 8): mvn install -pl groucho-core,lsh-invivo,groucho-lsh-rest,groucho-lab -DskipTests
# Il Governance Manager e' LSHGovernanceManager: groucho-lsh-rest deve essere in esecuzione.

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CROCHET="$ROOT/groucho-crochet/target"
CORE="$ROOT/groucho-core/target"
LAB="$ROOT/groucho-lab/target"
CRIJ_JAR="$CROCHET/CRIJ-0.0.1-SNAPSHOT.jar"
CORE_JAR="$CORE/groucho-core-1.0-SNAPSHOT.jar"
JACKSON="$LAB/dependency/jackson-databind-2.11.0.jar:$LAB/dependency/jackson-core-2.11.0.jar:$LAB/dependency/jackson-annotations-2.11.0.jar"
# Client REST di groucho-lsh-rest e sue dipendenze, usati da LSHGovernanceManager
LSH_CLIENT="$ROOT/groucho-lsh-rest/target/groucho-lsh-rest-1.0-SNAPSHOT.jar:$ROOT/lsh-invivo/target/lsh-invivo-1.0-SNAPSHOT.jar"
for dep in spring-web-5.2.5.RELEASE spring-core-5.2.5.RELEASE spring-beans-5.2.5.RELEASE spring-jcl-5.2.5.RELEASE threetenbp-1.3.1 jackson-datatype-threetenbp-2.6.4; do
	LSH_CLIENT="$LSH_CLIENT:$LAB/dependency/$dep.jar"
done

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
	-Dgroucho.config.file.location="$ROOT/groucho-lab/src/test/resources/invivoDemoAnnotation/invivoDemoAnnotation.properties" \
	-cp "$LAB/test-classes:$CORE/test-classes:$CORE/classes:$JACKSON:$LSH_CLIENT" \
	it.cnr.iasi.saks.invivoDemoByAnnotation.InvivoDemoMain
