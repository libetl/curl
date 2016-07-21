#!/bin/bash
curl -k --cert client.pem:password https://localhost:$(netstat -tlp 2>&1 | grep java | tr ' ' '\n' | sort | uniq | grep -e '^\[::\]:[0-9]' | sed 's/[^0-9]//g')/public/
echo
