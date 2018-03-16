# PublisherRestService

Cloud publishing service exposing a single method for publishing an arbitrary payload to a configured cloud service through REST API. 
**Context-path: /cloud-publisher**
___
## Method /publish

#### Parameters
**payload** (required) - The request body, containing list of  metrics described by: 
- name (String)
- type (String) - valid types are: string, double, int, float, long, boolean, base64Binary
- value (String, Number, Boolean)

**Request in json format:**
```json
{
  "metrics": [
    {
      "name": "...",
      "type": "...",
      "value": "..."
    }, ...
  ]
}
```

#### Responses
##### Response codes
- **200 - OK!**
In addition the internal messageID is returned.
- **400 - Bad request!**
Error due to badly formed request. Additional description is shown.
- **500 Server Error**

#### Example
- **CURL:** ```sh curl -X POST localhost/services/publish/publish -u "admin:admin" -H "Content-Type: application/json" -d "{"metrics" : [{ "name" : "temperature", "type" : "double", "value" : "15.5" }]}" ```
- **URL:** localhost/services/publish/publish
- **Body:** {"metrics" : [{ "name" : "temperature", "type" : "double", "value" : "15.5" }]}
- **Response:** 43

#### Additional notes
- When value in request exceed max value of data type defined in type attribute, it is set to max possible value for that data type
- "type" validation is case-insensitive (so "INT" will work as well as "int")

___

## Configuration
- `cloud.service.pid` - pid of the Cloud Service that is used to publish messages to the cloud platform
    - **default:** org.eclipse.kura.cloud.CloudService
- `app.id` - application ID
    - **default:** REST_PUBLISHER
- `publish.qos` - QoS to publish the messages with
    - **default:** 0
    - **options:**
        - 0 - At most once
        - 1 - At least once
        - 2 - Exactly once
- `publish.retain` - retaing flag for the published messages
    - **default:** false
- `publish.appTopic` - semantic topic to publish the metrics to
    - **default:** data/metrics