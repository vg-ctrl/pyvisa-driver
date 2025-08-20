from enum import Enum


class PortInformation:
    def __init__(self, port_name:str, input:bool, ouput:bool, maxInstTime:float, minInstTime:float):
        self.port_name = port_name
        self.input = input
        self.output = ouput
        self.maxInstTime = maxInstTime
        self.minInstTime = minInstTime



class VNAConfiguration:
    def __init__(self, int1, lisFreqs, str1, str2, str3, ifBandwidth, int2, float1):
        self.type = int1
    def getAutoPolarSwitch(self):
        return True
    
    def getAutoRXSwitch(self):
        return True
    
    def getCurrentProlarization(self):
        return True


class DataMeasurement:
    def isMultifrequency(self):
        return True
    
class TypeProbePolarization:
    Both = "Both"



class MeasurementEventListener:
    pass

class Frequencies:
    def __init__(self, startF, stopF, exit):
        self.startF = startF
        self.stopF = stopF
        self.exit = exit



class TypeVNADataFormat(Enum):
    REAL32 = 1
    ASCII = 2


class MedFile:
    pass