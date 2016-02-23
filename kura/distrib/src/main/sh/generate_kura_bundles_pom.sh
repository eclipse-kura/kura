#!/bin/bash
while IFS='=' read -r name version
do
    if [[ $name != *"kura.version"* ]] && [[ $name != *"emulator"* ]] && [[ $name != *"test"* ]] && [[ $name != *"demo"* ]] && [[ $name != *"protocol"* ]] && [[ $name != *"examples"* ]] && [[ $name != *"example"* ]] && [[ $name != *"web"* ]]
      then
        echo "                                <artifactItem>";
        echo "                                    <groupId>org.eclipse.kura</groupId>";
        echo "                                    <artifactId>${name%.version}</artifactId>";
        echo "                                    <version>$version</version>";
        echo "                                </artifactItem>";
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
</project>";
