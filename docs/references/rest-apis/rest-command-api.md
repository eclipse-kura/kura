# Rest Command v1 API

#### Execute Command
- Method: POST
- API PATH: `/services/command/v1/command/`
##### Request Body
``` JSON
{
	//Command to be excuted on gateway
    "command":"printenv TextEnvVarName1",

    //Service Password for command Service
    "password":"s3curePassw0rd",

    //String base64 encoding of a zip file to transfer to gateway
    "zipBytes": "UEsDBAoACAAAAIyD1lYAA AAAAAAAAAAAAAAJACAAdGVzdGZpbGUxVVQNAAfprpRk6a6UZOmulGR1eAsAAQT1AQAABBQAAABQSwcIAAAAAAAAAAAAAAAAUEsBAgoDCgAIAAAAjIPWVgAAAAAAAAAAAAAAAAkAIAAAAAAAAAAAAKSBAAAAAHRlc3RmaWxlMVVUDQAH6a6UZOmulGTprpRkdXgLAAEE9QEAAAQUAAAAUEsFBgAAAAABAAEAVwAAAFcAAAAAAA==",

    //Command argument String array
    "arguments":["arg 1"],

    //Shell environment Pairs Map
    "environmentPairs": 
    {
        "TextEnvVarName1":"TextEnvVarValue1",
        "TextEnvVarName2":"TextEnvVarValue2"
    },
    //Working directory of command to be executed
    "workingDirectory":"/tmp",
    
    //Run command synchronously/asynchronously
    "isRunAsync":false
}
```

##### Responses
- 200 OK status

```JSON
{
    "stdout": "Command error output is displayed in this field",
    "stderr": "Command output is displayed in this field",
    "exitCode": 0,
    "isTimeOut": false
}
```

- 400 Bad Request (Malformed Client JSON)
- 404 Resource Not Found
- 500 Internal Server Error



!!! note

    Use the following command to retrieve the base64 representation of a zip file. `base64 -i <filename.zip>`