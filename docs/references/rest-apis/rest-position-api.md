!!! note

    This API can also be accessed via the RequestHandler with app-id: `POS-V1`.

#### Get Position
- Method: GET
- API PATH: `/services/position/v1/position/`

##### Responses
- 200 OK status

```JSON
{
	//longitude of this position in degrees.
	"longitude": 7.729571119959449,
	
	//latitude of this position in degrees.
	"latitude": 50.45345802194789,
	
	//altitude of this position in meters.
	"altitude": 1000.0,
		
	//the ground speed of this position in meters per second.
	"speed": 1000.0,
		
	//the track of this position in degrees as a compass heading.
	"track": 1000.0
}
```

- 500 Internal Server Error
	- can also occur when GPS Position is not locked
```JSON
{
	"message": "Service unavailable. Position is not locked."
}
```

---
#### Get Date Time
- Method: GET
- API PATH: `/services/position/v1/datetime/`

##### Responses
- 200 OK status

```JSON
{
    //Date and time string in UTC timezone. 
	"dateTime": "2023-07-19T18:26:38Z"
}
```

- 500 Internal Server Error
	- can also occur when GPS Position is not locked 
```JSON
{
	"message": "Service unavailable. Position is not locked."
}
```

---
#### Get Is Position Locked
- Method: GET
- API PATH: `/services/position/v1/islocked/`

##### Responses
- 200 OK status

```JSON
{
	"islocked": false
}
```
- 500 Internal Server Error