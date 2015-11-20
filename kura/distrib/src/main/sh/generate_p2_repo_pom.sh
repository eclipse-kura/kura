#!/bin/bash
index=0;
IFS=''
cat "$1" |
while read -r line || [[ -n "$line" ]]; do
    if [[ $line == *"</plugin>"* ]]
      then
        if [[ $index == 1 ]];
          then
            echo -e "</plugin>" >> "$2";
            echo -e "</plugins>" >> "$2";
	          echo -e "</build>" >> "$2";
            echo -e "</project>" >> "$2";
            exit 0;
          else
            index=$[index + 1];
            echo -e $line >> "$2";
          fi
    else
      echo -e $line >> "$2";
    fi
done < "$1"
