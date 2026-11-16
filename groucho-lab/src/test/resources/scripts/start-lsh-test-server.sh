#!/usr/bin/env bash
#
# Starts a groucho-lsh-rest instance DEDICATED TO THE TESTS, separate from the experiment one:
#   - different port (8081 by default, 8080 stays with the experiment server);
#   - different working directory: the server stores its history (bucket.data, count.data)
#     in the directory it is started from, so the two histories stay independent.
#
# It is started automatically by the AbstractGovernanceManagerHistoryTest_IT suite (@BeforeClass)
# and stopped at the end of the suite (@AfterClass); it can also be run by hand from groucho-lab:
#        bash src/test/resources/scripts/start-lsh-test-server.sh [port]
# Test:  JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn test -Dtest='GovernanceManager*Test_IT' -DfailIfNoTests=false
#        (on a port other than 8081 add
#         -Dgroucho.lsh.rest_client.urlBasePath=http://localhost:<port>/groucho-state-observer)

set -euo pipefail

PORT="${1:-8081}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# the script lives in groucho/groucho-lab/src/test/resources/scripts
G="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
DATA_DIR="$G/lsh-test-server"

if [ "$PORT" = "8080" ]; then
	echo "Port 8080 is reserved for the experiment server: choose another one." >&2
	exit 1
fi

mkdir -p "$DATA_DIR"
cd "$DATA_DIR"

# shellcheck disable=SC1091
source "$G/lsh-invivo/.venv/bin/activate"

echo "TEST groucho-lsh-rest on http://localhost:$PORT/groucho-state-observer (data in $DATA_DIR)"
PYTHONPATH="$G/lsh-invivo/target/jep-inst" exec java \
	-Djava.library.path="$G/lsh-invivo/target/jep-inst/jep" \
	-jar "$G/groucho-lsh-rest/target/groucho-lsh-rest-1.0-SNAPSHOT-exec.jar" \
	--server.port="$PORT"
