# Shared settings for the demo scripts.
GEOSERVER_URL="${GEOSERVER_URL:-http://localhost:9090/geoserver/cloud}"
REST="$GEOSERVER_URL/rest"
AUTH="${GEOSERVER_AUTH:-admin:geoserver}"
WORKSPACE="${WORKSPACE:-demo}"
STORE="${STORE:-landshallow}"
COVERAGE="${COVERAGE:-landshallow}"
S3_INTERNAL_BASE="${S3_INTERNAL_BASE:-http://s3proxy/cogs}"

gs() {
    local method="$1"; shift
    curl -sS -u "$AUTH" -X "$method" "$@"
}

# describe <title>  (description prose piped in on stdin)
# Prints a titled banner followed by the piped description, for the
# end-to-end use case runners.
describe() {
    echo
    echo "########################################################################"
    echo "# $1"
    echo "########################################################################"
    cat
    echo
}
