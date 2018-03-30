**DeviceRestService**
----
  Device rest api - Rest services implemented in order to read / write data from specific Kura devices and components.

**Context-path: /device**

## Method : Get device status
Indicate the current device status
* **URL**

  /{deviceId}/status

* **Request type:**

  `GET` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`



*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Status read done.
  * **Code:** 200 <br />
  * **Content:** 
    ` {"status": "CONNECTED"}`

 
* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}/status. Reason: Unauthorized`
* **Example:**

        curl -X GET localhost:8080/services/device/{deviceId}/status -u "admin:admin" -H "Content-Type: application/json"


* **Notes:**

    Method returns positive response if the device existss. In curl command {deviceId} needs to be switched with real device name

## Method : Get data from all device's components
Read values of all components from the device
* **URL**

  /{deviceId}

* **Request type:**

  `GET` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`



*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Read done.
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

  * **Description :** No data available.
  * **Code:** 204 <br />
* **Error Response:**

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`

* **Example:**

        curl -X GET localhost:8080/services/device/{deviceId} -u "admin:admin" -H "Content-Type: application/json"

* **Notes:**

    Method performs read operation on all device's components, like temperature, humidity, etc.. Method checks if device exists and then performs read operation. If device doesn't have any componets attached, no content response is returned. Method returns JSON array of all components records.
    * Method is tested for modbus protocol.

## Method : Connect device
Connect the device at protocol level
* **URL**

  /{deviceId}/connection

* **Request type:**

  `POST` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`



*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Device connected.
  * **Code:** 200 <br />
  * **Content:** 
    ` Device connected.`

 
* **Error Response:**
 
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}/connection. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`

* **Example:**

        curl -X POST localhost:8080/services/device/{deviceId}/connection -u "admin:admin" -H "Content-Type: application/json"


* **Notes:**

    Currently, method returns immediately with success. No operation is actually performed.

## Method : Disconnect device
Disconnect device at protocol level
* **URL**

  /{deviceId}/connection

* **Request type:**

  `DELETE` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`



*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Device disconnected.
  * **Code:** 200 <br />
  * **Content:** 
    ` Device disconnected.`

 
* **Error Response:**

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}/connection. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`

* **Example:**

        curl -X DELETE localhost:8080/services/device/{deviceId}/connection -u "admin:admin" -H "Content-Type: application/json"


* **Notes:**

    Currently, method returns immediately with success. No operation is actually performed.

## Method : Execute command on the device
Perform read or write action on the device : 
   * **Write :** will write the specified data to the corresponding channels of the selected device
   * **Read :** will read the specified channels in the request body
   
* **URL**

  /{deviceId}/execute/{command}

* **Request type:**

  `POST` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`

   `command = [String] - Operation name to be performed (read or write)`


*  **Data Params - Payload**
    
    **Write**

        {"channels": [{"name": "channel_name", "type": "BOOLEAN", "value": ... }, ...]}
    **Read**
    
        {"channels": [ "channel_name1", "channel_name2", ...]}"

* **Success Response:**
  
  * **Description :** Action sent.
  * **Code:** 200 <br />
  * **Content:** 
    ` Action sent.`
  * **Description :** Action sent, no response received.
  * **Code:** 204 <br />

 
* **Error Response:**
  * **Code:** 400 BAD REQUEST (for write)<br />
    **Content:** `Bad request, expected request format: {"channels": [{"name": "channel-1", "type": "BOOLEAN", "value": true }]}`
  * **Code:** 400 BAD REQUEST (for read)<br />
    **Content:** `Bad request, expected request format: {"channels": [ "channel-1", "channel-2"]}`
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}/execute/{command}. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Specified channel(s) not found`
* **Example:**
  
  Read
    
        curl -X POST localhost:8080/services/device/{deviceId}/execute/read -u "admin:admin" -H "Content-Type: application/json" -d "{"channels": ["testChannel"]}"
  
  Write

        curl -X POST localhost:8080/services/device/{deviceId}/execute/write -u "admin:admin" -H "Content-Type: application/json" -d "{"channels": [{"name": "testChannel", "type": "BOOLEAN", "value": true }]}"

* **Notes:**

    Method for performing read or write action on the device, on specific channels specified in the payload. Method takes Json sent from payload,and then, depends on command, performs an action, using Kura api. If device or channel sent in request doesn't exist, error 404 occures. Also, one needs to send JSON properly formed and in right format.  
    Method doesn't return any data, only message about command execution.
    * Method is tested for modbus protocol

## Method : Subscribe to device
Enable a subscription to a data stream. 
* **URL**

  /{deviceId}/{componentId}/subscribe

* **Request type:**

  `POST` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`

   `componentId = [String] - Agile component name, like a sensor`



*  **Data Params - Payload**

    None
 

* **Success Response:**
    None
 
* **Error Response:**

  * **Code:** 501 NOT IMPLEMENTED <br />
    **Content:** `Subscribe is not implemented`
* **Example:**

        curl -X POST localhost:8080/services/device/{deviceId}/{componentId}/subscribe -u "admin:admin" -H "Content-Type: application/json"


* **Notes:**

    Currently, this is  not implemented.

## Method : Unsubscribe from device
Unsubscribe from a data stream. 
* **URL**

  /{deviceId}/{componentId}/subscribe

* **Request type:**

  `DELETE` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`

   `componentId = [String] - Agile component name, like a sensor`



*  **Data Params - Payload**

    None
 

* **Success Response:**
    None
 
* **Error Response:**

  * **Code:** 501 NOT IMPLEMENTED <br />
    **Content:** `Subscribe is not implemented`
* **Example:**

        curl -X DELETE localhost:8080/services/device/{deviceId}/{componentId}/subscribe -u "admin:admin" -H "Content-Type: application/json"


* **Notes:**

    Currently, this is not implemented.

## Method : Read data from specific device component
Perform read from a device's component
   
* **URL**

  /{deviceId}/{componentId}

* **Request type:**

  `GET` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`

   `componentId = [String] - Device component name, like a Temperature`


*  **Data Params - Payload**
    
    None

* **Success Response:**
  
  * **Description :** Read done.
  * **Code:** 200 <br />
  * **Content:** 

        {
            "deviceID": "modBusAsset",
            "componentID": "testChannel",
            "value": "false",
            "unit": "",
            "lastUpdate": "1522067659893",
            "format": ""
        }
  * **Description :** No data available.
  * **Code:** 204 <br />

 
* **Error Response:**
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}/{componentId}. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Component not found`
* **Example:**
  
        curl -X GET 10.89.40.28/services/device/{deviceId}/{componentId}l -u "admin:admin" -H "Content-Type: application/json"

* **Notes:**

    Method for performing read from specific device's channel. Method checks if device and component exist and performs a read from component sent in url. Record from component is returnd as JSON object.

## Method : Write data to specific device channel
Write to a device's component

* **URL**

  /{deviceId}/{componentId}

* **Request type:**

  `POST` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`

   `componentId = [String] - Device component name, like a Temperature`


*  **Data Params - Payload**
    
          {"channels": [{"name": "channel_name", "type": "BOOLEAN", "value": ... }, ...]}

* **Success Response:**
  
  * **Description :** Read done.
  * **Code:** 200 <br />
  * **Content:**  Write sent

  * **Description :** No data available.
  * **Code:** 204 <br />

 
* **Error Response:**
  * **Code:** 400 BAD REQUEST (for write)<br />
    **Content:** `Bad request, expected request format: {"channels": [{"name": "channel-1", "type": "BOOLEAN", "value": true }]}`
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}/{componentId}. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Component not found`
* **Example:**
  
        curl -X POST localhost:8080/services/device/modBusAsset/execute/write -u "admin:admin" -H "Content-Type: application/json" -d "{"channels": [{"name": "testChannel", "type": "BOOLEAN", "value": true }]}"

* **Notes:**

    Method for performing write operation to specific device's channel. Method checks if device and component exist and performs write operation on channel sent in payload / url. Component id from url and payload must match in order to perform write. Method is tested with modbus protocol.
## Method : Get data from all device's components
Read from a device's component
* **URL**

  /{deviceId}/{componentId}

* **Request type:**

  `GET` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`
   `componentId = [String] - Device's component name, like a Temperature`


*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Read done.
  * **Code:** 200 <br />
  * **Content:** 
    
  
        
            {
                "deviceID": "modBusAsset",
                "componentID": "testChannel",
                "value": "false",
                "unit": "",
                "lastUpdate": "1522067659893",
                "format": ""
            }

  * **Description :** No data available.
  * **Code:** 204 <br />
* **Error Response:**
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}/{componentId}. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
* **Example:**

        curl -X GET localhost:8080/services/device/{deviceId}/{componentId} -u "admin:admin" -H "Content-Type: application/json"
* **Notes:**

    Method performs read operation on specific device component, like temperature. Method checks if device exists and performs read operation on specified device's components . If device doesn't have any componets attached, no content response is returned. 
    If read is successful , method returns record from component as JSON object.

## Method : Read last record from component
Get the last record fetched from the component
* **URL**

  /{deviceId}/{componentId}/lastUpdate

* **Request type:**

  `GET` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`
   `componentId = [String] - Device component name, like a Temperature`


*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Read done.
  * **Code:** 200 <br />
  * **Content:** 
    
  
        
            {
                "deviceID": "modBusAsset",
                "componentID": "testChannel",
                "value": "false",
                "unit": "",
                "lastUpdate": "1522067659893",
                "format": ""
            }

  * **Description :** No data available.
  * **Code:** 204 <br />
* **Error Response:**
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}/{componentId}/lastUpdate. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
* **Example:**

        curl -X GET localhost:8080/services/device/{deviceId}/{componentId}/lastUpdate -u "admin:admin" -H "Content-Type: application/json"
* **Notes:**

    Method gets the last record fetched from the device's component, like temperature. Method checks if device exists and performs read operation on specific component, last updated record . If device doesn't have any componets attached, no content response is returned. If read is successful, method returns  last updated record in form of JSON.
    * Method is tested on modbus protocol.
    * Kura still doesn't have caching of reads, so this method, in basic, performs new read from component.

## Method : Read last record from component
Get the last record fetched from the device
* **URL**

  /{deviceId}/lastUpdate

* **Request type:**

  `GET` 
  
*  **URL Params**

   **Required:**
 
   `deviceId = [String] - Agile device Id`


*  **Data Params - Payload**

    None
 

* **Success Response:**
  
  * **Description :** Read done.
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

  * **Description :** No data available.
  * **Code:** 204 <br />
* **Error Response:**
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `Problem accessing /services/device/{deviceId}/lastUpdate. Reason: Unauthorized`
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Device not found : {deviceId}`
* **Example:**

        curl -X GET localhost:8080/services/device/{deviceId}/lastUpdate -u "admin:admin" -H "Content-Type: application/json"
* **Notes:**

    Method gets the last record fetched from the device's component, like temperature. Method checks if device exists and performs read operation on specific component, last updated record . If device doesn't have any componets attached, no content response is returned. If read is successful, method returns  record from all device's component in form of JSON.
    * Method is tested on modbus protocol.
    * Kura still doesn't have caching of reads, so this method, in basic, performs new read from all device's component.
