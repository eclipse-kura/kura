This is an experimental Karaf setup.

# Building a Karaf distribution

* Be sure to have run a full build of Kura before in order to have
  all the Maven artifacts installed in your local Maven repository.
  You can use `cd .. ; ./build-all.sh`
  
* Run `mvn clean install` in order to generate a Karaf distribution in `emulator-instance`

* Run `cd emulator-instance; mvn exec:java -Prun` in order to run the local Karaf distribution

# Building Karaf based targets

** This is extra-experimental and subject to change **


    cd deployment
    mvn clean package
