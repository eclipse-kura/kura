#!/bin/bash
index=0;
IFS=''
cat "$1" |
while read -r line || [[ -n "$line" ]]; do
    if [[ $line == *"</plugin>"* ]]
      then
        if [[ $index == 1 ]];
          then
            echo -e "            </plugin>";
            echo -e "        </plugins>";
	        echo -e "    </build>";
            echo -e "</project>";
            exit 0;
          else
            index=$[index + 1];
            echo -e $line;
          fi
    else
      echo -e $line;
    fi
done < "$1"
