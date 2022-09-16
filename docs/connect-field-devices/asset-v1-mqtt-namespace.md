# ASSET-V1 MQTT Namespace

The ASSET-V1 namespace allows to perform remote operations on the assets defined in an Kura-powered device. The requests and responses are represented as JSON arrays placed in the body of the MQTT payload. 

The namespace includes the following topics.



## GET/assets

This topic is used to retrieve metadata describing the assets defined on a specific device and their channel configuration.

### Request format

The request can contain JSON array containing a list of asset names for which the metadata needs to be returned. The request JSON must have the following structure:

```json
[
  {
    "name": "asset1"
  },
  {
    "name": "otherAsset"
  }
]
```

The request JSON is an array with all elements of the type `object`. The array object has the following properties:

- **name** (string, required): the name of the Asset for which the metadata needs to be returned.

If the provided array is empty or the request payload is empty, the metadata describing all assets present on the device will be returned.

### Response format

The response payload contains a JSON array with the following structure:

```json
[
  {
    "name": "asset1",
    "channels": [
      {
        "name": "first_channel",
        "type": "INTEGER",
        "mode": "READ"
      },
      {
        "name": "second_channel",
        "type": "BOOLEAN",
        "mode": "READ_WRITE"
      },
      {
        "name": "other_channel",
        "type": "STRING",
        "mode": "WRITE"
      }
    ]
  },
  {
    "name": "otherAsset",
    "channels": []
  },
  {
    "name": "nonExistingAsset",
    "error": "Asset not found"
  }
]
```

All elements of the array are of the type `object`. The array object has the following properties:

- **name** (string, required): the name of the asset
- **error** (string): this property is present only if the metadata for a not existing asset was explicitly requested, it contains an error message.
- **channels** (array): the list of channels defined on the asset, it can be empty if no channels are defined. This property and the `error` property are mutually exclusive. This object is an array with all elements of the type `object` and they have the following properties:
    - **name** (string, required): the name of the channel.
    - **mode** (string, required): the mode of the channel. The possible values are `READ`, `WRITE` or `READ_WRITE`.
    - **type** (string, required): the value type of the channel. The possible values are `BOOLEAN`, `BYTE_ARRAY`, `DOUBLE`, `INTEGER`, `LONG`, `FLOAT`, `STRING`.



## EXEC/read

This topic is used to perform a read operation on a specified set of assets and channels.

### Request format

The request can contain a JSON array with the following structure:

```json
[
  {
    "name": "asset1",
    "channels": [
      {
        "name": "channel1"
      },
      {
        "name": "channel2"
      },
      {
        "name": "otherChannel"
      }
    ]
  },
  {
    "name": "otherAsset"
  }
]
```

The request JSON is an array with all elements of the type `object`. If the list is empty or if the request payload is empty all channels of all assets will be read. The array object has the following properties:

- **name** (string, required): the name of the asset involved in the read operation
- **channels** (array): the list of the names of the channels to be read, if this property is not present or if its value is an empty array, all channels for the specified asset will be read. The object is an array with all elements of the type `object`. The array object has the following properties:
- **name** (string, required): the name of the channel to be read

### Response Format

The response is returned as a JSON array placed in the body of the response:

```json
[
  {
    "name": "asset1",
    "channels": [
      {
        "name": "first_channel",
        "type": "INTEGER",
        "value": "432",
        "timestamp": 1234550
      },
      {
        "name": "second_channel",
        "type": "BOOLEAN",
        "value": "true",
        "timestamp": 1234550
      },
      {
        "name": "other_channel",
        "error": "Read failed",
        "timestamp": 1234550
      },
      {
        "name": "binary_channel",
        "type": "BYTE_ARRAY",
        "value": "dGVzdCBzdHJpbmcK",
        "timestamp": 1234550
      }
    ]
  },
  {
    "name": "nonExistingAsset",
    "error": "Asset not found"
  }
]
```

The response JSON is an array with all elements of the type `object`. The array object has the following properties:

- **name** (string, required): the name of the asset.
- **error** (string): an error message. This property is present only if a read operation for a not existing asset was explicitly requested.
- **channels** (array): the object is an array with all elements of the type `object`. The array object has the following properties:
- **name** (string, required): the name of the channel.
- **timestamp** (integer, required): the device timestamp associated with the result in milliseconds since the Unix Epoch.
- **type** (string): the type of the result. This property is present only if the operation succeeded. The possible values are `BOOLEAN`, `BYTE_ARRAY`, `DOUBLE`, `INTEGER`, `LONG`, `FLOAT`, `STRING`.
- **value** (string): the result value of the read request encoded as a String. This property is present only if the operation succeeded. If the channel type is `BYTE_ARRAY`, the result will be represented using the base64 encoding.
- **error** (string): an error message. This property is present only if the operation failed.



## EXEC/write

Performs a write operation on a specified set of channels and assets.

### Request format

The request must contain a JSON array with the following structure:

```json
[
  {
    "name": "asset1",
    "channels": [
      {
        "name": "first_channel",
        "type": "INTEGER",
        "value": "432",
      },
      {
        "name": "second_channel",
        "type": "BOOLEAN",
        "value": "true",
      },
      {
        "name": "binary_channel",
        "type": "BYTE_ARRAY",
        "value": "dGVzdCBzdHJpbmcK",
      }
    ]
  }
]
```

The array object has the following properties:

- **name** (string, required): the name of the asset.
- **channels** (array, required): the list of channel names and values to be written. The object is an array with all elements of the type `object`. The array object has the following properties:
    - **name** (string, required): the name of the channel.
    - **type** (string, required): the type of the value to be written. The allowed values are `BOOLEAN`, `BYTE_ARRAY`, `DOUBLE`, `INTEGER`, `LONG`, `FLOAT`, `STRING`.
    - **value** (string, required): the value to be written encoded as a String. If the channel type is **BYTE_ARRAY**, the base64 encoding must be used.

### Response format

The response uses the same format as the `EXEC/read` request, in case of success the **type** and **value** properties in the response will report the same values specified in the request.