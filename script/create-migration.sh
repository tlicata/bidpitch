#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Takes one parameter: the migration name"
    exit 1
fi

project_root=$(cd "$(dirname "$0")/.." && pwd)
timestamp=$(date '+%Y%m%d%H%M%S')
dir="${project_root}/migrations/"
name="$1"

if [ ! -d "$dir" ]; then
    echo "creating migrations directory in: $dir"
    mkdir "$dir"
fi

stamped="${timestamp}-${name}"
pathed="${dir}${stamped}"
up="${pathed}.up.sql"
down="${pathed}.down.sql"

echo "creating $up"
echo "creating $down"

touch "$up"
touch "$down"
