import os;os.system('cls')

from InstDriver import InstrumentDriver
VNA = InstrumentDriver("VNA")
VNA.connect()

VNA.InitialConfiguration()

#VNA.instance.write("*rst; status:preset; *cls")


#VNA.instance.write('CALCulate1:PARameter:MEASure "Trc1", "S22"')
#VNA.sendCommand("INIT:CONT OFF")

#VNA.instance.write(":INITiate1:IMMediate;*WAI")

#VNA.instance.write("CALCulate1:FORMat MLINear")


#VNA.instance.write("CALCulate1:DATA? FDATa")


#tab = VNA.readDataFromAnaliser()
#response = VNA.sendQuery("CALC:DATA:NSW:COUN?")
#print(response)




#VNA.instance.write("SYST:COMM:GPIB:RTER EOI")

#print(VNA.instance.query("CALCulate1:DATA? FDATa"))

#print(VNA.instance.query_binary_values("CALCulate1:DATA? FDATa"))
