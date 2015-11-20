#!/bin/bash
while IFS='=' read -r name version
do
    if [[ $name != *"kura.version"* ]] && [[ $name != *"emulator"* ]] && [[ $name != *"test"* ]] && [[ $name != *"demo"* ]] && [[ $name != *"protocol"* ]] && [[ $name != *"examples"* ]]
      then
        echo "                                <artifactItem>" >> "$2";
        echo "                                    <groupId>org.eclipse.kura</groupId>" >> "$2";
        echo "                                    <artifactId>${name%.version}</artifactId>" >> "$2";
        echo "                                    <version>$version</version>" >> "$2";
        echo "                                </artifactItem>" >> "$2";
    fi
done < "$1"

echo "                          </artifactItems>
                            <stripVersion>true</stripVersion>
                            <outputDirectory>\${project.basedir}/target/source/plugins</outputDirectory>
                      </configuration>
                  </execution>
                </executions>
             </plugin>
        </plugins>
     </build>
</project>" >> "$2";
