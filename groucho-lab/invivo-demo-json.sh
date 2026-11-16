#!/bin/bash
#
# Runs it.cnr.iasi.saks.invivoDemoByJsonProperties.InvivoDemoJsonMain on the CROCHET JRE
# in the "JSON" version: Counter is not annotated in the source, the @TestableInVivo
# annotation is added at runtime by the groucho-lab agent, which reads
# src/test/resources/invivoDemoJson/model.json.
#
# Order of the agents (as in the groucho-lab experiment profiles):
#   1. CROCHET       checkpoint/rollback
#   2. groucho-lab   reads the JSON and adds @TestableInVivo
#   3. groucho-core  sees the annotation and injects the call to the in-vivo test
#
#   ./invivo-demo-json.sh           normal run
#   ./invivo-demo-json.sh debug     the JVM stops and waits for a debugger on localhost:5005
#
# Prerequisite: mvn install -pl groucho-core,lsh-invivo,groucho-lsh-rest,groucho-lab -DskipTests

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CROCHET="$ROOT/groucho-crochet/target"
CORE="$ROOT/groucho-core/target"
LAB="$ROOT/groucho-lab/target"
CRIJ_JAR="$CROCHET/CRIJ-0.0.1-SNAPSHOT.jar"
CORE_JAR="$CORE/groucho-core-1.0-SNAPSHOT.jar"
LAB_JAR="$LAB/groucho-lab-1.0-SNAPSHOT.jar"
JACKSON="$LAB/dependency/jackson-databind-2.11.0.jar:$LAB/dependency/jackson-core-2.11.0.jar:$LAB/dependency/jackson-annotations-2.11.0.jar"
# groucho-lsh-rest REST client and its dependencies, used by the Governance Managers
LSH_CLIENT="$ROOT/groucho-lsh-rest/target/groucho-lsh-rest-1.0-SNAPSHOT.jar:$ROOT/lsh-invivo/target/lsh-invivo-1.0-SNAPSHOT.jar"
for dep in spring-web-5.2.5.RELEASE spring-core-5.2.5.RELEASE spring-beans-5.2.5.RELEASE spring-jcl-5.2.5.RELEASE threetenbp-1.3.1 jackson-datatype-threetenbp-2.6.4; do
	LSH_CLIENT="$LSH_CLIENT:$LAB/dependency/$dep.jar"
done

case "$1" in
	"")    DEBUG_OPTS="" ;;
	debug) DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005" ;;
	*)     echo "Unrecognised argument: '$1' (use: debug or nothing)" >&2; exit 1 ;;
esac

exec "$CROCHET/jre-inst/bin/java" $DEBUG_OPTS \
	-Xmx4G -agentpath:"$CROCHET/libtagging.so" \
	-javaagent:"$CRIJ_JAR" \
	-Xbootclasspath/p:"$CRIJ_JAR" \
	-javaagent:"$LAB_JAR" \
	-javaagent:"$CORE_JAR" \
	-Xbootclasspath/p:"$CORE_JAR:$CORE/asm-all-5.0.3.jar:$CRIJ_JAR" \
	-Dgroucho.config.file.location="$ROOT/groucho-lab/src/test/resources/invivoDemoJson/invivoDemoJson.properties" \
	-cp "$LAB/test-classes:$LAB/classes:$JACKSON:$LSH_CLIENT" \
	it.cnr.iasi.saks.invivoDemoByJsonProperties.InvivoDemoJsonMain
