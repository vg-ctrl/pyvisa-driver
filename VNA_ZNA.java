/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drivers;

import auxClass.AuxFunctions;
import auxClass.Frequencies;
import dataPackage.DataMeasurement;
import static drivers.VNAModel.NL;
import static drivers.VNAModel.triggerType;
import files.LogFile;
import files.MedFile;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import measurement.TypeAcquisition;
import radiofrequency.PortInformation;
import radiofrequency.VNAConfiguration;
import java.nio.charset.Charset;
import main.FactoryClass;
import measurement.MeasurementEventListener;
import measurement.TypeMeasurementEvent;
import measurement.TypeProbePolarization;
import measurement.TypeTrigger;
import radiofrequency.HMixerAndMultiplierBands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author rbezanilla
 */
public class VNA_ZNA implements VNAModel, Serializable {

    private static final long serialVersionUID = 6629685068267757690L;

    /**
     * Internal IP of the VNA
     */
    private String ip;

    /**
     * Port of the socket connection
     */
    private int port;

    /**
     * Socket to create the lan connection between Software and VNA
     */
    private Socket socket;

    /**
     * Model of the VNA
     */
    private String name;

    /**
     * Identification number for the VNA
     */
    private int id;

    /**
     * The array contains the instructions to manage the VNA. NOTE: see
     * documentation to know the Order.
     */
    private ArrayList<String> listOfInstructions;

    /**
     * Minimum frequency accept by the VNA
     */
    private double minFrequency;

    /**
     * Maximum frequency acceptby the VNA
     */
    private double maxFrequency;

    /**
     * Maximum measurement frequency
     */
    private double maxMeasurementFrequency;

    private double minMeasurementFrequency;

    /**
     * Minimum average accept by the VNA
     */
    private int minAverage;

    /**
     * Maximum average accept by the VNA
     */
    private int maxAverage;

    /**
     * Maximum number of points accepted by the VNA
     */
    private int maxNumberOfPoints;

    /**
     * Minimum execution time of the value [All instruction]
     */
    private double minInstructionTime;

    /**
     * Maximum execution time of the value [All instruction]
     */
    private double maxInstructionTime;

    /**
     * List of all bandwith acepted by the VNA
     */
    private ArrayList<Double> listOfBandwith;

    /**
     * Index of the default bandwith
     */
    private int bandwithDefault;

    /**
     * Number of port
     */
    private int numberOfPorts;

    private double power;

    private double ifBandwidth;

    /**
     * List that associated each port with each minimum and maximum power
     * accepted and if they are available as input/ouput
     */
    private ArrayList<PortInformation> listOfPorts;

    private int error;

    public VNAConfiguration vnaConfiguration;

    private DataInputStream dataInput;

    private DataOutputStream dataOutput;

    private DataMeasurement dataMeasurement;

    private int socketTimeout = 120000;

    private byte[] data;

    private boolean useMultiplier;

    private boolean useHarmonicMixer;

    private double multiplierValue;

    private double harmonicMixerValue;

    private HMixerAndMultiplierBands harmonicBands;

    private boolean enable = true;

    private final int TIMESLEEP = 50;

    private int defaultIFBWIndex = 9;

    private int defaultInputPortIndex = 6;

    private int defaultOutputPotrtIndex = 7;

    private TypeVNADataFormat dataFormat = TypeVNADataFormat.REAL32;

    private ArrayList<MeasurementEventListener> eventListeners = new ArrayList<MeasurementEventListener>();

    private boolean useFifoBuffer = false;

    private double multiplierCutFrequency;

    private boolean useTwoReceivers = true;

    private boolean readFromVNA = false;

    private boolean configureReadDone = false;

    private int numTraces = 0;

    ArrayList<String> readTraces;

    private String[] traces;

    private double sweepTime = 0;

    private MedFile currentMedFile;

    private Logger logger = LogManager.getRootLogger();

    /*todo*/
    public VNA_ZNA(DataMeasurement dataMeasurement) {
        this.dataMeasurement = dataMeasurement;
        this.ip = "192.168.1.5";
        this.port = 5025;
        this.socket = null;
        this.name = "ZNA";
        this.id = 1;

        this.minFrequency = 0.07;
        this.maxFrequency = 70;
        this.minAverage = 1;
        this.maxAverage = 100;
        this.maxNumberOfPoints = 999999999;
        this.minInstructionTime = 0.0005;
        this.maxInstructionTime = 0.1;
        this.listOfBandwith = new ArrayList<Double>();
        this.listOfBandwith.add(1.0);
        this.listOfBandwith.add(2.0);
        this.listOfBandwith.add(5.0);
        this.listOfBandwith.add(10.0);
        this.listOfBandwith.add(20.0);
        this.listOfBandwith.add(50.0);
        this.listOfBandwith.add(100.0);
        this.listOfBandwith.add(200.0);
        this.listOfBandwith.add(500.0);
        this.listOfBandwith.add(1000.0);
        this.listOfBandwith.add(2000.0);
        this.listOfBandwith.add(5000.0);
        this.listOfBandwith.add(10000.0);
        this.listOfBandwith.add(20000.0);
        this.listOfBandwith.add(50000.0);
        this.listOfBandwith.add(100000.0);
        this.bandwithDefault = 1000;
        this.bandwithDefault = 0;
        this.numberOfPorts = 2;
        this.listOfPorts = new ArrayList<PortInformation>();
        PortInformation p1 = new PortInformation("1", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p2 = new PortInformation("2", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p3 = new PortInformation("3", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p4 = new PortInformation("4", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p5 = new PortInformation("A1", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p6 = new PortInformation("A2", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p7 = new PortInformation("A3", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p8 = new PortInformation("A4", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p9 = new PortInformation("B1", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p10 = new PortInformation("B2", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p11 = new PortInformation("B3", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p12 = new PortInformation("B4", true, true, maxInstructionTime, minInstructionTime);

        this.listOfPorts.add(p1);
        this.listOfPorts.add(p2);
        this.listOfPorts.add(p3);
        this.listOfPorts.add(p4);
        this.listOfPorts.add(p5);
        this.listOfPorts.add(p6);
        this.listOfPorts.add(p7);
        this.listOfPorts.add(p8);
        this.listOfPorts.add(p9);
        this.listOfPorts.add(p10);
        this.listOfPorts.add(p11);
        this.listOfPorts.add(p12);
        this.data = new byte[64];
        this.useHarmonicMixer = false;
        this.useMultiplier = false;
        this.multiplierValue = 0.0;
        this.harmonicMixerValue = 0.0;
        this.defaultInputPortIndex = 6;
        this.defaultOutputPotrtIndex = 7;
        this.harmonicBands = new HMixerAndMultiplierBands();
        this.readTraces = new ArrayList<String>();

        this.multiplierCutFrequency = 20.0;
        this.power = Double.parseDouble(this.dataMeasurement.getMedFile().getPower());
        this.ifBandwidth = Double.parseDouble(this.dataMeasurement.getMedFile().getBandwith());

    }

    public VNA_ZNA() {

        this.ip = "192.168.1.5";
        this.port = 5025;
        this.socket = null;
        this.name = "ZNA";
        this.id = 1;

        this.minFrequency = 0.07;
        this.maxFrequency = 70;
        this.minAverage = 1;
        this.maxAverage = 100;
        this.maxNumberOfPoints = 999999999;
        this.minInstructionTime = 0.0005;
        this.maxInstructionTime = 0.1;
        this.listOfBandwith = new ArrayList<Double>();
        this.listOfBandwith.add(1.0);
        this.listOfBandwith.add(2.0);
        this.listOfBandwith.add(5.0);
        this.listOfBandwith.add(10.0);
        this.listOfBandwith.add(20.0);
        this.listOfBandwith.add(50.0);
        this.listOfBandwith.add(100.0);
        this.listOfBandwith.add(200.0);
        this.listOfBandwith.add(500.0);
        this.listOfBandwith.add(1000.0);
        this.listOfBandwith.add(2000.0);
        this.listOfBandwith.add(5000.0);
        this.listOfBandwith.add(10000.0);
        this.listOfBandwith.add(20000.0);
        this.listOfBandwith.add(50000.0);
        this.listOfBandwith.add(100000.0);
        this.bandwithDefault = 1000;
        this.bandwithDefault = 0;
        this.numberOfPorts = 2;
        this.listOfPorts = new ArrayList<PortInformation>();

        PortInformation p1 = new PortInformation("1", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p2 = new PortInformation("2", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p3 = new PortInformation("3", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p4 = new PortInformation("4", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p5 = new PortInformation("A1", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p6 = new PortInformation("A2", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p7 = new PortInformation("A3", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p8 = new PortInformation("A4", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p9 = new PortInformation("B1", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p10 = new PortInformation("B2", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p11 = new PortInformation("B3", true, true, maxInstructionTime, minInstructionTime);
        PortInformation p12 = new PortInformation("B4", true, true, maxInstructionTime, minInstructionTime);

        this.listOfPorts.add(p1);
        this.listOfPorts.add(p2);
        this.listOfPorts.add(p3);
        this.listOfPorts.add(p4);
        this.listOfPorts.add(p5);
        this.listOfPorts.add(p6);
        this.listOfPorts.add(p7);
        this.listOfPorts.add(p8);
        this.listOfPorts.add(p9);
        this.listOfPorts.add(p10);
        this.listOfPorts.add(p11);
        this.listOfPorts.add(p12);

        this.data = new byte[1024];

        this.useHarmonicMixer = false;
        this.useMultiplier = false;
        this.multiplierValue = 0.0;
        this.harmonicMixerValue = 0.0;
        this.harmonicBands = new HMixerAndMultiplierBands();
        this.readTraces = new ArrayList<String>();

        this.multiplierCutFrequency = 20.0;
    }

    @Override
    public int connect() {
        try {
            if (logger == null) {
                logger = LogManager.getRootLogger();
            }
            socket = new Socket(this.ip, this.port);
            //Hay que ver qu? tiempo es coherente
            socket.setSoTimeout(this.socketTimeout);
            socket.setTcpNoDelay(true);
            InputStream input = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            dataInput = new DataInputStream(input);
            dataOutput = new DataOutputStream(out);

            return 1;
        } catch (UnknownHostException e) {
            return -71;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return -72;
        }

    }

    @Override
    public int disconnect() {
        try {

            if (socket != null && socket.isConnected()) {
                this.sendCommand("@LOC");
                socket.close();
            }

        } catch (IOException ex) {
            return -11;
        }
        return 1;
    }

    @Override
    public int InitialConfiguration(VNAConfiguration vnaConfiguration) {
        try {
            this.vnaConfiguration = vnaConfiguration;

            if (!this.readFromVNA) {

                this.sendCommand("*RST");

                if (FactoryClass.getProgramConfiguration().getGuiConfiguration().getLoggedInUser().getExternalClockSynchro().equalsIgnoreCase("yes")) { //Test to export in csv

                    //Set external clock for 696-ERI
                    this.sendCommand("ROSC EXT");
                    this.sendCommand("ROSC:EXT:FREQ 10MHz");

                }

                int numberOfPorts = 2;
                boolean Receivers = false;

                // Count the number of ports and eliminnate the atenuation 
                if (Receivers) {
                    try {

                        byte[] response = new byte[64];
                        this.sendCommand("INST:PORT:COUN?");
                        dataInput.read(response);
                        String message = new String(response, "UTF-8");
                        numberOfPorts = Integer.parseInt(message.trim());

                    } catch (IOException ex) {
                        logger.error("VNA_ZNA.getErrors: " + ex.getMessage());
                        return -1;
                    }
                }

                boolean testPort = false; //A true para TRT, cambiar para el resto de sistemas!

                String measUsedP1 = "";
                String measUsedP2 = "";
                int inti = 3;

                // Note: If we use multipliers the ratios cannot be the same
                if (useHarmonicMixer || useMultiplier) {

                    if (this.currentMedFile != null && !"".equals(this.currentMedFile.getPrimaryParameter().getMeasurementParameter())) {

                        measUsedP1 = this.currentMedFile.getPrimaryParameter().getMeasurementParameter();
                        this.sendCommand("CALC:PAR:SDEF 'TRC1','" + this.currentMedFile.getPrimaryParameter().getMeasurementParameter() + "'");
                        System.out.println("CALC:PAR:SDEF 'TRC1','" + this.currentMedFile.getPrimaryParameter().getMeasurementParameter() + "'");
                        logger.info("VNA_ZNA.initialConfiguration: " + "CALC:PAR:SDEF 'TRC1','" + this.currentMedFile.getPrimaryParameter().getMeasurementParameter() + "'");

                    } else if (this.dataMeasurement != null) {

                        measUsedP1 = this.dataMeasurement.getMedFile().getPrimaryParameter().getMeasurementParameter();
                        this.sendCommand("CALC:PAR:SDEF 'TRC1','" + this.dataMeasurement.getMedFile().getPrimaryParameter().getMeasurementParameter() + "'");
                        System.out.println("CALC:PAR:SDEF 'TRC1','" + this.dataMeasurement.getMedFile().getPrimaryParameter().getMeasurementParameter() + "'");
                        logger.info("VNA_ZNA.initialConfiguration: " + "CALC:PAR:SDEF 'TRC1','" + this.dataMeasurement.getMedFile().getPrimaryParameter().getMeasurementParameter() + "'");

                    }

                    if (testPort) {

                        this.sendCommand("CALC:PAR:SDEF 'TRC1','b2/a1'");
                        //System.out.println("CALC:PAR:SDEF 'TRC1','b2/a1'");
                        logger.info("CALC:PAR:SDEF 'TRC1','b2/a1'");

                    }

                    this.sendCommand("DISP:WIND:TRAC:FEED 'TRC1'");

                } else {

                    if (this.currentMedFile != null && !"".equals(this.currentMedFile.getPrimaryParameter().getMeasurementParameter())) {
                        measUsedP1 = this.currentMedFile.getPrimaryParameter().getMeasurementParameter();
                        this.sendCommand("CALC:PAR:SDEF 'TRC1','" + this.currentMedFile.getPrimaryParameter().getMeasurementParameter() + "'");
                        logger.info("VNA_ZNA.initialConfiguration: " + "CALC:PAR:SDEF 'TRC1','" + this.currentMedFile.getPrimaryParameter().getMeasurementParameter() + "'");
                    } else if (this.dataMeasurement != null) {
                        measUsedP1 = this.dataMeasurement.getMedFile().getPrimaryParameter().getMeasurementParameter();
                        this.sendCommand("CALC:PAR:SDEF 'TRC1','" + this.dataMeasurement.getMedFile().getPrimaryParameter().getMeasurementParameter() + "'");
                        logger.info("VNA_ZNA.initialConfiguration: " + "CALC:PAR:SDEF 'TRC1','" + this.dataMeasurement.getMedFile().getPrimaryParameter().getMeasurementParameter() + "'");
                    }

                    if (testPort) {

                        this.sendCommand("CALC:PAR:SDEF 'TRC1','b2/a1'");
                        System.out.println("CALC:PAR:SDEF 'TRC1','b2/a1'");
                        logger.info("CALC:PAR:SDEF 'TRC1','b2/a1'");

                    }

                    this.sendCommand("DISP:WIND:TRAC:FEED 'TRC1'");

                }

                if (measUsedP1.contains("b1")) {
                    this.sendCommand("SENS1:PATH1:DIR B16");
                } else if (measUsedP1.contains("b2")) {
                    this.sendCommand("SENS1:PATH2:DIR B16");
                } else if (measUsedP1.contains("b3")) {
                    this.sendCommand("SENS1:PATH3:DIR B16");
                } else if (measUsedP1.contains("b4")) {
                    this.sendCommand("SENS1:PATH4:DIR B16");
                }
                if (useTwoReceivers) {

                    if (FactoryClass.getProgramConfiguration().getGuiConfiguration().getLoggedInUser().getCustomer().equalsIgnoreCase("VIASAT")
                            && ((FactoryClass.getProgramConfiguration().getMeasurementSystemInformation().getMeasurementSystemIndexSelected() == 1) || (FactoryClass.getProgramConfiguration().getMeasurementSystemInformation().getMeasurementSystemIndexSelected() == 3))) {

                        this.sendCommand("CALC:PAR:SDEF 'TRC2','b1/a2'");
                        this.sendCommand("DISP:WIND:TRAC2:FEED 'TRC2'");
                        this.sendCommand("OUTP:DPOR PORT2");

                    } else {

                        if (testPort) {

                            this.sendCommand("CALC:PAR:SDEF 'TRC2','b4/a1'");
                            //System.out.println("CALC:PAR:SDEF 'TRC2','b4/a1'");
                            logger.info("CALC:PAR:SDEF 'TRC2','b4/a1'");
                            this.sendCommand("SENS1:PATH4:DIR B16");

                        } else {

                            if (this.currentMedFile != null && !"".equals(this.currentMedFile.getSecondaryParameter().getMeasurementParameter())) {

                                measUsedP2 = this.currentMedFile.getSecondaryParameter().getMeasurementParameter();
                                //this.sendCommand("CALC:PAR:SDEF 'TRC2','" + this.currentMedFile.getSecondaryParameter().getMeasurementParameter() + "'");
                                this.sendCommand("CALC:PAR:SDEF 'TRC2','" + measUsedP2 + "'");
                                logger.info("VNA_ZNA.initialConfiguration: " + "CALC:PAR:SDEF 'TRC2','" + this.currentMedFile.getSecondaryParameter().getMeasurementParameter() + "'");
                                System.out.println("CALC:PAR:SDEF 'TRC2','" + this.currentMedFile.getSecondaryParameter().getMeasurementParameter() + "'");

                            } else if (this.dataMeasurement != null) {

                                measUsedP2 = this.dataMeasurement.getMedFile().getSecondaryParameter().getMeasurementParameter();
                                //this.sendCommand("CALC:PAR:SDEF 'TRC2','" + this.dataMeasurement.getMedFile().getSecondaryParameter().getMeasurementParameter() + "'");
                                this.sendCommand("CALC:PAR:SDEF 'TRC2','" + measUsedP2 + "'");
                                logger.info("VNA_ZNA.initialConfiguration: " + "CALC:PAR:SDEF 'TRC2','" + this.dataMeasurement.getMedFile().getSecondaryParameter().getMeasurementParameter() + "'");

                            }

                            if (measUsedP2.contains("b1")) {
                                this.sendCommand("SENS1:PATH1:DIR B16");
                            } else if (measUsedP2.contains("b2")) {
                                this.sendCommand("SENS1:PATH2:DIR B16");
                            } else if (measUsedP2.contains("b3")) {
                                this.sendCommand("SENS1:PATH3:DIR B16");
                            } else if (measUsedP2.contains("b4")) {
                                this.sendCommand("SENS1:PATH4:DIR B16");
                            }

                        }
                        this.sendCommand("DISP:WIND:TRAC2:FEED 'TRC2'");

                    }

                }

                this.sendCommand("SENS:CORR:EWAV ON");
                this.sendCommand("INIT:CONT OFF");

                if (this.dataFormat == TypeVNADataFormat.REAL32) {
                    this.sendCommand("FORM REAL,32");
                } else {
                    this.sendCommand("FORM ASCII");
                }

                if (this.dataMeasurement != null) {
                    if (Integer.parseInt(this.dataMeasurement.getMedFile().getAverage()) > 1) {
                        this.sendCommand("SENS:AVER:COUN " + this.dataMeasurement.getMedFile().getAverage());
                        this.sendCommand("SENS:AVER ON");
                    }
                } else if (Integer.parseInt(this.currentMedFile.getAverage()) > 1) {
                    this.sendCommand("SENS:AVER:COUN " + this.currentMedFile.getAverage());
                    this.sendCommand("SENS:AVER ON");
                }

                if (this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Continuous) {
                    this.setExternalTrigger(true);
                    this.sendCommand("SWE:COUN " + this.dataMeasurement.getNumberOfScanPoints());
                } else {
                    this.setManualTrigger();
                }

                this.sendCommand("SYST:DISP:UPD ON");

                this.getErrors();
                return 1;
            } else {

                if (this.dataFormat == TypeVNADataFormat.REAL32) {
                    this.sendCommand("FORM REAL,32");
                } else {
                    this.sendCommand("FORM ASCII");
                }

                if (this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Continuous) {
                    this.setExternalTrigger(true);
                    this.sendCommand("SWE:COUN " + this.dataMeasurement.getNumberOfScanPoints());
                } else {
                    this.setManualTrigger();
                    this.sendCommand("SWE:COUN 1");
                    this.sendCommand("INIT");
                }
                return 1;
            }
        } catch (NumberFormatException ex) {
            logger.error("VNA_ZNA.InitialConfiguration: " + ex.getMessage());
            return -1;
        }
    }

    @Override
    public int ConfigurateFrequencies() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int ConfigurateData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int SendTrigger() {
        /*todo */
        if (this.sendCommand("*TRG") > 1) {
            try {
                Thread.sleep(TIMESLEEP);
            } catch (InterruptedException ex) {

            }
            return 1;
        }

        return -1;

    }

    @Override
    public int ActivateSwitchPort(int switchPort) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int EnableDigitalOutput(int digitalOutput) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int DisableDigitalOutput(int digitalOutput) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int dataReady(int time) {

        try {
            this.socket.setSoTimeout(time);
            dataInput.skipBytes(dataInput.available());
            byte[] response = new byte[64];
            if (!this.useFifoBuffer) {
                if (this.dataMeasurement.getAcquisitionType() != TypeAcquisition.Step) {
                    this.sendCommand("INIT");
                }
                this.sendCommand("*OPC?");

                dataInput.read(response);
                this.socket.setSoTimeout(this.socketTimeout);
                String message = new String(response, "UTF-8");
                if (message.contains("1")) {
                    return 1;
                }
            } else {
                return 1;
            }
        } catch (SocketTimeoutException ex) {
            //newMeasurementEvent(TypeMeasurementEvent.AnalyzerSocketTimeOut);
            logger.error("ZNA.dataReady: " + ex.getMessage());
            return -11;
        } catch (IOException ex) {
            newMeasurementEvent(TypeMeasurementEvent.AnalyzerDataError);
            logger.error("ZNA.dataReady: " + ex.getMessage());
            return -22;
        }

        return -33;

    }

    @Override
    public ArrayList<Double> getSingleData() {

        if ((this.readFromVNA && !this.useTwoReceivers) || (this.readFromVNA && this.useTwoReceivers && this.numTraces > 2)) {
            return this.getDataMultiTraces();
        } else if (((this.vnaConfiguration.getAutoPolarSwitch() || this.useTwoReceivers) && this.vnaConfiguration.getCurrentProlarization() == TypeProbePolarization.Both)
                || this.vnaConfiguration.getAutoRXSwitch() || this.readFromVNA && this.useTwoReceivers) {
            return this.getDataAutoPol();
        } else {
            return this.getDataSinglePol();
        }
    }

    @Override
    public ArrayList<Double> getDataSinglePol() {
        //   System.out.println("getSingleData");
        ArrayList<Double> listOfValues = new ArrayList<Double>();
        String message = "";

        try {
            dataInput.skipBytes(dataInput.available());
            this.data = new byte[this.data.length];
            if (dataMeasurement.isMultifrequency() && this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Continuous
                    && this.useFifoBuffer) {
                this.sendCommand("CALC:DATA:NSW:COUN?");
                byte[] bytes = new byte[64];
                try {
                    dataInput.read(bytes);
                    int numberSweeps = Integer.parseInt(new String(bytes, "UTF-8").trim());
                    FactoryClass.setNumberOfPointsAcquiredLastCut(numberSweeps);
                    if (numberSweeps == dataMeasurement.getNumberOfScanPoints()) {
                        for (int i = 1; i <= numberSweeps; i++) {
                            this.sendCommand("CALC:PAR:SEL 'TRC1'");
                            this.sendCommand("CALC:DATA:NSW:FIRS? SDAT, " + i);
                            listOfValues.addAll(readDataFromAnaliser());
                        }
                    }
                    this.sendCommand("*CLS");
                    this.sendCommand("SWE:COUN " + dataMeasurement.getNumberOfScanPoints());
                    //this.sendCommand("OUTP:UPOR:BUSY:LINK SWEep");
                    this.sendCommand("INIT");

                } catch (IOException | NumberFormatException ex) {
                    System.out.println("getDataSinglePol Exception-> " + ex.toString());
                    logger.error("ZNA.getDataSinglePol Exception->".concat(ex.getMessage()));
                }
            } else {

                this.sendCommand("CALC:DATA? SDATA");
                if (this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Step) {

                    try {
                        Thread.sleep(TIMESLEEP);
                    } catch (InterruptedException ex) {
                        System.out.println("getDataSinglePol InterruptedException-> " + ex.toString());
                        logger.error("ZNA.getDataSinglePol InterruptedException-> ".concat(ex.toString()));
                    }
                }
                ArrayList<Double> dataValues = readDataFromAnaliser();
                listOfValues.addAll(dataValues);
            }

        } catch (IOException ex) {
            newMeasurementEvent(TypeMeasurementEvent.AnalyzerDataError);
            logger.error("VNA_ZNA.getDataSinglePol: " + ex.getMessage());
            return null;
        }

        if (this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Step) {
            this.sendCommand("INIT");
        }

        return listOfValues;
    }

    @Override
    public ArrayList<Double> getDataAutoPol() {//Pendiente de implementaci?n
        ArrayList<Double> listOfValues = new ArrayList<Double>();
        int numPoints = 0;
        int maxPoints = 0;
        int multFactor = 1;
        int dataReady = 0;

        if (this.vnaConfiguration.getAutoPolarSwitch() || this.useTwoReceivers) {
            multFactor = this.vnaConfiguration.getPolarizationSteps();
        }
        if (this.vnaConfiguration.getAutoRXSwitch()) {
            multFactor = this.vnaConfiguration.getRXSwitchSteps();
        }

        if (this.useFifoBuffer) {
            maxPoints = dataMeasurement.getNumberOfScanPoints() * multFactor;
        } else {
            maxPoints = multFactor;
        }

        try {

            dataInput.skipBytes(dataInput.available());
            this.data = new byte[1024];
            if (dataMeasurement.isMultifrequency() && this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Continuous && this.useFifoBuffer) {
                byte[] bytes = new byte[64];

                try {
                    this.sendCommand("CALC:DATA:NSW:COUN?");
                    dataInput.read(bytes);
                    int numberSweeps = Integer.parseInt(new String(bytes, "UTF-8").trim());
                    //logger.error("getDataAutoPol_buffer: The VNA reports " + numberSweeps);
                    FactoryClass.setNumberOfPointsAcquiredLastCut(numberSweeps);
                    if (numberSweeps == dataMeasurement.getNumberOfScanPoints()) {
                        for (int i = 1; i <= numberSweeps; i++) {
                            this.sendCommand("Sweep->" + String.valueOf(i));
                            this.sendCommand("CALC:PAR:SEL 'TRC1'");
                            this.sendCommand("CALC:DATA:NSW:FIRS? SDAT, " + i);
                            listOfValues.addAll(readDataFromAnaliser());
                            this.sendCommand("CALC:PAR:SEL 'TRC2'");
                            this.sendCommand("CALC:DATA:NSW:FIRS? SDAT, " + i);
                            listOfValues.addAll(readDataFromAnaliser());
                        }
                    } else {
                        logger.error("getDataAutoPol_buffer: " + numberSweeps + " points acquired from a total of " + dataMeasurement.getNumberOfScanPoints());
                    }

                } catch (IOException | NumberFormatException ex) {
                    newMeasurementEvent(TypeMeasurementEvent.AnalyzerDataError);
                    logger.error("ZNA.getDataAutoPol_buffer: " + ex.getMessage());
                    return null;
                } finally {
                    this.sendCommand("*CLS");
                    this.sendCommand("SWE:COUN " + this.dataMeasurement.getNumberOfScanPoints());
                }

            } else {
                //  while (numPoints < maxPoints) {Descomentar junto a *1 y 2
                if (this.useTwoReceivers) {
                    this.sendCommand("CALC:PAR:SEL 'TRC1'");
                }
                this.sendCommand("CALC:DATA? SDATA");
                ArrayList<Double> dataPoints = readDataFromAnaliser();
                listOfValues.addAll(dataPoints);

                if (numPoints != maxPoints) {

                    if (this.vnaConfiguration.getAutoPolarSwitch() || this.vnaConfiguration.getAutoRXSwitch()) {
                        this.sendCommand("CALC:PAR:SEL 'TRC2'");
                        this.sendCommand("CALC:DATA? SDATA");
                        dataPoints = readDataFromAnaliser();
                        listOfValues.addAll(dataPoints);
                        dataReady = dataReady(300);
                        /*     if (dataReady < 0) {Descomentar junto a *1
                                break;
                            }
                         */
                    } else {
                        //Use two receivers
                        this.sendCommand("CALC:PAR:SEL 'TRC2'");
                        this.sendCommand("CALC:DATA? SDATA");
                        dataPoints = readDataFromAnaliser();
                        listOfValues.addAll(dataPoints);
                        numPoints++;
                    }
                }

                numPoints++;
                // } Descomentar junto a *2
            }

        } catch (SocketTimeoutException ex) {
            newMeasurementEvent(TypeMeasurementEvent.AnalyzerSocketTimeOut);
            logger.error("ZNA.getDataAutoPol: " + ex.getMessage());
            return null;
        } catch (IOException ex) {
            newMeasurementEvent(TypeMeasurementEvent.AnalyzerDataError);
            logger.error("ZNA.getDataAutoPol: " + ex.getMessage());
            return null;
        }
        if (dataReady < 0) {
            if (vnaConfiguration.getAutoRXSwitch()) {
                newMeasurementEvent(TypeMeasurementEvent.MultiportAcquisitionError);
            } else {
                newMeasurementEvent(TypeMeasurementEvent.AnalyzerDataError);
            }
        }

        if (this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Step || this.useFifoBuffer) {
            this.sendCommand("INIT");
        }

        return listOfValues;
    }

    public ArrayList<Double> getDataMultiTraces() {//En caso de tener m�s de dos trazas
        ArrayList<Double> listOfValues = new ArrayList<Double>();
        int numPoints = 0;
        int maxPoints = 0;
        int multFactor = 1;
        int dataReady = 0;

        multFactor = this.numTraces;

        if (this.useFifoBuffer) {
            maxPoints = dataMeasurement.getNumberOfScanPoints() * multFactor;
        } else {
            maxPoints = multFactor;
        }

        try {
            System.out.println("VNA_ZNA_getMultitrace");
            dataInput.skipBytes(dataInput.available());
            this.data = new byte[1024];
            if (dataMeasurement.isMultifrequency() && this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Continuous && this.useFifoBuffer) {
                byte[] bytes = new byte[64];

                try {
                    this.sendCommand("CALC:DATA:NSW:COUN?");
                    dataInput.read(bytes);
                    int numberSweeps = Integer.parseInt(new String(bytes, "UTF-8").trim());
                    //logger.error("getDataAutoPol_buffer: The VNA reports " + numberSweeps);
                    FactoryClass.setNumberOfPointsAcquiredLastCut(numberSweeps);
                    if (numberSweeps == dataMeasurement.getNumberOfScanPoints()) {
                        for (int i = 1; i <= numberSweeps; i++) {
                            this.sendCommand("Sweep->" + String.valueOf(i));
                            for (int j = 1; j <= this.numTraces; j++) {

                                this.sendCommand("CALC:PAR:SEL 'TRC" + j + "'");
                                System.out.println("CALC:PAR:SEL 'TRC" + j + "'");
                                this.sendCommand("CALC:DATA:NSW:FIRS? SDAT, " + i);
                                listOfValues.addAll(readDataFromAnaliser());

                                /*this.sendCommand("Sweep->" + String.valueOf(i));
                                this.sendCommand("CALC:PAR:SEL 'TRC1'");
                                this.sendCommand("CALC:DATA:NSW:FIRS? SDAT, " + i);
                                listOfValues.addAll(readDataFromAnaliser());
                                this.sendCommand("CALC:PAR:SEL 'TRC2'");
                                this.sendCommand("CALC:DATA:NSW:FIRS? SDAT, " + i);
                                listOfValues.addAll(readDataFromAnaliser());*/
                            }
                        }
                    } else {
                        logger.error("getDataAutoPol_buffer: " + numberSweeps + " points acquired from a total of " + dataMeasurement.getNumberOfScanPoints());
                    }

                } catch (IOException | NumberFormatException ex) {
                    newMeasurementEvent(TypeMeasurementEvent.AnalyzerDataError);
                    logger.error("ZNA.getDataAutoPol_buffer: " + ex.getMessage());
                    return null;
                } finally {
                    this.sendCommand("SWE:COUN " + this.dataMeasurement.getNumberOfScanPoints());
                }

            } else if (this.numTraces == 1) {
                this.sendCommand("CALC:DATA? SDATA");
                if (this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Step) {

                    try {
                        Thread.sleep(TIMESLEEP);
                    } catch (InterruptedException ex) {
                        System.out.println("getDataSinglePol InterruptedException-> " + ex.toString());
                        logger.error("ZNA.getDataSinglePol InterruptedException-> ".concat(ex.toString()));
                    }
                }

                ArrayList<Double> dataValues = readDataFromAnaliser();
                listOfValues.addAll(dataValues);

            } else {
                //  while (numPoints < maxPoints) {Descomentar junto a *1 y 2

                for (int i = 1; i <= this.numTraces; i++) {

                    this.sendCommand("CALC:PAR:SEL '" + this.traces[2 * i - 2] + "'");
                    //this.sendCommand("CALC:PAR:SEL 'TRC" + i + "'");
                    //System.out.println("CALC:PAR:SEL 'TRC" + i + "'");
                    System.out.println("CALC:PAR:SEL '" + this.traces[2 * i - 2] + "'");
                    this.sendCommand("CALC:DATA? SDATA");
                    if (this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Step) {

                        try {
                            Thread.sleep(TIMESLEEP);
                        } catch (InterruptedException ex) {
                            System.out.println("getDataSinglePol InterruptedException-> " + ex.toString());
                            logger.error("ZNA.getDataSinglePol InterruptedException-> ".concat(ex.toString()));
                        }
                    }
                    ArrayList<Double> dataPoints = readDataFromAnaliser();
                    listOfValues.addAll(dataPoints);

                    /*if (numPoints != maxPoints) {

                    if (this.vnaConfiguration.getAutoPolarSwitch() || this.vnaConfiguration.getAutoRXSwitch()) {
                        this.sendCommand("CALC:PAR:SEL 'TRC2'");
                        this.sendCommand("CALC:DATA? SDATA");
                        dataPoints = readDataFromAnaliser();
                        listOfValues.addAll(dataPoints);
                        dataReady = dataReady(300);
                        //     if (dataReady < 0) {Descomentar junto a *1
                        //        break;
                        //    }
                        //
                    } else {
                        //Use two receivers
                        this.sendCommand("CALC:PAR:SEL 'TRC2'");
                        this.sendCommand("CALC:DATA? SDATA");
                        dataPoints = readDataFromAnaliser();
                        listOfValues.addAll(dataPoints);
                        numPoints++;
                    }
                }*/
                    numPoints++;
                    // } Descomentar junto a *2
                }
            }

        } catch (SocketTimeoutException ex) {
            newMeasurementEvent(TypeMeasurementEvent.AnalyzerSocketTimeOut);
            logger.error("ZNA.getDataAutoPol: " + ex.getMessage());
            return null;
        } catch (IOException ex) {
            newMeasurementEvent(TypeMeasurementEvent.AnalyzerDataError);
            logger.error("ZNA.getDataAutoPol: " + ex.getMessage());
            return null;
        }
        if (dataReady < 0) {
            if (vnaConfiguration.getAutoRXSwitch()) {
                newMeasurementEvent(TypeMeasurementEvent.MultiportAcquisitionError);
            } else {
                newMeasurementEvent(TypeMeasurementEvent.AnalyzerDataError);
            }
        }

        if (this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Step) {
            this.sendCommand("INIT");
        }
        return listOfValues;
    }

    @Override
    public int getMultipleData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int clearBuffer() {
        if (this.sendCommand("*CLS") > 0) {
            return 1;
        }

        return -1; 
    }

    @Override
    public String getIp() {
        return this.ip;
    }

    @Override
    public void setIp(String ip) {
        this.ip = ip;
    }

    private double getPowerLO() {
        return this.harmonicBands.getBand(this.maxMeasurementFrequency).getPowerLO();
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public Socket getSocket() {
        return this.socket;
    }

    @Override
    public void setSocket(Socket socket) {
        if (socket == null) {
            this.dataInput = null;
            this.dataOutput = null;
            this.eventListeners = new ArrayList<MeasurementEventListener>();

        }
        this.socket = socket;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setId(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<String> getListOfInstructions() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setListOfInstructions(ArrayList<String> listOfInstructions) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getMinFrequency() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMinFrequency(double minFrequency) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getMaxFrequency() {
        return 0;//Hay que implementarlo
    }

    @Override
    public void setMaxFrequency(double maxFrequency) {
        this.maxFrequency = maxFrequency;
    }

    @Override
    public int getMinAverage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMinAverage(int minAverage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMaxAverage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMaxAverage(int maxAverage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMaxNumberOfPoints() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMaxNumberOfPoints(int maxNumberOfPoints) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getMinInstructionTime() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMinInstructionTime(double minInstructionTime) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getMaxInstructionTime() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMaxInstructionTime(double maxInstructionTime) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Double> getListOfBandwith() {
        return this.listOfBandwith;
    }

    @Override
    public void setListOfBandwith(ArrayList<Double> listOfBandwith) {
        this.listOfBandwith = listOfBandwith;
    }

    @Override
    public int getBandwithDefault() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setBandwithDefault(int bandwithDefault) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumberOfPorts() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public double getIFfrequency() {
        if (!this.harmonicBands.getBands().isEmpty()) {
            return this.harmonicBands.getBand(this.maxMeasurementFrequency).getIFfrequency();
        } else {
            return 0.0;
        }
    }

    public String getSignalInput() {
        if (!this.harmonicBands.getBands().isEmpty()) {
            return this.harmonicBands.getBand(this.maxMeasurementFrequency).getSignalInput();
        } else {
            return "Port";
        }
    }

    @Override
    public void setNumberOfPorts(int numberOfPorts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<PortInformation> getListOfPorts() {
        return this.listOfPorts;
    }

    @Override
    public void setListOfPorts(ArrayList<PortInformation> listOfPorts) {
        this.listOfPorts = listOfPorts;
    }

    @Override
    public int getError() {
        return error;
    }

    @Override
    public void setError(int error) {
        this.error = error;
    }

    @Override
    public void setDataMeasurement(DataMeasurement dataMeasurement) {
        this.dataMeasurement = dataMeasurement;
        if (dataMeasurement != null) {
            this.ifBandwidth = Double.parseDouble(this.dataMeasurement.getMedFile().getBandwith());
            this.power = Double.parseDouble(this.dataMeasurement.getMedFile().getPower());
        }
    }

    @Override
    public DataMeasurement getDataMeasurement() {
        return this.dataMeasurement;
    }

    @Override
    public int sendTrigger() {

        if (this.sendCommand("*TRG") > 0) {
            return 1;
        }

        return -1;
    }

    @Override
    public int hldMode() {

        if (this.sendCommand("SENS:SWE:MODE HOLD") > 0) {
            return 1;
        }
        return -1;

    }

    @Override
    public int sendCommand(String command) {
        try {
            String st = command + NL;
            byte[] b = st.getBytes(Charset.forName("UTF-8"));
            this.dataOutput.write(b);
            System.out.println("VNA_ZNA_command: " + command);
            //logger.error("trace".concat(command.toString()));
            return 1;
        } catch (IOException ex) {
            return -1;
        }

    }

    /**
     * Configura el analizador para recibir triggers manuales
     *
     * @return Int con los errores ocurridos
     */
    private int setManualTrigger() {

        try {
            this.sendCommand("TRIG:SOUR MAN");

            //Configuro el tipo de Sweep
            if (!this.dataMeasurement.isMultifrequency()) {
                this.sendCommand("SWE:TYPE POIN");
                this.sendCommand("TRIG:LINK 'POIN'");
            }

            //this.sendCommand("INIT");
            return 1;
        } catch (Exception ex) {
            logger.error("VNA_ZNA.setManualTrigger: " + ex.getMessage());
            return -1;
        }

    }

    /**
     * Funci?n que configura los triggers externos
     *
     * @param positiveTTL Booleano que indica que el pulso TTL es por franco
     * positivo
     * @return Integer
     */
    private int setExternalTrigger(boolean positiveTTL) {

        try {
            this.sendCommand("TRIG:SOUR EXT");
            if (positiveTTL) {
                this.sendCommand("TRIG:SLOP POS");
            } else {
                this.sendCommand("TRIG:SLOP NEG");
            }

            //Configuro el tipo de Sweep
            if (!this.dataMeasurement.isMultifrequency()) {
                this.sendCommand("SWE:TYPE POIN");
                this.sendCommand("TRIG:LINK 'POIN'");
            }

            return 1;
        } catch (Exception ex) {
            logger.error("VNA_ZNA.setExternalTrigger: " + ex.getMessage());
            return -1;
        }

    }

    @Override
    public int setMultifrecuencyParameters() {

        try {
            if (!this.readFromVNA) {
                // Frequency Selection and set up of measurement
                boolean swBB = false;

                if ((useHarmonicMixer || useMultiplier)) {

                    logger.info("enter sBB evaluation");

                    // Frequency Selection and set up of measurement
                    double divisorVal = this.getHarmonicMixerValue();
                    double multiplierVal = this.getMultiplierValue();

                    boolean divisorB = (divisorVal == 1);
                    boolean multiB = (multiplierVal == 1);

                    if (divisorB && multiB) {
                        swBB = false;
                        logger.info("swBB = false");
                    } else {
                        swBB = true;
                        logger.info("swBB = true");
                    }
                }

                if ((useHarmonicMixer || useMultiplier) && (swBB)) {

                    // If we have Harmonic mixer and amplifier we enable the Port Config Mode 
                    configureFrequencyOffset();

                } else {

                    if (this.currentMedFile != null) {

                        for (int i = 0; i < this.dataMeasurement.getMedFile().getFrecuencyList().size(); i++) {

                            this.sendCommand("SEGM:ADD");
                            this.sendCommand("FREQ:STAR " + this.currentMedFile.getFrecuencyList().get(i).getFirst() + "E+9; STOP " + this.currentMedFile.getFrecuencyList().get(i).getLast() + "E+9");
                            this.sendCommand("SWE:POIN " + this.currentMedFile.getFrecuencyList().get(i).getNumberOfFrequencies());
                            this.sendCommand("SENS:BWID " + String.valueOf(this.ifBandwidth));
                            this.sendCommand("SOUR:POW " + String.valueOf(this.power));

                        }

                    } else if (this.dataMeasurement != null) {

                        int freqSize = this.dataMeasurement.getMedFile().getFrecuencyList().size();

                        for (int i = 0; i < this.dataMeasurement.getMedFile().getFrecuencyList().size(); i++) {

                            this.sendCommand("SEGM:ADD");
                            this.sendCommand("FREQ:STAR " + this.dataMeasurement.getMedFile().getFrecuencyList().get(i).getFirst() + "E+9; STOP " + this.dataMeasurement.getMedFile().getFrecuencyList().get(i).getLast() + "E+9");
                            this.sendCommand("SWE:POIN " + this.dataMeasurement.getMedFile().getFrecuencyList().get(i).getNumberOfFrequencies());
                            this.sendCommand("SENS:BWID " + String.valueOf(this.ifBandwidth));
                            this.sendCommand("SOUR:POW " + String.valueOf(this.power));

                        }

                    }

                }

                //this.sendCommand("SWE:TYPE SEGM");
                //if (this.useFifoBuffer){
                if (useFifoBuffer && this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Continuous) {

                    //SCAN 
                    this.sendCommand("SWE:COUN " + dataMeasurement.getNumberOfScanPoints());
                } else {
                    //STEP or CONTINUOUS WITHOUT BUFFER
                    this.sendCommand("SWE:COUN " + 1);
                }

                //this.sendCommand("OUTP:UPOR:BUSY:LINK SWEep");
                if (this.useFifoBuffer || this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Step) {
                    this.sendCommand("INIT");
                }

//                // Removing the attenuation:
//                this.sendCommand("POW:ATT BREC, 0");
//                try {
//                    Thread.sleep(TIMESLEEP);
//                } catch (InterruptedException ex) {
//
//                }
//                this.sendCommand("POW:ATT DREC, 0");
//
//                try {
//                    Thread.sleep(TIMESLEEP);
//                } catch (InterruptedException ex) {
//
//                }
                //Limpio los posibles errores
                getErrors();
            } else {
                /*if (useFifoBuffer && this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Continuous) {

                    //SCAN 
                    this.sendCommand("SWE:COUN " + dataMeasurement.getNumberOfScanPoints());
                } else {
                    //STEP or CONTINUOUS WITHOUT BUFFER
                    this.sendCommand("SWE:COUN " + 1);
                }

                //this.sendCommand("OUTP:UPOR:BUSY:LINK SWEep");
                if (this.useFifoBuffer || this.dataMeasurement.getAcquisitionType() == TypeAcquisition.Step) {
                    this.sendCommand("INIT");
                }
                 */
            }

            return 1;
        } catch (Exception ex) {
            logger.error("VNA_ZNA.setMultiFrequencyParameters: " + ex.getMessage());
            return -1;
        }
    }

    @Override
    public int setMonofrecuencyParameters(double frequency) {

        try {
            this.sendCommand("SOUR:FREQ:CW " + frequency + "GHz");

            if (useHarmonicMixer || useMultiplier) {
                configureFrequencyOffset();
            }

            //Limpio los posibles errores
            getErrors();
            return 1;
        } catch (Exception ex) {
            logger.error("VNA_ZNA.setMonofrequencyParameters: " + ex.getMessage());
            return -1;
        }
    }

    /**
     * Funci?n para pedir y recoger los errores en el analizador
     *
     * @return Devuelve el n?mero de errores producidos. "" e.o.c.
     */
    private int getErrors() {
        int numberErrors = 0;
        String response = "";
        try {

            byte[] respBytes = new byte[64];
            dataInput.skip(dataInput.available());
            this.sendCommand("SYSTem:ERRor:ALL?");

            dataInput.read(respBytes);
            response = new String(respBytes, "UTF-8").trim();
            //System.out.println("Error response " + response);
        } catch (IOException ex) {
            logger.error("VNA_ZNA.getErrors: " + ex.getMessage());
            return -1;
        }
        return numberErrors;
    }

    private int getNumReceivers() {
        try {
            byte[] response = new byte[64];
            this.sendCommand("SENS:FOM:RNUM? \"Receivers\"");
            dataInput.read(response);
            String message = new String(response, "UTF-8");
            return Integer.parseInt(message.trim());
        } catch (IOException | NumberFormatException ex) {
            logger.error("getNumReceivers: " + ex.getMessage());
            return -1;
        }
    }

    private int getNumSource() {
        try {
            byte[] response = new byte[64];
            this.sendCommand("SENS:FOM:RNUM? \"Source\"");
            dataInput.read(response);
            String message = new String(response, "UTF-8");
            return Integer.parseInt(message.trim());
        } catch (IOException | NumberFormatException ex) {
            logger.error("getNumSource: " + ex.getMessage());
            return -1;
        }
    }

    private double getIfFrequency() {
        byte[] response = new byte[64];
        try {
            this.sendCommand("SENS:IF:FILT:STAG1:FREQ?");
            dataInput.read(response);
            String message = new String(response, "UTF-8");
            return Double.parseDouble(message.trim().replace(',', '.'));
        } catch (IOException | NumberFormatException ex) {
            logger.error("VNA_ZNA.getIfFrequency: ".concat(ex.getMessage()));
            return -1;
        }
    }

    private int configureFrequencyOffset() {
        try {

            // We enable the port config mode.
            double harmoValue = 0;
            double IfFrequency = 0;

            double divisorVal = this.getHarmonicMixerValue();
            double multiplierVal = this.getMultiplierValue();
            double powerRF = this.power;
            double powerLO = this.harmonicBands.getBand(this.maxMeasurementFrequency).getPowerLO();

            logger.info("mmWaveHead Enabled");

            // VDI mmHead: Will work with any IF (we showed the answer in freq)
            //double mixer = 279; // Recommended by VDI:
            double mixer = this.harmonicBands.getBand(this.maxMeasurementFrequency).getIFfrequency(); // Recommended by VDI:
            IfFrequency = mixer * 1e6;	//  MHz.

            /* Things to do:
                
                 -Checking that mixer value is the same
                - Checking that ports are enabled
                - Checking that mixing is the same (coefficients)
                
             */
            double sign = -1;
            if (this.harmonicBands.getBand(this.maxMeasurementFrequency).getLOMix() == HMixerAndMultiplierBands.LOMix.POS) {

                sign = 1; // VDI Modules

            } else {

                sign = -1; // OML Modules
            }

            double IFfrecCorrected = sign * (IfFrequency / divisorVal); //(for OML Modules)

            if (divisorVal != -1 && this.getMultiplierValue() > 0) {

                // Changing source:
                String StringPort1 = String.valueOf(1);
                String StringPort2 = String.valueOf(2);
                String StringPort3 = String.valueOf(3);
                String StringPort4 = String.valueOf(4);

                // Power Values:
                this.sendCommand("SOUR:POW" + StringPort1 + " " + String.valueOf(powerRF));
                this.sendCommand("SOUR:POW" + StringPort3 + ":OFFS " + String.valueOf(powerLO) + ", ONLY");
                //this.sendCommand("SOUR:POW" + StringPort4 + ":OFFS " + String.valueOf(diffPowerRFLO) + ", CPAD");

                // Generator on:
                this.sendCommand("SOUR:POW" + StringPort1 + ":PERM ON");
                this.sendCommand("SOUR:POW" + StringPort2 + ":PERM OFF");
                this.sendCommand("SOUR:POW" + StringPort3 + ":PERM ON");
                this.sendCommand("SOUR:POW" + StringPort4 + ":PERM OFF");

                // Port1
                this.sendCommand("SOUR:FREQ" + StringPort1 + ":CONV:ARB:IFR 1," + String.valueOf(multiplierVal) + ",0,SWE");
                // Port3
                this.sendCommand("SOUR:FREQ" + StringPort3 + ":CONV:ARB:IFR 1," + String.valueOf(divisorVal) + ", " + IFfrecCorrected + ", SWE");

                boolean setPorts2and4 = false;

                if (setPorts2and4) {

                    // Receiver Segment
                    // Receiver Segment
                    this.sendCommand("SENS:FREQ" + StringPort1 + ":CONV:ARB 1,1, " + IfFrequency + ", CW");
                    this.sendCommand("SENS:FREQ" + StringPort2 + ":CONV:ARB 1,1, " + IfFrequency + ", CW");
                    this.sendCommand("SENS:FREQ" + StringPort4 + ":CONV:ARB 1,1, " + IfFrequency + ", CW");

                    // Receiver formula
                    this.sendCommand("FREQ1:CONV:AWR ON");
                    this.sendCommand("FREQ2:CONV:AWR ON");
                    this.sendCommand("FREQ4:CONV:AWR ON");

                    //Direct Acces to receivers
                    this.sendCommand("SENS1:PATH1:DIR B16");
                    this.sendCommand("SENS1:PATH2:DIR B16");
                    this.sendCommand("SENS1:PATH4:DIR B16");

                } else {

                    // Port1
                    this.sendCommand("SOUR:FREQ" + StringPort1 + ":CONV:ARB:IFR 1," + String.valueOf(multiplierVal) + ",0,SWE");
                    // Port2
                    this.sendCommand("SOUR:FREQ" + StringPort2 + ":CONV:ARB:IFR 1," + String.valueOf(multiplierVal) + ",0,SWE");
                    // Port4
                    this.sendCommand("SOUR:FREQ" + StringPort4 + ":CONV:ARB:IFR 1," + String.valueOf(multiplierVal) + ",0,SWE");
                    // Port3
                    this.sendCommand("SOUR:FREQ" + StringPort3 + ":CONV:ARB:IFR 1," + String.valueOf(divisorVal) + ", " + IFfrecCorrected + ", SWE");

                    // Receiver Segment
                    // Receiver Segment
                    this.sendCommand("SENS:FREQ" + StringPort1 + ":CONV:ARB 1,1, " + IfFrequency + ", CW");
                    this.sendCommand("SENS:FREQ" + StringPort2 + ":CONV:ARB 1,1, " + IfFrequency + ", CW");
                    this.sendCommand("SENS:FREQ" + StringPort3 + ":CONV:ARB 1,1, " + IfFrequency + ", CW");
                    this.sendCommand("SENS:FREQ" + StringPort4 + ":CONV:ARB 1,1, " + IfFrequency + ", CW");

                    // Receiver formula
                    this.sendCommand("FREQ1:CONV:AWR ON");
                    this.sendCommand("FREQ2:CONV:AWR ON");
                    this.sendCommand("FREQ3:CONV:AWR ON");
                    this.sendCommand("FREQ4:CONV:AWR ON");

                    //Direct Acces to receivers
                    this.sendCommand("SENS1:PATH1:DIR B16");
                    this.sendCommand("SENS1:PATH2:DIR B16");
                    this.sendCommand("SENS1:PATH3:DIR B16");
                    this.sendCommand("SENS1:PATH4:DIR B16");

                }

                //                // Setting the frequency range
                for (int i = 0; i < this.dataMeasurement.getMedFile().getFrecuencyList().size(); i++) {

                    this.sendCommand("FREQ:STAR " + this.dataMeasurement.getMedFile().getFrecuencyList().get(i).getFirst() + "E+9; STOP " + this.dataMeasurement.getMedFile().getFrecuencyList().get(i).getLast() + "E+9");
                    this.sendCommand("SWE:POIN " + this.dataMeasurement.getMedFile().getFrecuencyList().get(i).getNumberOfFrequencies());
                    this.sendCommand("SENS:BWID " + String.valueOf(this.ifBandwidth));

                }

            }
            return 1;
        } catch (Exception ex) {
            logger.error("VNA_ZNA, configureFrequencyOffset: " + ex.getMessage());
            return -1;
        }
    }

    @Override
    public String readASCIIData() {
        try {
            data = new byte[dataInput.available()];
            dataInput.read(data);
            return new String(data, "UTF-8").replace(":", ",");
        } catch (IOException ex) {
            logger.error("VNA_ZNA.readASCIIData: ".concat(ex.getMessage()));
            return "";
        }
    }

    @Override
    public String readBinaryData() {

        int numBytes = 0;
        byte[] readByte = new byte[1];
        //Si el mensaje recibido es de datos binarios del analizador.
        byte[] readBinaryShort = new byte[4];
        //Si el mensaje recivido es de datos binarios del analizador.
        byte[] readBinary = new byte[8];
        //Si el mensaje no es de datos binarios leemos en bloques de 80 bytes
        byte[] readBlock = new byte[80];
        //Mensaje leido en crudo
        String s;
        //String utilizado para saber la cantidad de datos enviados en un mensaje del analizador de datos binarios
        String numberData = new String("");
        //Mensaje enviado al SW
        String dataOut = "";
        int charData;
        int numData;
        int i;
        long accum = 0;
        int accum32 = 0;
        double auxDouble = 0;
        this.data = new byte[1024];

        try {
            numBytes = dataInput.read(readByte);
            s = new String(readByte, "UTF8");
            if (numBytes > 0) {
                s = s.substring(0, numBytes);
                if (s.equals("#")) {

                    numBytes = dataInput.read(readByte);

                    s = new String(readByte, "UTF8");
                    s = s.substring(0, numBytes);

                    charData = Integer.parseInt(s);

                    for (int bucle = 0; bucle < charData; bucle++) {
                        numBytes = dataInput.read(readByte);
                        s = new String(readByte, "UTF8");
                        s = s.substring(0, numBytes);
                        numberData = numberData.concat(s);
                    }

                    //-----------------------
                    int byteLim;
                    byte[] readAux;

                    byteLim = 32;
                    readAux = readBinaryShort;

                    //-----------------------
                    numBytes = dataInput.read(readAux);
                    dataOut = "";
                    //Leo bloques de 8 bytes con el n_mero hasta que deje de llegarme informaci_n
                    while (dataInput.available() > 0) {
                        i = 0;
                        accum = 0;
                        accum32 = 0;
                        //Transformo el n_mero de binario a double
                        for (int shiftBy = 0; shiftBy < byteLim; shiftBy += 8) {
                            // Probamos:
                            accum32 |= ((int) (readAux[i] & 0xff)) << shiftBy;

                            i++;
                        }

                        auxDouble = (double) Float.intBitsToFloat(accum32);

                        //System.out.println("Transformo los " + texto +" datos a: " + auxDouble);
                        //Si es el pimer valor inicio el string
                        if (dataOut.equals("")) {
                            dataOut = String.valueOf(auxDouble);
                        } else { //Si no es as_ concateno separando los n_meros por ":"
                            dataOut = dataOut.concat(String.valueOf("," + auxDouble));
                        }
                        numBytes = dataInput.read(readAux);
                    }
                }
            }
        } catch (IOException | NumberFormatException ex) {
            System.out.print("VNA_ZNA, readBinaryData: " + ex.getMessage());
            logger.error("VNA_ZNA, readBinaryData: ".concat(ex.getMessage()));
        }
        //    System.out.print(dataOut);
        return dataOut;
    }

    @Override
    public TypeTrigger getTriggerType() {
        return triggerType;
    }

    @Override
    public boolean getUseMultiplier() {
        return this.useMultiplier;
    }

    @Override
    public void setUseMultiplier(boolean value) {
        this.useMultiplier = value;
    }

    @Override
    public boolean getUseHarmomixMixer() {
        return this.useHarmonicMixer;
    }

    @Override
    public void setUseHarmonicMixer(boolean value) {
        this.useHarmonicMixer = value;
    }

    @Override
    public int getMultiplierValue() {
        if (!this.harmonicBands.getBands().isEmpty()) {
            return this.harmonicBands.getBand(this.maxMeasurementFrequency).getMultiplierValue();
        } else {
            return 0;
        }
    }

    @Override
    public void setMultiplierValue(int value) {
        this.multiplierValue = value;
    }

    @Override
    public int getHarmonicMixerValue() {
        if (!this.harmonicBands.getBands().isEmpty()) {
            return this.harmonicBands.getBand(this.maxMeasurementFrequency).getHMixerValue();
        } else {
            return 0;
        }

    }

    @Override
    public HMixerAndMultiplierBands getHarmonicAndMultiplierBands() {
        return this.harmonicBands;
    }

    @Override
    public void setHarmonicAndMultiplierBands(HMixerAndMultiplierBands theBands) {
        this.harmonicBands = theBands;
    }

    @Override
    public void setHarmonicMixerValue(int value) {
        this.harmonicMixerValue = value;
    }

    @Override
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public double getMaxMeasurementFrequency() {
        return this.maxMeasurementFrequency;
    }

    @Override
    public void setMaxMeasurementFrequency(double maxFrequency) {
        this.maxMeasurementFrequency = maxFrequency;
    }

    @Override
    public double getMinMeasurementFrequency() {
        return this.minMeasurementFrequency;
    }

    @Override
    public void setMinMeasurementFrequency(double minFrequency) {
        this.minMeasurementFrequency = minFrequency;
    }

    @Override
    public void setSwitchForComponent(int component) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getDefaultIFBWIndex() {
        return this.defaultIFBWIndex;
    }

    @Override
    public int getDefaultInputPortIndex() {
        return this.defaultInputPortIndex;
    }

    @Override
    public int getDefaultOutputPortIndex() {
        return this.defaultOutputPotrtIndex;
    }

    @Override
    public void setMeasurementEventListener(MeasurementEventListener listener) {
        this.eventListeners.add(listener);
    }

    @Override
    public void setDataFormat(TypeVNADataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    @Override
    public TypeVNADataFormat getDataFormat() {
        return this.dataFormat;
    }

    @Override
    public boolean getUseFifoBuffer() {
        return this.useFifoBuffer;
    }

    @Override
    public void setUseFifoBuffer(boolean useFifoBuffer) {
        this.useFifoBuffer = useFifoBuffer;
    }

    @Override
    public void newMeasurementEvent(TypeMeasurementEvent newEvent) {
        for (MeasurementEventListener listener : this.eventListeners) {
            listener.onNewMeasurementEvent(newEvent, null);
        }
    }

    @Override
    public void setScreenOff() {
        if (this.dataMeasurement.getMedFile().getShowScreen().equals("No")) {
            this.sendCommand("SYST:DISP:UPD OFF");
        }
    }

    @Override
    public void setScreenOn() {
        this.sendCommand("SYST:DISP:UPD ON");
    }

    @Override
    public double getMultiplierCutFrequency() {
        //return this.multiplierCutFrequency;
        double cutFrequency = -1.0;

        if (this.useMultiplier && !this.harmonicBands.getBands().isEmpty()) {
            for (int i = 0; i < this.harmonicBands.getBands().size(); i++) {
                if (this.harmonicBands.getBands().get(i).getLowerLimit() > cutFrequency && this.harmonicBands.getBands().get(i).getMultiplierValue() >= 0) {
                    cutFrequency = this.harmonicBands.getBands().get(i).getLowerLimit();
                }
            }

        }

        return cutFrequency;
    }

    @Override
    public void setMultiplierCutFrequency(double cutFrequency) {
        this.multiplierCutFrequency = cutFrequency;
    }

    @Override
    public void resetBuffer() {
        this.sendCommand("*CLS");
    }

    @Override
    public void setUseTwoReceivers(boolean useTwoReceivers) {
        this.useTwoReceivers = useTwoReceivers;
    }

    @Override
    public boolean getUseTwoReceivers() {
        return this.useTwoReceivers;
    }

    @Override
    public double getSweepTime(boolean forceRead) {
        return this.sweepTime;
    }

    private ArrayList<Double> readDataFromAnaliser() {
        String message = "";
        ArrayList<Double> listOfValues = new ArrayList<Double>();

        try {
            if (dataFormat == TypeVNADataFormat.ASCII) {
                message = this.readASCIIData();
            } else {
                message = this.readBinaryData();
            }

            String[] values = message.split(",");
            for (String value : values) {
                if (!value.equals("")) {
                    listOfValues.add(Double.valueOf(value.replace("\n", "")));
                }
            }
        } catch (NumberFormatException ex) {
            logger.error("readDataFromAnaliser: " + ex.getMessage());
            System.out.println("readDataFromAnaliser: ".concat(ex.getMessage()));
        } finally {

            return listOfValues;
        }

    }

    @Override
    public void setPower(double power) {
        try {
            this.power = power;
            this.sendCommand("SOUR:POW " + String.valueOf(power));

        } catch (Exception ex) {

        }
    }

    @Override
    public void setIFBandwidth(double ifBW) {
        try {
            this.ifBandwidth = ifBW;
            this.sendCommand("SENS:BWID " + String.valueOf(ifBW));
        } catch (Exception ex) {

        }
    }

    @Override
    public void setFreqBandID(int bandId) {

    }

    @Override
    public boolean sendConfigurationAndDisconnect(MedFile medFile) {

        try {
            if (this.socket == null || (this.socket != null && !this.socket.isConnected()) || (this.socket != null && this.socket.isClosed())) {
                if (this.connect() >= 0) {
                    this.currentMedFile = medFile;
                    if (this.dataMeasurement == null) {
                        this.dataMeasurement = new DataMeasurement();
                        this.dataMeasurement.createDataMeasurement(medFile, true);
                    }
                    this.setMaxFrequency(Frequencies.getMaxFrequency(this.currentMedFile.getFrequencies()));

                    VNAConfiguration vnaConfiguration = new VNAConfiguration(FactoryClass.getProgramConfiguration().getMeasurementSystemInformation().getVNAID(medFile.getVNA()),
                            new AuxFunctions().stringListToDoubleListFrequency(medFile.getFrequencies()),
                            medFile.getMeasurementFrequencyAcquisitionMode(), medFile.getInputPort(), medFile.getOutputPort(), Double.parseDouble(medFile.getBandwith()),
                            Integer.parseInt(medFile.getAverage()), Double.parseDouble(medFile.getPower()));
                    int controllerId = FactoryClass.getProgramConfiguration().getMeasurementSystemInformation().getControllerID(medFile.getControllerName());

                    vnaConfiguration.setAutoPolarSwitch(FactoryClass.getProgramConfiguration().getMeasurementSystemInformation().getListOFControllers().get(controllerId).getAutoPolarSwitch());
                    vnaConfiguration.setPolarizationSteps(2);
                    vnaConfiguration.setAutoRXSwitch(FactoryClass.getProgramConfiguration().getMeasurementSystemInformation().getListOFControllers().get(controllerId).getAutoRXSwitch());
                    vnaConfiguration.setRXSwitchSteps(FactoryClass.getProgramConfiguration().getMeasurementSystemInformation().getListOFControllers().get(controllerId).getRXSwitchSteps());
                    vnaConfiguration.setCurrentPolarization(TypeProbePolarization.valueOf(medFile.getProbePolarization()));
                    this.ifBandwidth = Double.parseDouble(medFile.getBandwith());
                    this.power = vnaConfiguration.getPower();

                    this.InitialConfiguration(vnaConfiguration);
                    this.setMultifrecuencyParameters();
                    //this.sendCommand("SENS:SWE:MODE CONT");
                    this.setScreenOn();
                    if (this.socket != null && this.socket.isConnected()) {
                        this.socket.close();
                        this.socket = null;
                    }

                    return true;
                }
            }
        } catch (IOException | NumberFormatException ex) {
            System.out.println("ZNA.sendConfigurationAndDisconnect: " + ex.getMessage());
            logger.error("ZNA.sendConfigurationAndDisconnect: " + ex.getMessage());
        }
        return false;
    }

    public ArrayList<Double> getDataAutoCalibrate() {

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    @Override
    public boolean readConfigurationAndDisconnect() {

        try {
            if (this.socket == null || (this.socket != null && !this.socket.isConnected()) || (this.socket != null && this.socket.isClosed())) {
                if (this.connect() >= 0) {
                    this.readTraces = new ArrayList<String>();
                    if (this.readTraces != null) {
                        this.readTraces.clear();
                    }
                    this.readFromVNA = true;

                    double startF = 0.0;
                    double stopF = 0.0;
                    int numF = 0;

                    //Try to read any parameter from the VNA
                    byte[] response = new byte[64];
                    this.sendCommand("SEGM:COUN?");
                    dataInput.read(response);
                    String message = new String(response, "UTF-8");
                    int numSegm = Integer.parseInt(message.trim());
                    this.sendCommand("SENS:FREQ:STAR?"); //A�adir el n�mero de segmento despu�s de SEGM
                    dataInput.read(response);
                    message = new String(response, "UTF-8");
                    String startFreq = message.trim();
                    if (startFreq.contains("\n")) {
                        startFreq = startFreq.substring(0, startFreq.indexOf("\n"));
                    }
                    startF = Double.parseDouble(startFreq) / 1000000000; //Tenemos la freq en Ghz

                    this.sendCommand("SENS:FREQ:STOP?"); //A�adir el n�mero de segmento despu�s de SEGM
                    dataInput.read(response);
                    message = new String(response, "UTF-8");
                    String stopFreq = message.trim();
                    if (stopFreq.contains("\n")) {
                        stopFreq = stopFreq.substring(0, stopFreq.indexOf("\n"));
                    }
                    stopF = Double.parseDouble(stopFreq) / 1000000000;

                    //this.sendCommand("SENS:SWE:POIN?");
                    this.sendCommand("SENS:SWE:POIN?");
                    dataInput.read(response);
                    message = new String(response, "UTF-8");
                    String numFreq = (message.trim());
                    numFreq = numFreq.substring(0, numFreq.indexOf("\n"));
                    numF = Integer.parseInt(numFreq);

                    double exit = 0.0;
                    if (startF != stopF) {
                        double exit_aux1 = (stopF - startF) / (numF - 1);
                        double exit_aux2 = exit_aux1 * 10000;
                        double exit_aux3 = (double) Math.round(exit_aux2);
                        exit = exit_aux3 / 10000;
                    } else {
                        exit = 1;
                    }

                    ArrayList<Frequencies> listFreqs = new ArrayList<Frequencies>();
                    Frequencies fr = new Frequencies(startF, stopF, exit);
                    listFreqs.add(fr);

                    this.sendCommand("SENS:BWID?");
                    dataInput.read(response);
                    message = new String(response, "UTF-8");
                    String ifBWID = (message.trim());
                    ifBWID = ifBWID.substring(0, ifBWID.indexOf("\n"));

                    this.ifBandwidth = Double.parseDouble(ifBWID);

                    //N�mero de trazas y cuales son
                    this.sendCommand("CALC:PAR:CAT?");
                    dataInput.read(response);
                    message = new String(response, "UTF-8");
                    String Traces = (message.trim());
                    this.traces = Traces.replace("\'", "").split(",");
                    this.numTraces = this.traces.length / 2;

                    for (int i = 0; i < this.traces.length - 1; i++) {
                        this.readTraces.add(this.traces[i + 1]);
                        //readValues.add(trac[i]+" "+trac[i+1]);
                        i++;
                    }

                    //Definir useTwoReceivers value
                    if (this.numTraces < 2) {
                        this.useTwoReceivers = false;
                    }

                    this.sendCommand("SOUR:POW?");
                    dataInput.read(response);
                    message = new String(response, "UTF-8");
                    String Power = (message.trim());
                    this.power = Double.parseDouble(Power.substring(0, Power.indexOf("\n")));

                    this.vnaConfiguration = new VNAConfiguration(0, listFreqs, "", "", "", this.ifBandwidth, 0, this.power);
                    this.setScreenOn();
                    if (this.socket != null && this.socket.isConnected()) {
                        this.socket.close();
                        this.socket = null;
                    }
                    return true;
                }
            }

        } catch (IOException | NumberFormatException ex) {

        }
        return false;
    }

    @Override
    public void setReadFromVNA(boolean readFromVNA) {
        this.readFromVNA = readFromVNA;
    }

    @Override
    public boolean getReadFromVNA() {
        return this.readFromVNA;
    }

    @Override
    public VNAConfiguration getVNAConfiguration() {
        return this.vnaConfiguration;
    }

    @Override
    public ArrayList<String> getTraces() {
        return this.readTraces;
    }

    @Override
    public boolean getDataAndDiscard() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
