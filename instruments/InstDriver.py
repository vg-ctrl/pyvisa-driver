import json
import pyvisa
import logging
import time
from time import sleep

from new_classes import *

#list_devices()
#connect()
#check_connection()


class InstrumentDriver:
    def __init__(self, type, config_json="example.json", log_file="app.log"):
        self.config_json=config_json

        with open(config_json, 'r') as f:
            self.config_file = json.load(f)
        f.close()
        self.config = self.config_file["instruments"][self.type]
 

        self.log_file = log_file
        self.type = type
        self.rm = pyvisa.ResourceManager()
        self.instance = None
        self.start_time = time.time()



        #New vars
        self.readFromVNA = False
        self.useTwoReceivers = False
        self.useFifoBuffer = False



        self.ip = self.config["Parameters"]["IP value"]
        self.port = self.config["Parameters"]["Port value"]
        self.name = type
        self.id = None

        self.minFrequency = 0.07
        self.maxFrequency = 70
        self.minAverage = 1
        self.maxAverage = 100
        self.maxNumberOfPoints = 999999999
        self.minInstructionTime = 0.0005
        self.maxInstructionTime = 0.1
        self.listOfBandwith = [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000]
        self.bandwithDefault = 1000
        self.bandwithDefault = 0
        self.numberOfPorts = 2

        self.listOfPorts = []

        p1 =   PortInformation("1", True, True, self.maxInstructionTime, self.minInstructionTime)
        p2 =   PortInformation("2", True, True, self.maxInstructionTime, self.minInstructionTime)
        p3 =   PortInformation("3", True, True, self.maxInstructionTime, self.minInstructionTime)
        p4 =   PortInformation("4", True, True, self.maxInstructionTime, self.minInstructionTime)
        p5 =   PortInformation("A1", True, True, self.maxInstructionTime, self.minInstructionTime)
        p6 =   PortInformation("A2", True, True, self.maxInstructionTime, self.minInstructionTime)
        p7 =   PortInformation("A3", True, True, self.maxInstructionTime, self.minInstructionTime)
        p8 =   PortInformation("A4", True, True, self.maxInstructionTime, self.minInstructionTime)
        p9 =   PortInformation("B1", True, True, self.maxInstructionTime, self.minInstructionTime)
        p10 =  PortInformation("B2", True, True, self.maxInstructionTime, self.minInstructionTime)
        p11 =  PortInformation("B3", True, True, self.maxInstructionTime, self.minInstructionTime)
        p12 =  PortInformation("B4", True, True, self.maxInstructionTime, self.minInstructionTime)

        self.listOfPorts.extend([p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12])



        self.response = None

        self.useHarmonicMixer = False
        self.useMultiplier = False
        self.multiplierValue = 0.0
        self.harmonicMixerValue = 0.0
        self.multiplierCutFrequency = 20.0
        #TODO
        #self.harmonicBands = new HMixerAndMultiplierBands()
        #self.readTraces = new ArrayList<String>()

        #End new vars

        self.start_log()      


        



    def start_log(self):
        self.logger = logging.getLogger(__name__)
        self.logger.setLevel(logging.DEBUG)
        console_handler = logging.StreamHandler()
        file_handler = logging.FileHandler(self.log_file, mode="w", encoding="utf-8")
        self.logger.addHandler(console_handler)
        self.logger.addHandler(file_handler)
        formatter = logging.Formatter(
            "{asctime} - {levelname} - {message}",
            style="{",
            datefmt="%d-%m-%Y %H:%M",
        )
        file_handler.setFormatter(formatter)
        console_handler.setFormatter(formatter)

    def list_devices(self):
        instr_list = self.rm.list_resources('?*')
        if len(instr_list)==0:
            self.logger.info("No devices connected")
        else:
            self.logger.info(instr_list)

    def connect(self):
        #hislip port: 4880
        resource_address = "TCPIP::" + self.ip + "::hislip0"
        #resource_address = "TCPIP0::" + self.config["VNA_parameters"]["IP value"] +"::" + str(self.config["VNA_parameters"]["Port value"]) + "::SOCKET"
        self.logger.info(f"Connecting to {resource_address}")
        for attempt in range(3):
            try:
                self.instance = self.rm.open_resource(resource_address, timeout=5000)
            except Exception as e:
                self.logger.info(f"Connection attempt {attempt+1}/3 failed")
                self.logger.info(f"{e}")
                sleep(1)
            else:
                self.instance.read_termination = '\n'
                self.instance.write_termination = '\n'
                self.instance.write("*rst; status:preset; *cls")
                idn = self.instance.query("*IDN?")
                sleep(1)
                self.logger.info(f"Connected to: '{idn}'")
                break
    
    def disconnect(self):
        try:
            if self.instance is not None:
                self.write_cmd_sequence("disconnect")
                self.instance.write("@LOC")
                self.instance.close()
                self.logger.info("Connection closed")
                self.logger.info(f"Session time:{time.time()-self.start_time}")
            else:
                self.logger.info("No connection to close")  
        except Exception as e:
            self.logger.info(f"Error while disconnecting: {e}")



    def InitialConfiguration(self, vnaConfiguration):
        self.vnaConfiguration = vnaConfiguration
        if(not self.readFromVNA):
            self.instance.write("*RST")


            #TODO
            if(False):
                self.instance.write("ROSC EXT")
                self.instance.write("ROSC:EXT:FREQ 10MHz")
            
            Receivers = False
        
            #TODO
            if(Receivers):
                try:
                    self.responce = self.instance.query("INST:PORT:COUN")

                    self.numberOfPorts = self.responce.strip()

                except Exception as e:
                    self.logger.info(f"Error querying number of ports: {e}")
                    return

            testPort = False


            measUsedP1 = ""
            measUsedP2 = ""
            inti = 3

            #TODO
            if (self.useHarmonicMixer or self.useMultiplier):
                pass

            else:
                pass


            if ("b1" in measUsedP1):
                self.instance.write("SENS1:PATH1:DIR B16")
            elif ("b2" in measUsedP1):
                self.instance.write("SENS1:PATH2:DIR B16")
            elif ("b3" in measUsedP1):
                self.instance.write("SENS1:PATH3:DIR B16")
            elif ("b4" in measUsedP1):
                self.instance.write("SENS1:PATH4:DIR B16")


            #TODO
            if(self.useTwoReceivers):
                pass

    def ConfigurateFrequencies(self):
        raise NotImplementedError("Not supported yet.")


    def ConfigurateData(self):
        raise NotImplementedError("Not supported yet.")


    def SendTrigger(self):
        self.instance.write("*TRG")
        sleep(self.TIMESLEEP)




    def ActivateSwitchPort(self, switchPort:int):
         raise NotImplementedError("Not supported yet.")

    def EnableDigitalOutput(self, digitalOutput:int):
        raise NotImplementedError("Not supported yet.")



    def DisableDigitalOutput(self, digitalOutput: int):
        raise NotImplementedError("Not supported yet.")

    #TODO
    def dataReady(self, time:int):
        pass


    #TODO
    def getDataSinglePol(self):
        listOfValues = []
        message = ""

        try:

            if(self.useFifoBuffer):
                pass
            else:
                self.instance.write("CALC:DATA? SDATA")
                if(self.dataMeasurement.getAcquisitionType() == self.TypeAcquisition.Step):
                    try:
                        sleep(self.TIMESLEEP)
                    except Exception as e:
                        self.logger.info(f"Error during sleep: {e}")
                        return


        except Exception as e:
            self.logger.info(f"Error: {e}")  


    def getDataMultiTraces(self):
        pass

    def getDataAutoPol(self):
        pass


    def getSingleData(self):
        if ((self.readFromVNA and not self.useTwoReceivers) or (self.readFromVNA and self.useTwoReceivers and self.numberOfPorts > 2)):
            return self.getDataMultiTraces()
        elif (((self.vnaConfiguration.getAutoPolarSwitch() or self.useTwoReceivers) and self.vnaConfiguration.getCurrentProlarization() == TypeProbePolarization.Both) \
                or self.vnaConfiguration.getAutoRXSwitch() or self.readFromVNA and self.useTwoReceivers):
            return self.getDataAutoPol()
        else:
            return self.getDataSinglePol()
    

    def getMultipleData():
        raise NotImplementedError("Not supported yet.")

    def clearBuffer(self):
        self.writeCommand("*CLS")

    def getIp(self) -> str:
        return self.ip
    
    def setIp(self, ip:str):
        self.ip = ip

    #TODO
    def getPowerLO(self):
        pass



    def getPort(self) -> int:
        return self.port
    
    def setPort(self, port:int):
        self.port = port

    #TODO?
    #def getSocket(self):
    #def setSocket(self):

    def getName(self) -> str:
        return self.name
    

    def setName(self, name:str):
        self.name = name



    def getId(self) -> int:
        raise NotImplementedError("Not supported yet.")

    def setId(self, id:int):
        raise NotImplementedError("Not supported yet.")

    
    def getListOfInstructions(self) -> list:
        raise NotImplementedError("Not supported yet.")

    def setListOfInstructions(self, listOfInstructions:list):
        raise NotImplementedError("Not supported yet.")


    def getMinFrequency(self) -> float:
        raise NotImplementedError("Not supported yet.")

    def setMinFrequency(self, minFrequency:float):
        raise NotImplementedError("Not supported yet.")

    def getMaxFrequency(self) -> float:
        raise NotImplementedError("Not supported yet.")


    def setMaxFrequency(self, maxFrequency:float):
        self.maxFrequency = maxFrequency

    def getMinAverage(self) -> int:
        raise NotImplementedError("Not supported yet.")
    


    def setMinAverage(self, minAverage:int):
        raise NotImplementedError("Not supported yet.")
    


    def getMaxAverage(self) -> int:
        raise NotImplementedError("Not supported yet.")
    

    def setMaxAverage(self, maxAverage:int):
        raise NotImplementedError("Not supported yet.")
    


    def getMaxNumberOfPoints(self) -> int:
        raise NotImplementedError("Not supported yet.")
    
    def setMaxNumberOfPoints(self, maxNumberOfPoints:int):
        raise NotImplementedError("Not supported yet.")
    
    def getMinInstructionTime(self) -> float:
        raise NotImplementedError("Not supported yet.")

    def setMinInstructionTime(self, minInstructionTime:float):
        raise NotImplementedError("Not supported yet.")
    

    def getMaxInstructionTime(self) -> float:
        raise NotImplementedError("Not supported yet.")
    
    def setMaxInstructionTime(self, maxInstructionTime:float):
        raise NotImplementedError("Not supported yet.")


    def getListOfBandwith(self) -> list:
        return self.listOfBandwith
    
    def setListOfBandwith(self, listOfBandwith:list[float]):
        self.listOfBandwith = listOfBandwith

    
    def getBandwithDefault() -> int:
        raise NotImplementedError("Not supported yet.")
    

    def setBandwithDefault(self, bandwithDefault:int):
        raise NotImplementedError("Not supported yet.")


    def getNumberOfPorts(self) -> int:
        raise NotImplementedError("Not supported yet.")

    #TODO
    def getIFfrequency() -> float:
        pass

    #TODO
    def getSignalInput() -> str:
        pass


    def setNumberOfPorts(self, numberOfPorts:int):
        raise NotImplementedError("Not supported yet.")

    def getListOfPorts(self) -> list:
        return self.listOfPorts
    


    def setListOfPorts(self, listOfPorts:list):
        self.listOfPorts = listOfPorts


    def getError(self) -> int:
        return self.error
    

    def setError(self, error:int):
        self.error = error

    #TODO
    def setDataMeasurement(self, dataMeasurement:DataMeasurement):
        self.dataMeasurement = dataMeasurement


    def getDataMeasurement(self) -> DataMeasurement:
        return self.dataMeasurement
    
    def sendTrigger(self) -> int:
        if (self.sendCommand("*TRG")>0):
            return 1
        else:
            return -1
        
    def hldMode(self) -> int:

        if (self.sendCommand("SENS:SWE:MODE HOLD") > 0):
            return 1
        else:
            return -1
        

    def sendCommand(self,command:str) -> int:
        try:
            self.instance.write(command)
            self.logger.info("VNA_ZNA_command: " + command)
            return 1

        except Exception as e:
            self.logger.info(f"Error: {e}")
            return -1
    
    #TODO
    def setManualTrigger(self) -> int:
        """
        Configura el analizador para recibir triggers manuales 
            Returns: Int con los errores ocurridos
        """
        try:
            self.sendCommand("TRIG:SOUR MAN")

            if (not self.dataMeasurement.isMultifrequency()):
                self.sendCommand("SWE:TYPE POIN")
                self.sendCommand("TRIG:LINK 'POIN'")
            return 1
        except Exception as e:
            self.logger.error(f"VNA_ZNA.setManualTrigger: {e}")
            return -1

    def setExternalTrigger(self,positiveTTL:bool) -> int:
        """
        Funccion que configura los triggers externos
        
            Parameter: positiveTTL Booleano que indica que el pulso TTL es por franco
            positivo
        
            Returns: Int con los errores ocurridos
        """
        try:
            self.sendCommand("TRIG:SOUR EXT")
            if (positiveTTL):
                self.sendCommand("TRIG:SLOP POS")
            else:
                self.sendCommand("TRIG:SLOP NEG")
            

            if (not self.dataMeasurement.isMultifrequency()):
                self.sendCommand("SWE:TYPE POIN")
                self.sendCommand("TRIG:LINK 'POIN'")
            

            return 1
         
        except Exception as e:
            self.logger.error(f"VNA_ZNA.setExternalTrigger: {e}")
            return -1
        

    def setMultifrecuencyParameters(self) -> int:
        try:
            if(not self.readFromVNA):
                swBB = False

                if(self.useHarmonicMixer or self.useMultiplier):
                    self.logger.info("enter sBB evaluation")

                    divisorVal = self.getHarmonicMixerValue()
                    multiplierVal = self.getMultiplierValue()

                    divisorB = (divisorVal == 1)
                    multiB = (multiplierVal == 1)

                    if(divisorB and multiB):
                        swBB = False
                        self.logger.info("swBB = False")
                    else:
                        swBB = True
                        self.logger.info("swBB = True")

                if((self.useHarmonicMixer or self.useMultiplier) and swBB):
                    self.configureFrequencyOffset()
                else:
                    #TODO
                    pass


        except Exception as e:
            pass



    def setMonofrecuencyParameters(self, frequency:float) -> int:
        try:
            self.sendCommand("SOUR:FREQ:CW " + frequency + "GHz")
            if(self.useHarmonicMixer or self.useMultiplier):
                self.configureFrequencyOffset()
            
            self.getErrors()
            return 1
        except Exception as e
            self.logger.error(f"VNA_ZNA.setMonofrequencyParameters: {e}")
            return -1
        
    def getErrors(self) -> int:
        """
        Funccionn para pedir y recoger los errores en el analizador
        
        Returns: Devuelve el numero de errores producidos. "" e.o.c.
        """
        numberErrors = 0

        try:
            self.instance.clear()
            numberErrors = self.instance.query("SYSTem:ERRor:COUNt?")
            self.instance.clear()
            response = self.instance.query("SYSTem:ERRor:ALL?").strip()
            self.logger.info("Error response " + response)

        except Exception as e
            self.logger.error(f"VNA_ZNA.getErrors: f{e}")
            return -1
        return numberErrors
    

    def getNumReceivers(self) -> int:
        try:
            #TODO possibly add query method?
            responce = self.instance.query("SENS:FOM:RNUM? \"Receivers\"")
            return responce.strip()
        except Exception as e:
            self.logger.error(f"getNumReceivers: {e}")
            return -1


    def getNumSource(self) -> int:
        try:
            response = self.sendQuery("SENS:FOM:RNUM? \"Source\"")
            return response.strip()
        except Exception as e:
            self.logger.error(f"VNA_ZNA.getNumSource: {e}")
            return -1
        
    def getIfFrequency() -> float:
        try:
            response = self.sendQuery("SENS:IF:FILT:STAG1:FREQ?")
            return response.strip().replace(',','.')
        except Exception as e:
            self.logger.error(f"VNA_ZNA.getIfFrequency: {e}")
            return -1

    def configureFrequencyOffset() -> int:
        try:
            # We enable the port config mode.
            harmoValue = 0
            IfFrequency = 0

            divisorVal = self.getHarmonicMixerValue()
            multiplierVal = self.getMultiplierValue()
            powerRF = self.power
            #TODO
            powerLO = self.harmonicBands.getBand(this.maxMeasurementFrequency).getPowerLO()
            self.logger.info("mmWaveHead Enabled")
            # VDI mmHead: Will work with any IF (we showed the answer in freq)
            # double mixer = 279; // Recommended by VDI:
            mixer = self.harmonicBands.getBand(this.maxMeasurementFrequency).getIFfrequency(); // Recommended by VDI:
            IfFrequency = mixer * 1e6;	//  MHz.

            # Things to do:   
            #     -Checking that mixer value is the same
            #    - Checking that ports are enabled
            #    - Checking that mixing is the same (coefficients)
            
            sign = -1


    def readASCIIData() -> str:
        try:
            response = self.instance.read_ascii_values()
            return response.strip().replace(":",",")
        except Exception as e:
            self.logger.error(f"VNA_ZNA.readASCIIData: {e}")
            return ""
        


    



    def write_with_delay(self, cmd, delay=0.1):
        if not self.check_connection():
            return
        self.instance.write(cmd)
        sleep(delay)
        return
    
    def query_with_delay(self, cmd, delay=0.1):
        if not self.check_connection():
            return
        response = self.instance.query(cmd)
        sleep(delay)
        return response.strip()
    
    def check_connection(self):
        if self.instance is None:
            self.logger.info("Device is not connected")
            return False
        response = self.instance.query("*IDN?")
        if response is None: 
            self.logger.info("Device is not responding")
            return False
        else:
            return True
    

            
    def write_cmd_sequence(self, cmd):
        self.logger.info(f"Executing command sequence: {cmd}")
        if cmd.startswith("get") or cmd not in self.config["cmd_dist"].keys():
            self.logger.info("Incorrect command or command type")
            return
        
        cmd_sequence = self.config["cmd_dist"][cmd]
        self.logger.info(f"Containing the following commands\n{cmd_sequence}")
        if not self.check_connection():
            return
        try: 
            for command in cmd_sequence:
                if command == "SYSTem:ERRor:ALL?":
                    self.logger.info(self.instance.query(command))
                else:
                    self.write_with_delay(command, 1)
        except Exception as e:
            self.logger.info(str(e))
            exit()   






