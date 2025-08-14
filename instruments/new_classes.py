class PortInformation:
    def __init__(self, port_name:str, input:bool, ouput:bool, maxInstTime:float, minInstTime:float):
        self.port_name = port_name
        self.input = input
        self.output = ouput
        self.maxInstTime = maxInstTime
        self.minInstTime = minInstTime



class VNAConfiguration:
    def __init__(self, type, config_json="example.json", log_file="app.log"):
        self.type = type
        self.config_json = config_json
    def getAutoPolarSwitch(self):
        return True


class DataMeasurement:
    def isMultifrequency(self):
        return True
    
class TypeProbePolarization:
    Both = "Both"