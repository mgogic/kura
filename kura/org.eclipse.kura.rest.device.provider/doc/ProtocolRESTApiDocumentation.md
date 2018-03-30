**ProtocolRestService**
----
Rest services implemented in order to perform read or write commands on  specific Kura devices and channels connected via specific protocols.


**Context-path: /protocol**

## Method : Connect to device via protocol
Connect to the device
* **URL**

  /{protocolId}/connection/{deviceId}

* **Request type:**

  `POST` 
  
*  **URL Params**

   **Required:**
 
   `protocolId = [String] - Agile Id of the protocol`

   `deviceId = [String] - Agile device Id`


*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Status read done.
  * **Code:** 200 <br />
  * **Content:** Connection initialized.

 
* **Error Response:**
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/protocol/{protocolId}/connection/{deviceId}. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Protocol not found : {protocolId}`

* **Example:**

        curl -X POST localhost:8080/services/protocol/{protocolId}/connection/{deviceId} -u "admin:admin" -H "Content-Type: application/json"


* **Notes:**

    Currently method returns immediately with success. No operation is actually performed.
    In curl example {protocolId} and {componentId} need to be switched with real protocol and device names

## Method : Disconnect from device
Disconnect from the device
* **URL**

  /{protocolId}/connection/{deviceId}

* **Request type:**

  `DELETE` 
  
*  **URL Params**

   **Required:**
 
   `protocolId = [String] - Agile Id of the protocol`

   `deviceId = [String] - Agile device Id`


*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Status read done.
  * **Code:** 200 <br />
  * **Content:** Disconnection completed.

 
* **Error Response:**
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/{protocolId}/connection/{deviceId}. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Protocol not found : {protocolId}`

* **Example:**

        curl -X DELETE localhost:8080/services/protocol/{protocolId}/connection/{deviceId} -u "admin:admin" -H "Content-Type: application/json"


* **Notes:**

    Currently method returns immediately with success. No operation is actually performed.

## Method : Perform write on device via protocol
Call a write on device's channels via protocol
* **URL**

  /{protocolId}/{deviceId}

* **Request type:**

  `POST` 
  
*  **URL Params**

   **Required:**
 
   `protocolId = [String] - Agile Id of the protocol`
   `deviceId = [String] - Agile device Id`


*  **Data Params - Payload**

         {"channels": [{"name": "channel_name", "type": "BOOLEAN", "value": ... }, ...]}
 

* **Success Response:**
  
  * **Code:** 200 <br />
  * **Content:** Write succeeded

 
* **Error Response:**

  * **Code:** 400 BAD REQUEST <br />
    **Content:** `Bad request, expected request format: {"channels": [{"name": "channel-1", "type": "BOOLEAN", "value": true }]}`
  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `This combination of device and protocol is not available.`
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/protocol/{protocolId}/{deviceId}. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Protocol not found : {protocolId}`

* **Example:**

        curl -X POST localhost:8080/services/protocol/{protocolId}/{deviceId} -u "admin:admin" -H "Content-Type: application/json" -d "{"channels": [{"name": "testChannel", "type": "BOOLEAN", "value": true }]}"


* **Notes:**

    Method for performing write operation on device's channels connected via specific protocol. Method expects payload as JSON array, performs validation of sent payload, and if it is well formed, performs write operation. In case of malformed payload, 400 BAD_REQUEST occurs. Also, method checks is device connected via protocol sent in {protocolId} and is device and protocol exist.
    * Method doesn't work with modbus protocol becouse  implementation for modbus write still   doesn't exist.
    *  If JSON in payload is malformed (for example "123 "{channels" : + "name" ./}" ) com.google.gson.JsonSyntaxException (500 INTERNAL_SERVER_ERROR ) is thrown.


## Method : Read data via protocol
Call a read via protocol
* **URL**

  /{protocolId}/{deviceId}

* **Request type:**

  `GET` 
  
*  **URL Params**

   **Required:**
 
   `protocolId = [String] - Agile Id of the protocol`
   `deviceId = [String] - Agile device Id`



*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Read suceeded.
  * **Code:** 200 <br />
  * **Content:** 
    
   
        [
            {
                "deviceID": "modBusAsset",
                "componentID": "testChannel",
                "value": "false",
                "unit": "",
                "lastUpdate": "1522067659893",
                "format": ""
            },
            
            {
                "deviceID": "modBusAsset",
                "componentID": "testChannel2",
                "value": "true",
                "unit": "",
                "lastUpdate": "1522067659893",
                "format": ""
                }
        ]


* **Error Response:**
  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `This combination of device and protocol is not available.`
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/protocol/{protocolId}/{deviceId}. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Protocol not found : {protocolId}`

* **Example:**

        curl -X GET localhost:8080/services/protocol/{protocolId}/{deviceId} -u "admin:admin" -H "Content-Type: application/json"

* **Notes:**

    Method performs read operation on all device's components connected via specific protocol , like temperature, humidity, etc.. Method checks if device and protocol exist, are they connected, and then performs read operations.On success response  method returns JSON array of all components records connected via protocol.