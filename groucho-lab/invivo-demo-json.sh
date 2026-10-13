#!/bin/bash
#
# Lancia it.cnr.iasi.saks.invivoDemoJson.InvivoDemoJsonMain sulla JRE di CROCHET
# nella versione "JSON": Contatore non e' annotato nel sorgente, l'annotazione
# @TestableInVivo viene aggiunta a runtime dall'agent di groucho-lab leggendo
# src/test/resources/invivoDemoJson/model.json.
#
# Ordine degli agent (come nei profili degli esperimenti di groucho-lab):
#   1. CROCHET       checkpoint/rollback
#   2. groucho-lab   legge il JSON e aggiunge @TestableInVivo
#   3. groucho-core  vede l'annotazione e inietta la chiamata al test in-vivo
#
#   ./invivo-demo-json.sh           esecuzione normale
#   ./invivo-demo-json.sh debug     la JVM si ferma e attende un debugger su localhost:5005
#
# Prerequisito: mvn install -pl groucho-core,groucho-lab -DskipTests

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CROCHET="$ROOT/groucho-crochet/target"
CORE="$ROOT/groucho-core/target"
LAB="$ROOT/groucho-lab/target"
CRIJ_JAR="$CROCHET/CRIJ-0.0.1-SNAPSHOT.jar"
CORE_JAR="$CORE/groucho-core-1.0-SNAPSHOT.jar"
LAB_JAR="$LAB/groucho-lab-1.0-SNAPSHOT.jar"
JACKSON="$LAB/dependency/jackson-databind-2.11.0.jar:$LAB/dependency/jackson-core-2.11.0.jar:$LAB/dependency/jackson-annotations-2.11.0.jar"

case "$1" in
	"")    DEBUG_OPTS="" ;;
	debug) DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005" ;;
	*)     echo "Argomento non riconosciuto: '$1' (usa: debug oppure niente)" >&2; exit 1 ;;
esac

exec "$CROCHET/jre-inst/bin/java" $DEBUG_OPTS \
	-Xmx4G -agentpath:"$CROCHET/libtagging.so" \
	-javaagent:"$CRIJ_JAR" \
	-Xbootclasspath/p:"$CRIJ_JAR" \
	-javaagent:"$LAB_JAR" \
	-javaagent:"$CORE_JAR" \
	-Xbootclasspath/p:"$CORE_JAR:$CORE/asm-all-5.0.3.jar:$CRIJ_JAR" \
	-Dgroucho.config.file.location="$ROOT/groucho-lab/src/test/resources/invivoDemoJson/invivoDemoJson.properties" \
	-cp "$LAB/test-classes:$LAB/classes:$JACKSON" \
	it.cnr.iasi.saks.invivoDemoJson.InvivoDemoJsonMain
