#!/bin/bash
curl -k -E clients/libe/libe.pem https://localhost:$(netstat -tlp 2>&1 | grep java | tr ' ' '\n' | sort | uniq | grep -e '^\[::\]:[0-9]' | sed 's/[^0-9]//g')/public/form -F 'toto=titi' -F 'tata=tutu;foo=bar'
echo
