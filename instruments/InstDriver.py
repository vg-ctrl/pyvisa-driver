import json
import pyvisa
import logging
import time
from time import sleep

from new_classes import *

#TODO lists to numpy array?



class InstrumentDriver:
    def __init__(self, type, config_json="example.json", log_file="app.log"):
        self.config_json=config_json

        with open(config_json, 'r') as f:
            self.config_file = json.load(f)
        f.close()
        self.config = self.config_file["instruments"][self.type]
 
        with open("bands.json",'r') as f:
            self.harmonicBands = json.load(f)
        f.close()
        

        self.log_file = log_file
        self.type = type
        self.rm = pyvisa.ResourceManager()
        self.instance = None
        self.start_time = time.time()

        self.VNAConfiguration: VNAConfiguration
        self.dataFormat = TypeVNADataFormat.REAL32

        #New vars
        self.readFromVNA = False
        self.useTwoReceivers = False
        self.useFifoBuffer = False
        self.triggerType = None
        self.TIMESLEEP = 50
        self.currentMedFile = None
        self.defaultIFBWIndex = 9
        self.defaultInputPortIndex = 6
        self.defaultOutputPotrtIndex = 7
        self.sweepTime = 0

        self.readTraces = []
        self.traces = []
        self.numTraces = 0


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
        self.eventListeners = []

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

    def connect(self) -> int:
        #hislip port: 4880
        resource_address = "TCPIP::" + self.ip + "::hislip0"
        #resource_address = "TCPIP0::" + self.config["VNA_parameters"]["IP value"] +"::" + str(self.config["VNA_parameters"]["Port value"]) + "::SOCKET"
        self.logger.info(f"Connecting to {resource_address}")

        try:
            self.instance = self.rm.open_resource(resource_address, timeout=5000)
        except Exception as e:
            self.logger.error(f"VNA_ZNA.connect: {e}")
            return -1
        else:
            self.instance.read_termination = '\n' # type: ignore
            self.instance.write_termination = '\n'# type: ignore
            self.instance.write("*rst; status:preset; *cls") # type: ignore
            idn = self.instance.query("*IDN?") # type: ignore
            self.logger.info(f"Connected to: '{idn}'")
            return 1
    
    def disconnect(self):
        try:
            if self.instance is not None:
                self.write_cmd_sequence("disconnect")
                self.instance.write("@LOC") # type: ignore
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
            self.instance.write("*RST") # type: ignore


            #TODO dataMeasurement vnaConfiguration
            if(False):
                self.instance.write("ROSC EXT")
                self.instance.write("ROSC:EXT:FREQ 10MHz")
            
            Receivers = False
        
            if(Receivers):
                try:
                    self.responce = self.instance.query("INST:PORT:COUN") # type: ignore

                    self.numberOfPorts = self.responce.strip()

                except Exception as e:
                    self.logger.info(f"Error querying number of ports: {e}")
                    return

            testPort = False


            measUsedP1 = ""
            measUsedP2 = ""
            inti = 3

            if (self.useHarmonicMixer or self.useMultiplier):
                pass

            else:
                pass


            if ("b1" in measUsedP1):
                self.instance.write("SENS1:PATH1:DIR B16") # type: ignore
            elif ("b2" in measUsedP1):
                self.instance.write("SENS1:PATH2:DIR B16") # type: ignore
            elif ("b3" in measUsedP1):
                self.instance.write("SENS1:PATH3:DIR B16") # type: ignore
            elif ("b4" in measUsedP1):
                self.instance.write("SENS1:PATH4:DIR B16") # type: ignore


            if(self.useTwoReceivers):
                pass

    def ConfigurateFrequencies(self):
        raise NotImplementedError("Not supported yet.")


    def ConfigurateData(self):
        raise NotImplementedError("Not supported yet.")


    def SendTrigger(self):
        self.instance.write("*TRG") # type: ignore
        sleep(self.TIMESLEEP)




    def ActivateSwitchPort(self, switchPort:int):
         raise NotImplementedError("Not supported yet.")

    def EnableDigitalOutput(self, digitalOutput:int):
        raise NotImplementedError("Not supported yet.")



    def DisableDigitalOutput(self, digitalOutput: int):
        raise NotImplementedError("Not supported yet.")

    #TODO dataMeasurement
    def dataReady(self, time:int) -> int:
        try:
            self.instance.clear() #type: ignore
            if


    #TODO dataMeasurement
    def getDataSinglePol(self):
        listOfValues = []
        message = ""

        try:

            if(self.useFifoBuffer):
                pass
            else:
                self.instance.write("CALC:DATA? SDATA") # type: ignore
                if(True):
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
    

    def getMultipleData(self):
        raise NotImplementedError("Not supported yet.")

    def clearBuffer(self):
        self.sendCommand("*CLS")

    def getIp(self) -> str:
        return self.ip
    
    def setIp(self, ip:str):
        self.ip = ip

    def getPowerLO(self) -> float:
        return float(self.harmonicBands[self.maxMeasurementFrequency]["PowerLO"])



    def getPort(self) -> int:
        return self.port
    
    def setPort(self, port:int):
        self.port = port


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

    
    def getBandwithDefault(self) -> int:
        raise NotImplementedError("Not supported yet.")
    

    def setBandwithDefault(self, bandwithDefault:int):
        raise NotImplementedError("Not supported yet.")


    def getNumberOfPorts(self) -> int:
        raise NotImplementedError("Not supported yet.")


    def getIFfrequency(self) -> float:
        try:
            response = self.sendQuery("SENS:IF:FILT:STAG1:FREQ?")
            return float(response.strip().replace(",","."))
        except Exception as e:
            self.logger.error(f"VNA_ZNA.getIFfrequency: {e}")
            return -1

    def getSignalInput(self) -> str:
        if(not (self.harmonicBands is None or not self.harmonicBands)):
            return self.harmonicBands[self.maxMeasurementFrequency]["SignalInput"]
        else:
            return "Port"


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

    #TODO dataMeasurement
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
            self.instance.write(command) # type: ignore
            self.logger.info("VNA_ZNA_command: " + command)
            return 1

        except Exception as e:
            self.logger.info(f"Error: {e}")
            return -1
    
    def sendQuery(self,command:str) -> str:
        try:
            self.instance.write(command) # type: ignore
            self.logger.info("VNA_ZNA_query: " + command)
            response = self.instance.read() # type: ignore
            return response

        except Exception as e:
            self.logger.info(f"Error: {e}")
            return ""


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
                    


            return 1
        except Exception as e:
            return -1



    def setMonofrecuencyParameters(self, frequency:float) -> int:
        try:
            self.sendCommand("SOUR:FREQ:CW " + str(frequency) + "GHz")
            if(self.useHarmonicMixer or self.useMultiplier):
                self.configureFrequencyOffset()
            
            self.getErrors()
            return 1
        except Exception as e:
            self.logger.error(f"VNA_ZNA.setMonofrequencyParameters: {e}")
            return -1
        
    def getErrors(self) -> int:
        """
        Funccionn para pedir y recoger los errores en el analizador
        
        Returns: Devuelve el numero de errores producidos. "" e.o.c.
        """
        numberErrors = 0

        try:
            self.instance.clear() # type: ignore
            numberErrors = self.sendQuery("SYSTem:ERRor:COUNt?")
            self.instance.clear()# type: ignore
            response = self.sendQuery("SYSTem:ERRor:ALL?").strip()
            self.logger.info("Error response " + response)

        except Exception as e:
            self.logger.error(f"VNA_ZNA.getErrors: f{e}")
            return -1
        return int(numberErrors)
    

    def getNumReceivers(self) -> int:
        try:
            response = self.sendQuery("SENS:FOM:RNUM? \"Receivers\"")
            return int(response.strip())
        except Exception as e:
            self.logger.error(f"getNumReceivers: {e}")
            return -1


    def getNumSource(self) -> int:
        try:
            response = self.sendQuery("SENS:FOM:RNUM? \"Source\"")
            return int(response.strip())
        except Exception as e:
            self.logger.error(f"VNA_ZNA.getNumSource: {e}")
            return -1
        
    def getIfFrequency(self) -> float:
        try:
            response = self.sendQuery("SENS:IF:FILT:STAG1:FREQ?")
            return float(response.strip().replace(',','.'))
        except Exception as e:
            self.logger.error(f"VNA_ZNA.getIfFrequency: {e}")
            return -1

    def configureFrequencyOffset(self) -> int:
        try:
            # We enable the port config mode.
            harmoValue = 0
            IfFrequency = 0

            divisorVal = self.getHarmonicMixerValue()
            multiplierVal = self.getMultiplierValue()
            powerRF = self.power
            powerLO = float(self.harmonicBands[self.maxMeasurementFrequency]["PowerLO"])

            self.logger.info("mmWaveHead Enabled")

            # VDI mmHead: Will work with any IF (we showed the answer in freq)
            # double mixer = 279; // Recommended by VDI:
            mixer = float(self.harmonicBands[self.maxMeasurementFrequency]["IFfrequency"]) #Recommended by VDI:
            IfFrequency = mixer * 1e6;	#  MHz.

            # Things to do:   
            #     -Checking that mixer value is the same
            #    - Checking that ports are enabled
            #    - Checking that mixing is the same (coefficients)
            
            sign = -1
            #TODO POS?
            if(self.harmonicBands[self.maxMeasurementFrequency]["LOMix"] == "POS"):
                sign = 1
            else:
                sign = -1

            IFfrecCorrected = sign * (IfFrequency / divisorVal)

            if(divisorVal != -1 and self.getMultiplierValue() > 0):

                #Changing source:
                StringPort1 = "1"
                StringPort2 = "2"
                StringPort3 = "3"
                StringPort4 = "4"

                #Power Values:
                self.sendCommand("SOUR:POW" + StringPort1 + " " + str(powerRF))
                self.sendCommand("SOUR:POW" + StringPort3 + ":OFFS " + str(powerLO) + ", ONLY")

                #Generator on:
                self.sendCommand("SOUR:POW" + StringPort1 + ":PERM ON")
                self.sendCommand("SOUR:POW" + StringPort2 + ":PERM OFF")
                self.sendCommand("SOUR:POW" + StringPort3 + ":PERM ON")
                self.sendCommand("SOUR:POW" + StringPort4 + ":PERM OFF")

                # Port1
                self.sendCommand("SOUR:FREQ" + StringPort1 + ":CONV:ARB:IFR 1," + str(multiplierVal) + ",0,SWE")
                # Port3
                self.sendCommand("SOUR:FREQ" + StringPort3 + ":CONV:ARB:IFR 1," + str(divisorVal) + ", " + str(IFfrecCorrected) + ", SWE")

                setPorts2and4 = False


                if(setPorts2and4):

                    # Receiver Segment
                    self.sendCommand("SENS:FREQ" + StringPort1 + ":CONV:ARB 1,1, " + str(IfFrequency) + ", CW")
                    self.sendCommand("SENS:FREQ" + StringPort2 + ":CONV:ARB 1,1, " + str(IfFrequency) + ", CW")
                    self.sendCommand("SENS:FREQ" + StringPort4 + ":CONV:ARB 1,1, " + str(IfFrequency) + ", CW")

                    # Receiver formula
                    self.sendCommand("FREQ1:CONV:AWR ON")
                    self.sendCommand("FREQ2:CONV:AWR ON")
                    self.sendCommand("FREQ4:CONV:AWR ON")

                    #Direct Acces to receivers
                    self.sendCommand("SENS1:PATH1:DIR B16")
                    self.sendCommand("SENS1:PATH2:DIR B16")
                    self.sendCommand("SENS1:PATH4:DIR B16")

                else:
                    # Port1
                    self.sendCommand("SOUR:FREQ" + StringPort1 + ":CONV:ARB:IFR 1," + str(multiplierVal) + ",0,SWE")
                    # Port2
                    self.sendCommand("SOUR:FREQ" + StringPort2 + ":CONV:ARB:IFR 1," + str(multiplierVal) + ",0,SWE")
                    # Port4
                    self.sendCommand("SOUR:FREQ" + StringPort4 + ":CONV:ARB:IFR 1," + str(multiplierVal) + ",0,SWE")
                    # Port3
                    self.sendCommand("SOUR:FREQ" + StringPort3 + ":CONV:ARB:IFR 1," + str(divisorVal) + ", " + str(IFfrecCorrected) + ", SWE")

                    # Receiver Segment
                    self.sendCommand("SENS:FREQ" + StringPort1 + ":CONV:ARB 1,1, " + str(IfFrequency) + ", CW")
                    self.sendCommand("SENS:FREQ" + StringPort2 + ":CONV:ARB 1,1, " + str(IfFrequency) + ", CW")
                    self.sendCommand("SENS:FREQ" + StringPort3 + ":CONV:ARB 1,1, " + str(IfFrequency) + ", CW")
                    self.sendCommand("SENS:FREQ" + StringPort4 + ":CONV:ARB 1,1, " + str(IfFrequency) + ", CW")

                    # Receiver formula
                    self.sendCommand("FREQ1:CONV:AWR ON")
                    self.sendCommand("FREQ2:CONV:AWR ON")
                    self.sendCommand("FREQ3:CONV:AWR ON")
                    self.sendCommand("FREQ4:CONV:AWR ON")

                    #Direct Acces to receivers
                    self.sendCommand("SENS1:PATH1:DIR B16")
                    self.sendCommand("SENS1:PATH2:DIR B16")
                    self.sendCommand("SENS1:PATH3:DIR B16")
                    self.sendCommand("SENS1:PATH4:DIR B16")


                    #Setting the frequency range
                    #TODO dataMeasurement


            return 1
    
        except:
            return  


    def readASCIIData(self) -> str:
        try:
            response = self.instance.read_ascii_values() # type: ignore
            return response.strip().replace(":",",")
        except Exception as e:
            self.logger.error(f"VNA_ZNA.readASCIIData: {e}")
            return ""
        

    def readBinaryData(self) -> str:
        try:
            response = self.instance.read_binary_values( #type: ignore
                container=list        
            )
            return response
        
        except Exception as e:
            self.logger.error(f"VNA_ZNA.readBinaryData: {e}")
            return ""


    def getTriggerType(self):
        return self.triggerType
    

    def getUseMultiplier(self) -> bool:
        return self.useMultiplier
    


    def setUseMultiplier(self, value:bool):
        self.useMultiplier = value


    def getUseHarmomixMixer(self) -> bool:
        return self.useHarmonicMixer
    

    def setUseHarmonicMixer(self, value:bool):
        self.useHarmonicMixer = value

    def getMultiplierValue(self) -> int:
        if(not (self.harmonicBands is None or not self.harmonicBands)):
            return self.harmonicBands[str(self.maxMeasurementFrequency)]["MultiplierValue"]
        else:
            return 0


    def setMultiplierValue(self, value:int):
        self.multiplierValue = value

    def getHarmonicMixerValue(self) -> int:
        if(not (self.harmonicBands is None or not self.harmonicBands)):
            return self.harmonicBands[str(self.maxMeasurementFrequency)]["HMixerValue"]
        else: 
            return 0

    def getHarmonicAndMultiplierBands(self):
        return self.harmonicBands

    def setHarmonicAndMultiplierBands(self, theBands):
        self.harmonicBands = theBands

    def setHarmonicMixerValue(self, value:int):
        self.harmonicMixerValue = value

    def setEnable(self, enable:bool):
        self.enable = enable

    def getMaxMeasurementFrequency(self) -> float:
        return self.maxMeasurementFrequency


    def etMaxMeasurementFrequency(self, maxFrequency:float):
        self.maxMeasurementFrequency = maxFrequency

    def getMinMeasurementFrequency(self) -> float:
        return self.minMeasurementFrequency
    

    def setMinMeasurementFrequency(self, minFrequency:float):
        self.minMeasurementFrequency = minFrequency

    def setSwitchForComponent(self, component:int):
        raise NotImplementedError("Not supported yet.")

    def getDefaultIFBWIndex(self) -> int:
        return self.defaultIFBWIndex
    


    def getDefaultInputPortIndex(self) -> int:
        return self.defaultInputPortIndex
    

    def getDefaultOutputPortIndex(self) -> int:
        return self.defaultOutputPotrtIndex
    

    def setMeasurementEventListener(self,listener:MeasurementEventListener):
        self.eventListeners.append(listener)
    

    def setDataFormat(self, dataFormat:TypeVNADataFormat):
        self.dataFormat = dataFormat
    


    def getDataFormat(self) -> TypeVNADataFormat:
        return self.dataFormat
    

    def getUseFifoBuffer(self) ->bool:
        return self.useFifoBuffer
    

    def setUseFifoBuffer(self,useFifoBuffer:bool):
        self.useFifoBuffer = useFifoBuffer
    
    def newMeasurementEvent(self):
        #TODO MeasurementEvent
        pass

    def setScreenOff(self):
        #TODO dataMeasurement
        pass

    def setScreenOn(self):
        self.sendCommand("SYST:DISP:UPD ON")


    def getMultiplierCutFrequency(self) -> float:
        cutFrequency = -1.0

        if(not (self.harmonicBands is None or not self.harmonicBands)):
            for band in self.harmonicBands:
                if (self.harmonicBands[band]["LowerLimit"] > cutFrequency and self.harmonicBands[band]["MultiplierValue"] >= 0):
                    cutFrequency = self.harmonicBands[band]["LowerLimit"]
        return cutFrequency
           
        
        

    
    def setMultiplierCutFrequency(self, cutFrequency:float):
        self.multiplierCutFrequency = cutFrequency


    def resetBuffer(self):
        self.sendCommand("*CLS")


    def setUseTwoReceivers(self, useTwoReceivers:bool):
        self.useTwoReceivers = useTwoReceivers

    def getUseTwoReceivers(self) -> bool:
        return self.useTwoReceivers
    

    def getSweepTime(self, forceRead:bool) -> float:
        return self.sweepTime


    def readDataFromAnaliser(self) -> list:
        listOfValues = []
        try:
            if(self.dataFormat == TypeVNADataFormat.ASCII):
                message = self.readASCIIData()
            else:
                message = self.readBinaryData()
            values = message.split(",")

            for value in values:
                if(not value == ""):
                    listOfValues.append(float(value))
        
        except Exception as e:
            self.logger.error(f"VNA_ZNA.readDataFromAnaliser: {e}")
        finally:
            return listOfValues
            
        

    def setPower(self, power:float) -> int:
        try:
            self.power = power
            self.sendCommand("SOUR:POW " + str(power))
            return 1
        except Exception as e:
            self.logger.error(f"VNA_ZNA.setPower: {e}")
            return -1
        

    def setIFBandwidth(self, ifBW: float):
        try:
            self.ifBandwidth = ifBW
            self.sendCommand("SENS:BWID " + str(ifBW))
            return 1
        except Exception as e:
            self.logger.error(f"VNA_ZNA.setIFBandwidth: {e}")
            return -1


    def setFreqBandID(self,bandId:int): 
        raise NotImplementedError("Not supported yet.")


    #TODO dataMeasurement vnaConfiguration 
    def sendConfigurationAndDisconnect(self, medFile:MedFile) -> bool:
        try:
            if(not self.isConnected()):
                if(self.connect() >= 0):
                    self.currentMedFile = medFile
                    if(self.dataMeasurement):
                        return False
                    
            return False
                    
        except:
            return True
                        




    def getDataAutoCalibrate(self) -> list:
        raise NotImplementedError("Not supported yet.")


    def readConfigurationAndDisconnect(self) -> bool:
        try:
            if(not self.isConnected):
                if(self.connect() >= 0):
                    self.readTraces = []
                    self.readFromVNA = True

                    #Try to read any parameter from the VNA
                    response = self.sendQuery("SEGM:COUN?")

                    numSegm = response.strip()
                    #Anadir el numero de segmento despues de SEGM
                    response = self.sendQuery("SENS:FREQ:STAR?")
                    startFreq = float(response.strip())

                    #Tenemos la freq en Ghz
                    startF = startFreq / 1000000000

                    #A�adir el n�mero de segmento despu�s de SEGM
                    response = self.sendQuery("SENS:FREQ:STOP?")
                    stopFreq = float(response.strip())
                    stopF = stopFreq / 1000000000

                    response = self.sendQuery("SENS:SWE:POIN?")

                    numF = int(response)


                    if(startF != stopF):
                        exit_aux1 = (stopF - startF) / (numF -1)
                        exit_aux2 = exit_aux1 * 10000
                        exit_aux3 = round(exit_aux2)
                        exit = exit_aux3 / 10000
                    else:
                        exit = 1


                    listFreqs = []

                    fr = Frequencies(startF, stopF, exit)
                    listFreqs.append(fr)

                    response = self.sendQuery("SENS:BWID?")

                    ifBWID = response.strip()

                    self.ifBandwidth = float(ifBWID)


                    #Numero de trazas y cuales son
                    response = self.sendQuery("CALC:PAR:CAT?")
                    Traces = response.strip()
                    self.traces = Traces.replace("\'","").split(",")
                    self.numTraces = len(self.traces) / 2

                    for i in range (0, len(self.traces) - 1):
                        self.readTraces.append(self.traces[i + 1])


                    #Definir useTwoReceivers value
                    if(self.numTraces < 2):
                        self.useTwoReceivers = False

                    response = self.sendQuery("SOUR:POW?")

                    self.power = float(response.strip())


                    self.vnaConfiguration = VNAConfiguration(0, listFreqs, "", "" , "", self.ifBandwidth, 0, self.power)
                    self.setScreenOn()

                    self.disconnect()
            return True
                
        except Exception as e:
            self.logger.error(f"VNA_ZNA.readConfigurationAndDisconnect: {e}")
            return False

                    


    def setReadFromVNA(self, readFromVNA:bool):
        self.readFromVNA = readFromVNA

    def getReadFromVNA(self) -> bool:
        return self.readFromVNA
    
    def getVNAConfiguration(self) -> VNAConfiguration:
        return self.VNAConfiguration
    

    def getTraces(self) -> list:
        return self.readTraces
    

    def getDataAndDiscard(self) -> bool:
        raise NotImplementedError("Not supported yet.")









    def write_with_delay(self, cmd, delay=0.1):
        if not self.isConnected():
            return
        self.instance.write(cmd)# type: ignore
        sleep(delay)
        return
    
    def query_with_delay(self, cmd, delay=0.1):
        if not self.isConnected():
            return
        response = self.instance.query(cmd) # type: ignore
        sleep(delay)
        return response.strip()
    
    def isConnected(self):
        if self.instance is None:
            self.logger.info("Device is not connected")
            return False
        response = self.sendQuery("*IDN?")
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
        if not self.isConnected():
            return
        try: 
            for command in cmd_sequence:
                if command == "SYSTem:ERRor:ALL?":
                    self.logger.info(self.sendQuery(command))
                else:
                    self.write_with_delay(command, 1)
        except Exception as e:
            self.logger.info(str(e))
            exit()   






