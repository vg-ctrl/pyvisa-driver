VNA = 1
cmd_sequence = [
                    "*RST",
                    #attenuation of 0dbm on ports
                    #"POW:ATT AREC, 0",
                    #"POW:ATT BREC, 0",
                    #"POW:ATT CREC, 0",
                    #"POW:ATT DREC, 0",
                    #create 2 traces for S12 and S21
                    "CALC:PAR:SDEF 'TRC1','S12'",
                    "DISP:WIND:TRAC:FEED 'TRC1'",
                    "CALC:PAR:SDEF 'TRC2','S21'",
                    "DISP:WIND:TRAC2:FEED 'TRC2'",

                    #page 1458
                    "SENS:PATH1:DIR B16",
                    "SENS:PATH2:DIR B16",
                    #"SENS:PATH3:DIR B16",
                    #"SENS:PATH4:DIR B16",

                    #wave correction
                    "SENS:CORR:EWAV OFF",
                    "INIT:CONT OFF",

                    #data format 4B block data
                    "FORM REAL,32",
                    #trigger config
                    #"TRIG:SOUR EXT",
                    #"TRIG:SLOP POS",

                    #number of sweeps in a single sweep mode
                    "SWE:COUN 19",

                    #add segment at 19GHZ with 1 sweep point
                    "SEGM:ADD",
                    "FREQ:STAR 19.0E+9; STOP 19.0E+9",
                    "SWE:POIN 1",
                    #IF bandwith
                    "SENS:BWID 1000.0",
                    #port power
                    "SOUR:POW -30.0",
                    "SWE:COUN 1",
                    "SYSTem:ERRor:ALL?"

]


for command in cmd_sequence:
    if command == "SYSTem:ERRor:ALL?":
        print(VNA.query_with_delay(command).strip())
        break
    else:
        VNA.write_with_delay(command, 0)


#sweep 983