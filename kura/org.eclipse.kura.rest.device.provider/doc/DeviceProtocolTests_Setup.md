# SETUP OF JMETER DEVICE AND PROTOCOL TESTS 


1.	**Transfer** the file: org.eclipse.kura.rest.device.provider-1.0.0-SNAPSHOT.jar to Raspberry PI:
scp org.eclipse.kura.rest.device.provider-1.0.0-SNAPSHOT.jar root@ipaddress:/home

2.	**Run** command in the RPI: telnet localhost 5002.

3.	**Install** the snaphot in the osqi console:  install file:/home/org.eclipse.kura.rest.device.provider-1.0.0-SNAPSHOT.jar

4.	**Start** the installed bundle in the osgi console: start bundleID (For example: start 138)

5.	**Install** ModBus driver in Drivers and Assets page.

6.	In the installed  Modbus driver, **Add** a New Asset and name it: “b924dsds4d”.

7.	In the created asset “b924dsds4d” **Add** three new channels: 
	* Temperature with type:READ_WRITE, value.type: BOOLEAN, primary.table: COILS and memory.address: 1
	* Heater with type:READ_WRITE, value.type: BOOLEAN, primary.table: COILS and memory.address: 2
	* ChannelRead with type:READ, value.type: BOOLEAN, primary.table: COILS and memory.address: 3
	
8. 	**Add** new asset "devicewithchannelwrite" with channel:
	* ChannelWrite with type:WRITE, value.type: BOOLEAN, primary.table: COILS and memory.address: 4
	
9.	**Add** new asset “devicenochannel” without channels.

10.	**Start** Modbus.jar, open ModbusPal_test_project, and run the project.

11.	In the Modbas.jar-> Modbus slaves, **Click** on the EYE button, Coils tab and add 4 rows (represent created memory.addresses) select all the rows below and unbind them.

12.	**Open** JMeter and import Jmeter script: DevicesProtocolsScript.jmx.

13.	**Setup** parameters such as ip_address, Kura username and password in the SetTestPlanParameters file insade the JMeter tool

14.	**Run** the tests with Start (Play green) button and watch general test results in the file: 99_View Results Tree in JMeter script.

15. If You want to see just **part of test results** for the specified test controller, You could open in JMeter for example: 1_Device_Endpoints_PositiveTests,
and watch test results in the file: 1_View Results Tree.
 
