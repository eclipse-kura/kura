#/bin/sh
cd $HOME
sudo rm -r ifconfig.txt
sudo rm -r route.txt
sudo ifconfig eth0 down
sleep 30s
ifconfig eth0 > ifconfig.txt
route > route.txt
sudo reboot