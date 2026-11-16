#!/bin/bash
#
# Runs it.cnr.iasi.saks.invivoDemoByAnnotation.InvivoDemoMain on the CROCHET JRE with
# the same arguments used by failsafe in the root pom.
#
#   ./invivo-demo.sh           normal run
#   ./invivo-demo.sh debug     the JVM stops and waits for a debugger on localhost:5005
#                              (IntelliJ: Run > Edit Configurations > Remote JVM Debug)
#
# Prerequisite (with Java 8): mvn install -pl groucho-core,lsh-invivo,groucho-lsh-rest,groucho-lab -DskipTests
# The Governance Manager is set in the .properties file: groucho-lsh-rest must be running.

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CROCHET="$ROOT/groucho-crochet/target"
CORE="$ROOT/groucho-core/target"
LAB="$ROOT/groucho-lab/target"
CRIJ_JAR="$CROCHET/CRIJ-0.0.1-SNAPSHOT.jar"
CORE_JAR="$CORE/groucho-core-1.0-SNAPSHOT.jar"
JACKSON="$LAB/dependency/jackson-databind-2.11.0.jar:$LAB/dependency/jackson-core-2.11.0.jar:$LAB/dependency/jackson-annotations-2.11.0.jar"
# groucho-lsh-rest REST client and its dependencies, used by the Governance Managers
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
	-cp "$LAB/classes:$LAB/test-classes:$CORE/test-classes:$CORE/classes:$JACKSON:$LSH_CLIENT" \
	it.cnr.iasi.saks.invivoDemoByAnnotation.InvivoDemoMain
