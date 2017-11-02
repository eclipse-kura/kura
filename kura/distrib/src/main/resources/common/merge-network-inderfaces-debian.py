#!/usr/bin/python
import sys, os, socket
from optparse import OptionParser

DFLT_SRC_NET_IFACES_FILE_LINUX = "/etc/network/interfaces"
DFLT_SRC_NET_IFACES_FILE_KURA = "/tmp/network.interfaces.kura"
DFLT_DST_NET_IFACES_FILE = "/etc/network/interfaces"

def enum(**enums):
    return type('Enum', (), enums)

BootProtocol = enum(NONE='manual', STATIC='static', DHCP='dhcp')

class NetInterface:
    
    def __init__(self, ifaceName):
	self.__name = ifaceName
	self.__auto = False
	self.__bootproto = BootProtocol.NONE
	self.__address = ""
	self.__netmask = ""
	self.__network = ""
	self.__broadcast = ""
	self.__gateway = ""
	self.__commands = []

    def __isValidIpAddress(self, ipAddress):
	try:
	    socket.inet_aton(ipAddress)
	    ret = True
	except socket.error:
	    ret = False
	return ret
    
    # getters
    def getName(self):
	return self.__name
    def getAuto(self):
	return self.__auto
    def getBootProtocol(self):
	return self.__bootproto
    def getIpAddress(self):
	return self.__address
    def getNetmask(self):
	return self.__netmask
    def getNetwork(self):
	return self.__network
    def getBroadcast(self):
	return self.__broadcast
    def getGateway(self):
	return self.__gateway
    def getCommands(self):
	return self.__commands

    # setters
    def setAuto(self, auto):
	self.__auto = auto
    def setBootProtocol(self, bootProtocol):
	self.__bootproto = bootProtocol
    def setIpAddress(self, ipAddress):
	if self.__isValidIpAddress(ipAddress):
	    self.__address = ipAddress
    def setNetmask(self, netmask):
	if self.__isValidIpAddress(netmask):
	    self.__netmask = netmask
    def setNetwork(self, network):
	if self.__isValidIpAddress(network):
	    self.__network = network
    def setBroadcast(self, broadcast):
	if self.__isValidIpAddress(broadcast):
	    self.__broadcast = broadcast
    def setGateway(self, gateway):
	if self.__isValidIpAddress(gateway):
	    self.__gateway = gateway
    def addCommand(self, command):
	self.__commands.append(command)

    def toString(self):
	ret = ""
	if self.__auto:
	    ret += "auto " + self.__name + "\n"
	ret += "iface " + self.__name + " inet " + self.__bootproto
	if self.__bootproto == BootProtocol.STATIC:
	    if len(self.__address) > 0:
		ret += "\n\taddress " + self.__address
	    if len(self.__netmask) > 0:
		ret += "\n\tnetmask " + self.__netmask
	    if len(self.__network) > 0:
		ret += "\n\tnetwork " + self.__network
	    if len(self.__broadcast) > 0:
		ret += "\n\tbroadcast " + self.__broadcast
	    if len(self.__gateway) > 0:
		ret += "\n\tgateway " + self.__gateway
	for cmd in self.__commands:
	    ret += "\n\t" + cmd		
	ret += "\n\n"
	return ret

def readIfaceConfigFile(filename):
    with open(filename) as f:
	fileContent = f.readlines()
    f.close()
    fileContent = [x.strip() for x in fileContent]
    return fileContent 

def parseIfaceConfigFile(fileContent):
    ifaces = []
    iface = None
    for i in range(0, len(fileContent)):
	if fileContent[i].startswith("auto"):
	    tokens = fileContent[i].split(" ")
	    iface = NetInterface(tokens[1])
	    iface.setAuto(True);
	elif fileContent[i].startswith("iface"):
	    tokens = fileContent[i].split(" ")
	    if iface is None:
		iface = NetInterface(tokens[1])
		iface.setAuto(False);
	    iface.setBootProtocol(tokens[3])
	elif len(fileContent[i]) == 0:
	    if not iface is None:
		ifaces.append(iface)
		iface = None;
	else:
	    # parsing interface configuration
	    if fileContent[i].startswith("address"):
		tokens = fileContent[i].split(" ")
		iface.setIpAddress(tokens[1])
	    elif fileContent[i].startswith("netmask"):
		tokens = fileContent[i].split(" ")
		iface.setNetmask(tokens[1])
	    elif fileContent[i].startswith("network"):
		tokens = fileContent[i].split(" ")
		iface.setNetwork(tokens[1])
	    elif fileContent[i].startswith("broadcast"):
		tokens = fileContent[i].split(" ")
		iface.setBroadcast(tokens[1])
	    elif fileContent[i].startswith("gateway"):
		tokens = fileContent[i].split(" ")
		iface.setGateway(tokens[1])
	    else:
		if not fileContent[i].startswith("#"):
		    iface.addCommand(fileContent[i]) 
    if not iface is None:
	ifaces.append(iface)
    return ifaces

def getIface(name, ifaces):
    ret = None
    for iface in ifaces:
	if name == iface.getName():
	    ret = iface
	    break
    return ret

def createNetIfaceConfigFile(filename, ifaces):
    f = open(filename, 'w')
    f.write("# /etc/network/interfaces -- configuration file for ifup(8), ifdown(8)\n\n")
    for iface in ifaces:
	if iface.getName() == "lo":
	    f.write("# The loopback interface\n")
	elif iface.getName().startswith("e"):
	    f.write("# Wired interface - {}\n".format(iface.getName()))
	elif  iface.getName().startswith("w"):
	    f.write("# Wireless interface - {}\n".format(iface.getName()))
	f.write(iface.toString())
    f.close()

def displayInterfaces(ifaces):
    for iface in ifaces:
	print iface.toString()

parser = OptionParser()
parser.add_option("--srcLinux", type="string", help="Inteface configuration file provided by Linux", dest="srcFlOS", default=DFLT_SRC_NET_IFACES_FILE_LINUX)
parser.add_option("--srcKura", type="string", help="Inteface configuration file provided by Kura", dest="srcFlKura", default=DFLT_SRC_NET_IFACES_FILE_KURA)
parser.add_option("--dstFile", type="string", help="Merged inteface configuration file", dest="dstFl", default=DFLT_DST_NET_IFACES_FILE)
(options, args) = parser.parse_args(sys.argv)

netIfacesOsContent = readIfaceConfigFile(options.srcFlOS)
netIfacesKuraContent = readIfaceConfigFile(options.srcFlKura)
osIfaces = parseIfaceConfigFile(netIfacesOsContent)
kuraIfaces = parseIfaceConfigFile(netIfacesKuraContent)

#print "\nOS INTERFACES:"
#displayInterfaces(osIfaces)
#print "\nKura INTERFACES:"
#displayInterfaces(kuraIfaces)

ifacesToAdd = []
for iface in osIfaces:
    if getIface(iface.getName(), kuraIfaces) is None:
	ifacesToAdd.append(iface)
for iface in ifacesToAdd:
    kuraIfaces.append(iface)

#print "\nResulting Kura INTERFACES:"
#displayInterfaces(kuraIfaces)

createNetIfaceConfigFile(options.dstFl, kuraIfaces)
