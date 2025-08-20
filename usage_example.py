import os
import json
os.system('cls')


with open("bands.json",'r') as f:
    harmonicBands = json.load(f)
f.close()

for band in harmonicBands:
    print(harmonicBands[band]["LowerLimit"])

exit()

VNA = InstrumentDriver("VNA")

VNA.connect()



#VNA.write_cmd_sequence("setScreenOn")

#VNA.write_cmd_sequence("Initial Configuration")

#response = VNA.query_with_delay("CALC:DATA? SDATA")


cmd_sequence = [
'SYSTEM:DISPLAY:UPDATE ON',
'SENSe1:FREQuency:Start 20e9',
'SENSe1:FREQuency:Stop 30e9',
'SENSe1:SWEep:POINts 5',
#change the measure of the existing trace
'CALCulate1:PARameter:MEAsure "Trc1", "S11"',
#defines a trace with a measurment parameter but doesnt show
'CALCulate1:PARameter:SDEFine "Trc2", "S11"',
#CALCULATE2:PARAMETER:SELECT "Trc2"
#display the second trace
'DISPlay:WINDow1:TRACe2:FEED "Trc2"',
"SYSTem:ERRor:ALL?"
]


for command in cmd_sequence:
    if command == "SYSTem:ERRor:ALL?":
        print(VNA.query_with_delay(command).strip())
        break
    else:
        VNA.write_with_delay(command, 0)


#*OPC? alternative page 953
#trace without format = SDAT?, real and imaginary data, page 1197
complex_data = VNA.query_with_delay("CALC:DATA:TRAC? 'Trc1', SDAT")
freq_list = VNA.query_with_delay('CALC:DATA:STIM?')

logfile = open("op_read_data.txt", "w")
logfile.write("Frequ / Hz; Mag / db; Phase / rad\n")


complex_tup = tuple(map(str, complex_data.split(',')))
freq_tup = tuple(map(str, freq_list.split(',')))

x = 0
while x < 501*2:
    logfile.write(freq_tup[x] + ";")
    logfile.write(complex_tup[x] + ";" + complex_tup[x+1] + "\n")
    x = x + 2
logfile.close()

VNA.close_connection()



#VNA.write_cmd_sequence("Initial Configuration")
#VNA.write_cmd_sequence("setMultifrequencyParameters2")
#VNA.write_cmd_sequence("getSweepTime")


