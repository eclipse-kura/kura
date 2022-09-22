# System Testing

## QA Procedure

A set of automated and manual test are performed before releasing a new Eclipse&trade; Kura version to ensure software follows our quality standards. 

Once a Release Candidate (RC) is tagged on its maintenance branch the QA process starts. The QA process involves a set of automated and manual tests performed on the target environment listed [below](#environment). These tests are updated continuosly to follow the large amount of features added in each release. The QA process continues with new Release Candidate builds until the amount of defects in the software is reduced. When this happens the RC is promoted to final release and tagged on the maintenance branch.

## Environment

### Hardware

* Raspberry Pi 3/4
* Intel Up2
* Nvidia Jetson Nano
* Docker

### OS

* Raspberry Pi OS
* Ubuntu 20.04
* Ubuntu 18.04
* CentOS 7

### Java

* Eclipse Adoptium Temurin&trade; JDK 1.8
