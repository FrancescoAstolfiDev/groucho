#!/usr/bin/env bash
#
# Avvia un'istanza di groucho-lsh-rest DEDICATA AI TEST, separata da quella degli esperimenti:
#   - porta diversa (predefinita 8081, la 8080 resta al server degli esperimenti);
#   - cartella di lavoro diversa: il server salva lo storico (bucket.data, count.data)
#     nella cartella da cui viene lanciato, quindi i due storici restano indipendenti.
#
# Viene avviato in automatico dalla suite AbstractGovernanceManagerHistoryTest_IT (@BeforeClass)
# e spento a fine suite (@AfterClass); si puo' anche lanciare a mano dalla cartella groucho-lab:
#        bash src/test/resources/scripts/start-lsh-test-server.sh [porta]
# Test:  JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn test -Dtest='GovernanceManager*Test_IT' -DfailIfNoTests=false
#        (su una porta diversa da 8081 aggiungere
#         -Dgroucho.lsh.rest_client.urlBasePath=http://localhost:<porta>/groucho-state-observer)

set -euo pipefail

PORT="${1:-8081}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# lo script sta in groucho/groucho-lab/src/test/resources/scripts
G="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
DATA_DIR="$G/lsh-test-server"

if [ "$PORT" = "8080" ]; then
	echo "La porta 8080 e' riservata al server degli esperimenti: sceglierne un'altra." >&2
	exit 1
fi

mkdir -p "$DATA_DIR"
cd "$DATA_DIR"

# shellcheck disable=SC1091
source "$G/lsh-invivo/.venv/bin/activate"

echo "groucho-lsh-rest di TEST su http://localhost:$PORT/groucho-state-observer (dati in $DATA_DIR)"
PYTHONPATH="$G/lsh-invivo/target/jep-inst" exec java \
	-Djava.library.path="$G/lsh-invivo/target/jep-inst/jep" \
	-jar "$G/groucho-lsh-rest/target/groucho-lsh-rest-1.0-SNAPSHOT-exec.jar" \
	--server.port="$PORT"
