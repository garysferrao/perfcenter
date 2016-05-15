#!/usr/bin/python2.7
import os, sys, random
from array import array
from copy import deepcopy, copy

class DevType:
    CPU = 1
    NONCPU = 2

class DeviceCategory:
    def __init__(self):
      self.name = ""
      self.devtype = DevType.CPU

    def __init__(self, _name, _type):
      self.name = _name
      self.devtype = _type

    def __str__(self):
        return "\t" + ("cpu" if self.devtype == 1 else "noncpu") + " " + self.name
    

class Device:
    def __init__(self):
        self.name = ""
        self.devcat = None

    def __init__(self, _name, _devcat):
        self.name = _name
        self.devcat = _devcat

    def __str__(self):
        return "\t" + self.devcat.name + " " + self.name


class SubTaskServiceTime:
    def __init__(self):
        self.devcat = None 
        self.servt = 0.0
        self.basespeed = -1.0

    def __init__(self, _devcat, _servt):
        self.devcat = _devcat
        self.servt = _servt
        selt.basespeed = -1.0

    def __init__(self, _devcat, _servt, _basespeed):
        self.devcat = _devcat
        self.servt = _servt
        self.basespeed = _basespeed

    def __str__(self):
        return self.devcat.name + " servt " + str(self.servt) + " at " + str(self.basespeed if self.basespeed else "")

    __repr__ = __str__


class Task:
    def __init__(self, name):
        self.name = name
        self.server = "" 
        self.subtasks = []

    def __str__(self):
        #s = "task " + self.name + " " + str(len(self.subtasks)) + "\n"
        s = "task " + self.name + "\n"
        for subtask in self.subtasks:
            s += ("\t" + str(subtask) + "\n")
        s += "end"
        return s

    __repr__ = __str__

class SoftServer:
    def __init__(self):
        self.name = ""
        self.count = 1
        self.buf = 1
        self.schedp = "fcfs"
        self.tasks = dict()
        self.machineName = ""
        self.isDummy = False
        self.pm = None

    def __str__(self):
        s = "server " + self.name + "\n"
        s += "\tthread count " + str(self.count) + "\n"
        s += "\tthread buffer " + str(self.buf) + "\n"
        s += "\tthread schedp " + self.schedp + "\n"
        for taskname in self.tasks:
            s += "\ttask " + taskname + "\n"
        s += "end\n"
        return s

    __repr__ = __str__

class MessagePassed:
    def __init__(self, _fromTask, _toTask, _pktSz, _isSync):
        self.fromTask = _fromTask
        self.toTask = _toTask
        self.pktSz = _pktSz
        self.isSync = _isSync
        self.modified = False

    def __str__(self):
        s = "\t" + str(self.fromTask) + " " + str(self.toTask) + " " + str(self.pktSz) + (" sync" if self.isSync else "") + "\n"
        return s

    __repr__ = __str__


class Scenario:
    def __init__(self):
        self.name = ""
        self.prob = 0.0
        self.msgSeq = []

    def __str__(self):
        s = "scenario " + self.name + " prob " + str(self.prob) + "\n"
        for msg in self.msgSeq:
            s += str(msg)
        s += "end\n"
        return s
    __repr__ = __str__

#This is similar to device, it is required to store details about device in physical machine definition
class Hw:
    def __init__(self):
        self.name = ""
        self.count = 0
        self.buf = 0
        self.schedP = ""

    def __init__(self, _name):
        self.name = _name
        self.count = 0
        self.buf = 0
        self.schedP = ""

    def __str__(self):
        s = '\t' + self.name + " count " + str(self.count) + '\n'
        s += '\t' + self.name + " buffer " + str(self.buf) + '\n'
        s += '\t' + self.name + " schedP " + str(self.schedP) + '\n'
        return s


class PhysicalMachine:
    def __init__(self):
        self.name = ""
        self.hwMap = dict()
        self.virtSupported = "no"       #This is kept as string literal instead of boolean so that in future specific virtualization type can be stored in it 

    def __init__(self, _name):
        self.name = _name
        self.hwMap = dict()
        self.virtSupported = "no"

    def __str__(self):
        s = "physicalmachine " + self.name + '\n'
        for k, v in self.hwMap.iteritems():
            s +=  str(v) 
        if self.virtSupported != "no":
            s += '\t' + "virtualization " + self.virtSupported + "\n"
        s += "end\n"
        return s

class VirtualMachine:
    def __init__(self):
        self.name = ""
        self.vhwMap = []
        self.deployedOn = ""

    def __init__(self, _name):
        self.name = _name
        self.vhwMap = []
        self.deployedOn = ""

    def __str__(self):
        s = "virtualmachine " + self.name + '\n'
        for k, v in self.vhwMap.iteritems():
            s += str(v)
        s += "end\n"
        return s

class Lan:
    def __init__(self, _name):
        self.name = _name
    def __str__(self):
        s = self.name
        return s

class Variable:
    def __init__(self, _name, _val):
        self.name = _name
        self.val = _val
    def __str__(self):
        s = self.name
        s += " " + self.val
        return s

class System:
    def __init__(self):
        self.devcatMap = dict()
        self.pdeviceMap = dict()
        self.vdeviceMap = dict()
        self.taskMap = dict()
        self.serverMap = dict() 
        self.pmMap = dict() 
        self.pmGrpMap = dict()
        self.vmMap = dict() 
        self.vmGrpMap = dict()
        self.scenarioMap = dict() 
        self.lanMap = dict()
        self.deployment = dict()
        self.transformed = False

    def taskExists(self,_name):
        if _name in self.taskMap():
            return true
        else:
            print "Task " + _name + " does not exist."
            return false

    def serverExists(self,_name):
        if _name in self.serverMap():
            return true
        else:
            return false

    def getCpuDevCat(self,pmname):
        cpudevcats = []
        for hwname, hw in self.pmMap[pmname].hwMap.iteritems():
            if self.pdeviceMap[hwname].devcat.devtype == DevType.CPU: 
                return self.pdeviceMap[hwname].devcat
        return None

    def isDeployedOnVm(self,sname):
        if self.vmMap.get(self.deployment[sname]) is not None:
            return True
        if self.pmMap.get(self.deployment[sname]) is not None:
            return False 
        print "Not able to verify deployment of " + sname 
        print "==Deployment=="
        print self.deployment
        print "==pms=="
        printMap(self.pmMap)
        print "==vms=="
        printMap(self.vmMap)
        sys.exit(1)

    def printWhole(self):
        print "==Device Category=="
        printMap(self.devcatMap)
        print "==PDevices=="
        printMap(self.pdeviceMap)
        print "==VDevices=="
        printMap(self.vdeviceMap)
        print "==Tasks=="
        printMap(self.taskMap)
        print "==Servers=="
        printMap(self.serverMap)
        print "==pms=="
        printMap(self.pmMap)
        print "==vms=="
        printMap(self.vmMap)
        print "==scenario=="
        printMap(self.scenarioMap)
        print "==Deployment=="
        print self.deployment

    def __str__(self):
        s = "devicecategory\n"
        s += dictStitch(self.devcatMap)
        s += "end\n\n"
        if self.transformed:
            s += "pdevice\n"
            s += dictStitch(self.pdeviceMap)
            s += "end\n\n"
        else:
            s += "pdevice\n"
            s += dictStitch(self.pdeviceMap)
            s += "end\n\n"
            s += "vdevice\n"
            s += dictStitch(self.vdeviceMap)
            s += "end\n\n"
        for k,v in self.taskMap.iteritems():
            s += str(v) + "\n\n"
        for k,v in self.serverMap.iteritems():
            s += str(v) + "\n\n"
        for k,v in self.pmGrpMap.iteritems():
            pmname = k + "[" + str(v) + "]"
            s += str(self.pmMap[pmname]) + "\n\n"
        if not self.transformed:
            for k,v in self.vmGrpMap.iteritems():
                vmname = k + "[" + str(v) + "]"
                print k,v 
                #s += str(self.vmMap[vmname]) + "\n\n"
        for k,v in self.scenarioMap.iteritems():
            s += str(v) + "\n\n"
        s += "lan\n"
        for k,v in self.lanMap.iteritems():
            s += "\t" + str(v) + "\n"
        s += "end\n\n"
        for k,v in self.deployment.iteritems():
            s += "deploy " + k + " " + v + "\n"
        return s

    __repr__ = __str__

class Config:
    def __init__(self):
        self.distSys = System()
        self.modelParamStr = ""
        self.loadParamStr = ""
        self.varMap = dict()
        self.statStr = ""
    def __str__(self):
        s = ""
        if len(self.varMap) > 0:
            s += "variable\n"
            for k,v in self.varMap.iteritems():
                s += "\t" + str(v) + "\n"
            s += "end\n\n"
        s += str(self.distSys)
        s += "modelparams\n"
        s += self.modelParamStr
        s += "end\n\n"
        s += "loadparams\n"
        s += self.loadParamStr
        s += "end\n\n"
        s +=  self.statStr
        return s

#Construct values of dict separated by newline
def removeBrackets(s):
    s = s.replace('[','')
    s = s.replace(']','')
    return s

def dictStitch(d):
    s = ""
    for k,v in d.iteritems():
        s += str(v) + "\n"
    return s

#Used for getting body of different structures like task, server, scenario from filestream
def getBody(filestream):
    body = []
    for line in filestream:
        tokens = line.split()
        if len(tokens) > 0 :
            if tokens[0] == "end":
                break
        else:
            continue
        body.append(line)
    return body

#Same as above, just used for getting body from list of strings
def getBodyFromList(filelines, idx):
    body = []
    while idx < len(filelines):
        tokens = filelines[idx].split()
        if len(tokens) > 0 :
            if tokens[0] == "end":
                break
        else:
            continue
        body.append(filelines[idx])
        idx += 1
    return body, idx

#Debugging statement switch
def debug(s):
    if True:
        print s

def printMap(m):
    for k,v in m.iteritems():
        print str(v)

####Functions for parsing specific structures
def parseDevcat(body):
    devcatMap = dict()
    for line in body:
        tokens = line.split()
        if tokens[0].lower() == "cpu":
            devcat = DeviceCategory(tokens[1], DevType.CPU)
        elif tokens[0].lower() == "noncpu":
            devcat = DeviceCategory(tokens[1], DevType.NONCPU)
        devcatMap[tokens[1]] = devcat
    return devcatMap

def parseDevice(body, isVirtual):
    vdeviceMap = dict()
    pdeviceMap = dict()
    for line in body:
        tokens = line.split()
        if tokens[0] in system.devcatMap:
            device = Device(tokens[1], system.devcatMap[tokens[0]])
            if isVirtual:
                vdeviceMap[tokens[1]] = device
            else:
                pdeviceMap[tokens[1]] = device
        else:
            device = None
            sys.stderr.write("There doesn't exist any device category with name " + tokens[0])
            #sys.stderr.write("There doesn't exist any device with name " + (tokens[0] if len(tokens) > 0 else "None") + "\n")
    return (vdeviceMap if isVirtual else pdeviceMap)

def parseTask(body,_name):
    task = Task(_name)
    for line in body:
        words = line.split()
        if len(words) > 0: 
            if words[0] in system.devcatMap:
                devcat = system.devcatMap[words[0]] 
                if len(words) == 3:
                    stst = SubTaskServiceTime(devcat, words[2]) #cpu1 servt 0.2 
                    task.subtasks.append(stst)
                elif len(words) == 5:
                    stst = SubTaskServiceTime(devcat, words[2], words[4]) #cpu1 servt 0.2 at 2.8
                    task.subtasks.append(stst)
            else:
                device = None
                sys.stderr.write("There doesn't exist any device with name " + (words[0] if len(words) > 0 else "None") + "\n")
        #else ##error   #later
    return task


def parseScenario(body, _name, _prob):
    sce = Scenario()
    sce.name = _name
    sce.prob = _prob
    for line in body:
        words = line.split()
        msgPassed = MessagePassed("", "", 0, False)
        if len(words) >= 2:
          msgPassed.fromTask = words[0] 
          msgPassed.toTask = words[1] 
          if len(words) == 3:
              msgPassed.pktSz = words[2]
          elif len(words) == 4:
              msgPassed.isSync = True 
          sce.msgSeq.append(msgPassed)
        else:
            sys.stderr.write("Parsing msgparsed:" + line)
    return sce


def parseServer(body, _name, taskMap):
    server = SoftServer()
    server.name = _name
    for line in body:
        words = line.split()
        if len(words) > 0 and words[0] == "thread":
            if words[1] == "count":
                server.count = int(words[2])
            elif words[1] == "buffer":
                server.buffer = int(words[2])
            elif words[1] == "schedP":
                server.schedp = words[2]
        elif len(words) > 0 and words[0] == "task":
            if words[1] in taskMap:
                server.tasks[taskMap[words[1]].name] = True
                taskMap[words[1]].server = server.name
            else:
                sys.stderr.write("Make sure that tasks are declared first before softServers. Problem in line:" + line)
    return server

def parseMName(mname):
    idx = -1
    strnum = "" 
    if mname[idx] == ']':
        idx -= 1
        while mname[idx] != '[':
            strnum = mname[idx] + strnum
            idx -= 1
    num = int(strnum) 
    mname = mname[0:idx]
    return mname, num

def parsePmVm(body, _name, isVm):
    _name, num = parseMName(_name)
    i = 0
    pms = []
    vms = []
    hwMap = dict()
    if isVm:
        system.vmGrpMap[_name] = num
    else:
        system.pmGrpMap[_name] = num
    extra = []
    virtSupported = "no"
    while i < len(body):
        tokens = body[i].split() 
        if len(tokens) == 0:
            continue
        if (isVm and tokens[0] in system.vdeviceMap) or (not isVm and tokens[0] in system.pdeviceMap):
            if tokens[0] not in hwMap:
                hwMap[tokens[0]] = Hw(tokens[0])
            if tokens[1].lower() == "count" :
                hwMap[tokens[0]].count = int(tokens[2])
            elif tokens[1].lower() == "buffer" :
                hwMap[tokens[0]].buf = int(tokens[2])
            elif tokens[1].lower() == "schedp" :
                hwMap[tokens[0]].schedP = tokens[2]
        else:
            tokens[0] == "virtualization"
            virtSupported = tokens[1] 
        i += 1
    while num > 0:
        mname = _name + "[" + str(num) + "]"
        if isVm:
            vm = VirtualMachine(mname)
            vm.vhwMap = hwMap
            vms.append(vm)
        else:
            pm = PhysicalMachine(mname)
            pm.hwMap = hwMap
            pm.virtSupported = virtSupported
            pms.append(pm)
        num -= 1
    return vms if isVm else pms

def parseLan(body):
    lanMap = dict() 
    for line in body:
        tokens = line.split()
        if len(tokens) >= 1:
            lantemp = Lan(tokens[0])
            lanMap[tokens[0]] = lantemp
    return lanMap

def parseVar(body):
    varMap = dict() 
    for line in body:
        tokens = line.split()
        if len(tokens) >= 2:
            vartemp = Variable(tokens[0], tokens[1])
            varMap[tokens[0]] = vartemp
    return varMap
        
def getVserver(vmname, vservers):
    i = random.randint(0, len(vservers[vmname])-1)
    return vservers[vmname][i]

#### Main Function which transforms given input system into new system with vcpu and hypervisor as soft server
def transform(inputSys):
    ##Create hypervisor softServer for each virt enabled physical machine
    outputSys = System()
    outputSys.devcatMap = deepcopy(inputSys.devcatMap)
    outputSys.pdeviceMap = deepcopy(inputSys.pdeviceMap)
    outputSys.lanMap = deepcopy(inputSys.lanMap)

    outputSys.pmMap = deepcopy(inputSys.pmMap)
    for pmname, pm in outputSys.pmMap.iteritems():
        pm.virtSupported = "no"
    outputSys.pmGrpMap = deepcopy(inputSys.pmGrpMap)
    outputSys.serverMap = deepcopy(inputSys.serverMap)
    #Set list of tasks for each server to empty dict 
    for sname, server in outputSys.serverMap.iteritems():
        mname = inputSys.deployment[sname] 
        if mname in inputSys.vmMap:
            outputSys.serverMap[sname].tasks = dict()

    outputSys.transformed = True

    #For each physicalMachine with virtualization support, add hypervisor
    for pmname, pm in inputSys.pmMap.iteritems():
        if pm.virtSupported != "no":
            hypervisor = SoftServer()
            hypervisor.name = removeBrackets(pm.name) + "_hypervisor"
            hypervisor.count = 10           #THINK
            hypervisor.buf = 100            #THINK
            hypervisor.schedP = "fcfs"      #CHECK
            outputSys.serverMap[hypervisor.name] = deepcopy(hypervisor)
            outputSys.deployment[hypervisor.name] = pm.name
        ##For each hypervisor, add network call task 
            t = Task("network_call_"  + hypervisor.name)
            st = SubTaskServiceTime(inputSys.getCpuDevCat(pmname), 0.001, 2.8)     #ADD    #CHANGE
            t.subtasks.append(st)
            t.server = hypervisor.name 
            outputSys.taskMap[t.name] = deepcopy(t)
            outputSys.serverMap[hypervisor.name].tasks[t.name] = True

    ##For each cpu type device in VM, create a softserver
    vservers = dict()     #For each cpu type device in it, create server. Key will be vmname and value will be list of soft servers for it
    for vmname, vm in inputSys.vmMap.iteritems():
        vservers[vmname] = []
        for vhwname, vhw in vm.vhwMap.iteritems():
            if inputSys.vdeviceMap[vhwname].devcat.devtype == DevType.NONCPU:
                continue
            server = SoftServer()
            server.name = removeBrackets(vmname) + "_" + vhwname + "_server"
            server.count = vhw.count
            server.buf = vhw.buf
            server.schedP = vhw.schedP
            outputSys.serverMap[server.name] = deepcopy(server)
            outputSys.deployment[server.name] = vm.deployedOn
            vservers[vmname].append(server.name)

    ##Transforming tasks
    taskgroup = dict()  #original task as key, list of equivalent tasks as value

    for sname, server in inputSys.serverMap.iteritems():
        if not inputSys.isDeployedOnVm(sname):
            outputSys.deployment[sname] = deepcopy(inputSys.deployment[sname])
            for taskname, flag in inputSys.serverMap[sname].tasks.iteritems():
                outputSys.taskMap[taskname] = deepcopy(inputSys.taskMap[taskname])
            continue
        outputSys.deployment[sname] = inputSys.vmMap[inputSys.deployment[sname]].deployedOn
        for taskname, flag in server.tasks.iteritems():
            tstart = deepcopy(inputSys.taskMap[taskname])
            if len(tstart.subtasks) == 0:
                print sname + "'s task " + taskname + " has no subtasks"        #errexit
                sys.exit(1)
            tstart.name = tstart.name + "_start"
            tstart.subtasks = tstart.subtasks[0:1]
            tstart.subtasks[0].servt = 0
            cputgroups = []         #List of lists. Each member is list of subtasks which can be made part of one task
            noncputgroups = []      #Same as above, but for noncpu type devices
            l = len(inputSys.taskMap[taskname].subtasks)
            taskgroup[taskname] = []
            taskgroup[taskname].append(tstart.name)
            outputSys.taskMap[tstart.name] = deepcopy(tstart)
            outputSys.serverMap[tstart.server].tasks[tstart.name] = True        #To complete sync call, just placeholder task

            #Check each subtask, according to device type of its device category group them accordingly and create corresponding tasks
            for i in range(l):
                tmp = []
                if inputSys.taskMap[taskname].subtasks[i].devcat.devtype == DevType.CPU:
                    while i < l and inputSys.taskMap[taskname].subtasks[i].devcat.devtype == DevType.CPU:
                        tmp.append(deepcopy(inputSys.taskMap[taskname].subtasks[i]))
                        i += 1
                    cputgroups.append(deepcopy(tmp))
                elif inputSys.taskMap[taskname].subtasks[i].devcat.devtype == DevType.NONCPU:
                    while i < l and inputSys.taskMap[taskname].subtasks[i].devcat.devtype == DevType.NONCPU:
                        tmp.append(deepcopy(inputSys.taskMap[taskname].subtasks[i]))
                        i += 1
                    noncputgroups.append(deepcopy(tmp))

            for i in range(len(cputgroups)):
                ssname = getVserver(inputSys.deployment[sname], vservers)
                name = taskname + "_" + ssname + "_" + str(i+1) + "_" + str(len(cputgroups))
                ttemp = Task(name)
                ttemp.subtasks = cputgroups[i]
                ttemp.server = ssname
                taskgroup[taskname].append(ttemp.name)
                outputSys.taskMap[ttemp.name] = ttemp
                outputSys.serverMap[ttemp.server].tasks[ttemp.name] = True

            for i in range(len(noncputgroups)):
                ssname = removeBrackets(inputSys.vmMap[inputSys.deployment[sname]].deployedOn) + "_hypervisor"
                name = taskname + "_" + ssname + "_" + str(i+1) + "_" + str(len(noncputgroups))
                ttemp = Task(name)
                ttemp.subtasks = noncputgroups[i]
                ttemp.server = ssname
                taskgroup[taskname].append(ttemp.name)
                outputSys.taskMap[ttemp.name] = ttemp
                outputSys.serverMap[ttemp.server].tasks[ttemp.name] = True
            tend = deepcopy(tstart)
            tend.name = (tend.name)[:-6] + "_end"
            taskgroup[taskname].append(tend.name)
            outputSys.taskMap[tend.name] = deepcopy(tend)
            outputSys.serverMap[tend.server].tasks[tend.name] = True         #To complete sync call, just placeholder task

    #We want actual server deployed on vmachine to make have tasks with zero service time and make transfer their actual service demand to vcpu_server(s) and hypervisor. Now the actual task will make a sync call to other other servers. 
    ##Trasnform scenarios
    for scename, scenario in deepcopy(inputSys.scenarioMap).iteritems():
        newsce = Scenario()
        newsce.name = scenario.name
        newsce.prob = scenario.prob
        newsce.msgSeq = []
        
        ''' subpart1 starts
            1. Replace each task in original message sequence with equivalent sequence of tasks in new set-up. 
            2. Based on original message's synchronous property, handling will slightly vary

            3. Only fromTask will be transformed for first msg, for rest of messages, it will be already get transformed in previous msg's toTask
               So first message will be transformed differently, then rest of the messages
        '''
        l = len(scenario.msgSeq)
        if l != 0:
            msg = scenario.msgSeq[0]
            fromTaskList = taskgroup.get(msg.fromTask)
            toTaskList = taskgroup.get(msg.toTask)
            if fromTaskList != None and msg.isSync and fromTaskList[-1][-4:] == "_end":
                fromTaskList = fromTaskList[:-1]
            if fromTaskList != None and toTaskList != None:
                newmsg = MessagePassed("", "", 0, True)
                newmsg.modified = True
                for i in range(0, len(fromTaskList)):
                    newmsg.fromTask = fromTaskList[i]
                    if i == len(fromTaskList)-1:
                        newmsg.toTask = msg.toTask
                    else:
                        newmsg.toTask = fromTaskList[i+1]
                        newmsg.pktSz = msg.pktSz
                        newsce.msgSeq.append(copy(newmsg))
                        newmsg = MessagePassed("", "", 0, False)
                        newmsg.modified = True
                first = True
                for i in range(0, len(toTaskList)):
                    newmsg.toTask = toTaskList[i]
                    newmsg.pktSz = msg.pktSz
                    newsce.msgSeq.append(copy(newmsg))
                    if i != len(toTaskList)-1:
                        newmsg = MessagePassed("","", 0, first)
                        newmsg.modified = True
                        newmsg.fromTask = toTaskList[i]
                    first = False
            elif fromTaskList != None:
                first = True
                for i in range(0, len(fromTaskList)):
                    newmsg = MessagePassed("", "", 0, first) #For only first message, it will be synchronous. For rest, it will be async
                    newmsg.modified = True
                    newmsg.fromTask = fromTaskList[i]
                    if i == len(fromTaskList)-1:
                        newmsg.toTask = msg.toTask
                    else:
                        newmsg.toTask = fromTaskList[i+1]
                    newmsg.pktSz = msg.pktSz
                    newsce.msgSeq.append(copy(newmsg))
                    first = False
            elif toTaskList != None:
                newmsg = MessagePassed("", "", 0, True)
                newmsg.modified = True
                newmsg.fromTask = msg.fromTask 
                for i in range(0, len(toTaskList)):
                    newmsg.toTask = toTaskList[i]
                    newmsg.pktSz = msg.pktSz
                    newsce.msgSeq.append(copy(newmsg))
                    if i != len(toTaskList)-1:
                        newmsg = MessagePassed("", "", 0, False)
                        newmsg.fromTask = toTaskList[i]
            else:
               newsce.msgSeq.append(msg)

        ''' Continuation, of subpart1 
            Now replacing will be done for message 2 to end of message sequence
        '''
        for i in range(1,len(scenario.msgSeq)):
            msg = scenario.msgSeq[i]
            fromTaskList = taskgroup.get(msg.fromTask)
            toTaskList = taskgroup.get(msg.toTask)
            #If actual message is synchronous, then remove extra syncrounous reply call added to fromTaskList
            if fromTaskList != None and msg.isSync and fromTaskList[-1][-4:] == "_end":
                fromTaskList = fromTaskList[:-1]
            if fromTaskList != None and toTaskList != None:
                newmsg = MessagePassed("", "", 0, False)
                newmsg.modified = True
                newmsg.fromTask = fromTaskList[len(fromTaskList)-1]
                first = True
                for i in range(0, len(toTaskList)):
                    newmsg.toTask = toTaskList[i]
                    newmsg.pktSz = msg.pktSz
                    newsce.msgSeq.append(copy(newmsg))
                    if i != len(toTaskList)-1:
                        newmsg = MessagePassed("","", 0, first)
                        newmsg.modified = True
                        newmsg.fromTask = toTaskList[i]
                    first = False
            elif fromTaskList != None:
                newmsg = MessagePassed("", "", 0, True)
                newmsg.modified = True
                newmsg.fromTask = fromTaskList[len(fromTaskList)-1]
                newmsg.toTask = msg.toTask
                newmsg.pktSz = msg.pktSz
                newsce.msgSeq.append(copy(newmsg))
            elif toTaskList != None:
                newmsg = MessagePassed("", "", 0, False)
                newmsg.modified = True
                newmsg.fromTask = msg.fromTask 
                first = True
                for i in range(0, len(toTaskList)):
                    newmsg.toTask = toTaskList[i]
                    newmsg.pktSz = msg.pktSz
                    newsce.msgSeq.append(copy(newmsg))
                    if i != len(toTaskList)-1:
                        newmsg = MessagePassed("", "", 0, first)
                        newmsg.modified = True
                        newmsg.fromTask = toTaskList[i]
                    first = False
            else:
                newsce.msgSeq.append(msg)
        ''' subpart1 ends
        '''

        ''' subpart2 starts
            Now add first entry and last exit network tasks while handling case where dummy task user is used
        '''
        if newsce.msgSeq[0].fromTask == "user":
            firstTask = outputSys.taskMap[newsce.msgSeq[0].toTask]
            origFirstTask = inputSys.taskMap[inputSys.scenarioMap[newsce.name].msgSeq[0].toTask]
        else:
            firstTask = outputSys.taskMap[newsce.msgSeq[0].fromTask]
            origFirstTask = inputSys.taskMap[inputSys.scenarioMap[newsce.name].msgSeq[0].fromTask]

        if inputSys.isDeployedOnVm(origFirstTask.server):
            newFirstTask = Task("receive_req")
            newFirstTask.server = removeBrackets(inputSys.vmMap[inputSys.deployment[origFirstTask.server]].deployedOn) + "_hypervisor"
            _devcat = outputSys.getCpuDevCat(outputSys.deployment[newFirstTask.server])
            _servt = 0.02                                                     #FIXED #CHANGE
            st = SubTaskServiceTime(_devcat, _servt, 2.8)                     #FIXED #CHANGE
            newFirstTask.subtasks.append(st)
            outputSys.taskMap[newFirstTask.name] = newFirstTask
            if newsce.msgSeq[0].fromTask == "user":
                newsce.msgSeq[0].fromTask = newFirstTask.name 
            else:
                newFirstMsg = MessagePassed("", "", 0, False)
                newFirstMsg.modified = True
                newFirstMsg.fromTask = newFirstTask.name
                newFirstMsg.toTask = newsce.msgSeq[0].fromTask
                newFirstMsg.pktSz = newsce.msgSeq[0].pktSz
                newMsgSeq = []
                newMsgSeq.append(newFirstMsg)
                newMsgSeq.extend(newsce.msgSeq)
                newsce.msgSeq = newMsgSeq
            outputSys.serverMap[newFirstTask.server].tasks[newFirstTask.name] = True

        if newsce.msgSeq[-1].toTask != "user":
            lastTask = outputSys.taskMap[newsce.msgSeq[-1].toTask]
            origLastTask = inputSys.taskMap[inputSys.scenarioMap[newsce.name].msgSeq[-1].toTask]  
            if inputSys.isDeployedOnVm(origLastTask.server):
                newLastTask = Task("send_req")
                newLastTask.server = removeBrackets(inputSys.vmMap[inputSys.deployment[origLastTask.server]].deployedOn) + "_hypervisor"
                _devcat = outputSys.getCpuDevCat(outputSys.deployment[newLastTask.server])
                _servt = 0.02                                                     #FIXED #CHANGE
                st = SubTaskServiceTime(_devcat, _servt, 2.8)
                newLastTask.subtasks.append(st)
                outputSys.taskMap[newLastTask.name] = newLastTask
                newLastMsg = MessagePassed("", "", 0, False)
                newLastMsg.modified = True
                newLastMsg.fromTask = newsce.msgSeq[len(newsce.msgSeq)-1].toTask
                newLastMsg.toTask = newLastTask.name
                newLastMsg.pktSz = newsce.msgSeq[len(newsce.msgSeq)-1].pktSz
                newsce.msgSeq.append(newLastMsg)
                outputSys.serverMap[newLastTask.server].tasks[newLastTask.name] = True
        outputSys.scenarioMap[newsce.name] = newsce
        '''subpart2 ends
        '''


        ''' subpart3 starts
            For each msg between non-colocated pair of vms in newly formed scenario, add hypervisor network call in between
        '''
        for scename, sce in outputSys.scenarioMap.iteritems():
            newMsgSeq = []
            for msg in sce.msgSeq:
                if not msg.modified:
                    newMsgSeq.append(msg)
                    continue 
                fromPm = outputSys.deployment[outputSys.taskMap[msg.fromTask].server]
                if msg.toTask == "user":
                    newMsgSeq.append(msg)
                    continue
                toPm = outputSys.deployment[outputSys.taskMap[msg.toTask].server]
                if fromPm != toPm:
                    msg1 = MessagePassed(msg.fromTask, "", msg.pktSz, False)
                    msg1.toTask = "network_call_" + removeBrackets(fromPm) + "_hypervisor"
                    msg2 = MessagePassed(msg1.toTask, "", msg.pktSz, False)
                    msg2.toTask = "network_call_" + removeBrackets(toPm) + "_hypervisor"
                    msg3 = MessagePassed(msg2.toTask, "", msg.pktSz, False)
                    msg3.toTask = msg.toTask
                    newMsgSeq.append(copy(msg1))
                    newMsgSeq.append(copy(msg2))
                    newMsgSeq.append(copy(msg3))
                else:
                    newMsgSeq.append(msg)
            sce.msgSeq = newMsgSeq
        '''subpart3 ends
        '''
    return outputSys
      

inConfig = Config()
system = inConfig.distSys
#Actual Parsing Starts Here
if len(sys.argv) <= 1:
    print "Please provide perfcenter input file as argument."
    print "Format: .\\" + sys.argv[0] + " <input_file_name> <output_file_name>"
    sys.exit(1)
inputfilename = sys.argv[1]
if len(sys.argv) == 2:
    outputfilename = "virt_" + inputfilename
else:
    outputfilename = sys.argv[2]
fread = open(inputfilename)
fwrite = open(outputfilename, "w")
filelines = []
#Reconnaissance 
for line in fread:
    tokens = line.split()
    if len(tokens) > 0:
        if tokens[0] == "variable":
            body = getBody(fread)
            inConfig.varMap = parseVar(body)
        elif tokens[0] == "devicecategory":
            body = getBody(fread)
            system.devcatMap = parseDevcat(body)
        elif tokens[0] == "pdevice":
            body = getBody(fread)
            system.pdeviceMap = parseDevice(body, False)
        elif tokens[0] == "vdevice":
            body = getBody(fread)
            system.vdeviceMap = parseDevice(body, True)
        elif tokens[0] == "physicalmachine":
            body = getBody(fread)
            pms = parsePmVm(body, tokens[1],False )
            for pm in pms:
                system.pmMap[pm.name] = pm
        elif tokens[0] == "virtualmachine":
            body = getBody(fread)
            vms = parsePmVm(body, tokens[1], True)
            for vm in vms:
                system.vmMap[vm.name] = vm
        elif tokens[0] == "deploy":
            if tokens[1] in system.vmMap:
                system.vmMap[tokens[1]].deployedOn = system.pmMap[tokens[2]].name
            else:
                system.deployment[tokens[1]] = tokens[2]
        elif tokens[0] == "lan":
            body = getBody(fread)
            system.lanMap = parseLan(body)
        elif tokens[0] == "loadparams":
            inConfig.loadParamStr = "".join(getBody(fread))
        elif tokens[0] == "modelparams":
            inConfig.modelParamStr = "".join(getBody(fread))
        else:
          filelines.append(line)
i=0
while i < len(filelines):
    tokens = filelines[i].split()
    if len(tokens) > 0:
        if tokens[0] == "task":
            body,i = getBodyFromList(filelines, i+1)
            task = parseTask(body, tokens[1])
            system.taskMap[task.name] = task
        elif tokens[0] == "scenario":
            body,i = getBodyFromList(filelines, i+1)
            if len(tokens) != 4:
               print "Syntax Error in scenario declaration"
               print "found: ", " ".join(tokens)
               print "expected: sce <sce_name> prob <prob_value"
               sys.exit(1)
            sce = parseScenario(body, tokens[1], tokens[3])
            system.scenarioMap[sce.name] = sce
        elif tokens[0] == "server":
            body,i = getBodyFromList(filelines, i+1)
            server = parseServer(body, tokens[1], system.taskMap)
            system.serverMap[server.name] = server
            if server.name not in system.deployment:
                continue
            if system.deployment[server.name] in system.vmMap:
                server.pm = system.pmMap[system.vmMap[system.deployment[server.name]].deployedOn]
            elif system.deployment[server.name] in system.pmMap:
                server.pm = system.pmMap[system.deployment[server.name]]
        else: 
            inConfig.statStr += filelines[i]
    i += 1

outConfig = Config()
outConfig.varMap = deepcopy(inConfig.varMap)
outConfig.distSys = transform(inConfig.distSys)
outConfig.modelParamStr = deepcopy(inConfig.modelParamStr)
outConfig.loadParamStr = deepcopy(inConfig.loadParamStr)
outConfig.statStr = deepcopy(inConfig.statStr)
fwrite.write(str(outConfig))

fread.close()
fwrite.close()
