#!/bin/bash

OPTIND=1         # Reset in case getopts has been used previously in the shell.

INDEX_NAME='wmind_search_01'
ES_HOST='localhost:9200'
SET_MAP='set-map.json'

function help {
    echo "Usage : $0 [options]"
    echo "    -h|-?                 help"
    echo "    -H {host:port}        host:port. \"localhost:9200\" is default "
    echo "    -i {index}            index"
    echo "    -s {set-map_file}     setting & mapping json file"
    echo ""
}

if [ "$#" -eq 0 ]; then
   help
   exit;
fi

while getopts "H:i:s:" opt; do
    case "$opt" in
    \h|\?)
        help
        exit 0
        ;;
    H) ES_HOST=$OPTARG
        ;;
    i) INDEX_NAME=$OPTARG
        ;;
    s) SET_MAP=$OPTARG
        ;;
    :)
        echo "Option -$OPTARG requires an argument." >&2
        echo ""
        help
        exit 0
        ;;
    esac
done

shift $((OPTIND-1))
[ "$1" = "--" ] && shift

echo '* remove old index'
curl -X DELETE http://${ES_HOST}/${INDEX_NAME}?pretty
echo
echo


echo '* create index'
curl -H 'Content-Type: application/json' -XPUT http://${ES_HOST}/${INDEX_NAME}?pretty --data-binary @${SET_MAP}
echo
echo


echo '* index info'
curl -XGET http://${ES_HOST}/${INDEX_NAME}?pretty
echo

echo '* bulk insert'
curl -H 'Content-Type: application/json' -X POST http://${ES_HOST}/_bulk?pretty --data-binary @test_collection.json
echo
