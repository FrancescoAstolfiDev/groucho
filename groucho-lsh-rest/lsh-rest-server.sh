#!/bin/bash
#
# Lancia il server REST di LSH (groucho-lsh-rest) in foreground, su Java 8.
# E' il server interrogato da LSHGovernanceManager durante le demo in vivo: Ctrl+C per fermarlo.
#
#   ./lsh-rest-server.sh                     esecuzione normale (porta 8080)
#   ./lsh-rest-server.sh build               ricompila lsh-invivo e groucho-lsh-rest prima di avviare
#   ./lsh-rest-server.sh debug               la JVM si ferma e attende un debugger su localhost:5006
#   ./lsh-rest-server.sh --server.port=9090  gli altri argomenti passano a Spring Boot
#   (build e debug si possono combinare: ./lsh-rest-server.sh build debug)
#
# Swagger UI: http://localhost:8080/groucho-state-observer/
#
# bucket.data e count.data vengono letti e scritti nella directory di lavoro (di default
# questa directory) e sopravvivono ai riavvii: si cambia con LSH_DATA_DIR, per azzerarli
# c'e' PUT /reset.
#
# Variabili d'ambiente:
#   JAVA8_HOME    JDK 8 da usare (default /usr/lib/jvm/java-8-openjdk-amd64)
#   LSH_DATA_DIR  directory di bucket.data e count.data

MODULE_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$MODULE_DIR/.." && pwd)"
REST_JAR="$MODULE_DIR/target/groucho-lsh-rest-1.0-SNAPSHOT-exec.jar"
JEP_INST="$ROOT/lsh-invivo/target/jep-inst"
JAVA_HOME_8="${JAVA8_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}"
JAVA_BIN="$JAVA_HOME_8/bin/java"
DATA_DIR="${LSH_DATA_DIR:-$MODULE_DIR}"

BUILD=0
DEBUG_OPTS=""
while [ $# -gt 0 ]; do
	case "$1" in
		build) BUILD=1; shift ;;
		debug) DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006"; shift ;;
		*) break ;;
	esac
done

if [ "$BUILD" -eq 1 ]; then
	(cd "$ROOT" && JAVA_HOME="$JAVA_HOME_8" mvn -q install -pl lsh-invivo,groucho-lsh-rest -am -DskipTests) || {
		echo "Build fallita" >&2
		exit 1
	}
fi

for f in "$JAVA_BIN" "$REST_JAR" "$JEP_INST/jep/libjep.so"; do
	if [ ! -e "$f" ]; then
		echo "File mancante: $f" >&2
		echo "Compila con: ./lsh-rest-server.sh build" >&2
		exit 1
	fi
done

mkdir -p "$DATA_DIR" && cd "$DATA_DIR" || exit 1

export PYTHONPATH="$JEP_INST${PYTHONPATH:+:$PYTHONPATH}"

exec "$JAVA_BIN" $DEBUG_OPTS \
	-Djava.library.path="$JEP_INST/jep/" \
	-jar "$REST_JAR" "$@"
