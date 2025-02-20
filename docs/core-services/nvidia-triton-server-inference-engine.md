# Nvidia™ Triton Server Inference Engine

The [Nvidia™ Triton Server](https://developer.nvidia.com/nvidia-triton-inference-server) is an open-source inference service software that enables the user to deploy trained AI models from any framework on GPU or CPU infrastructure. It supports all major frameworks like TensorFlow, TensorRT, PyTorch, ONNX Runtime, and even custom framework backend. With specific backends, it is also possible to run Python scripts, mainly for pre-and post-processing purposes, and exploit the [DALI](https://github.com/triton-inference-server/dali_backend) building block for optimized operations. For more detail about the Triton Server, please refer to the official [website](https://github.com/triton-inference-server/server).

Kura provides three components for exposing the Triton Server service functionality which implement the inference engine APIs and provides methods for interacting with a local or remote Nvidia™ Triton Server:

- **TritonServerRemoteService**: provides methods for interacting with a remote Nvidia™ Triton Server without managing the server lifecycle. Can be used both for connecting to a remote instance or a local non-managed instance. It exposes a simpler but more limited configuration.
- **TritonServerNativeService**: provides methods for interacting with a local native Nvidia™ Triton Server. Requires the Triton Server executable to be already available on the device and offers more options and features (like AI Model Encryption).
- **TritonServerContainerService**: provides methods for interacting with a local container running Nvidia™ Triton Server. Requires the Triton Server container image to be already available on the device and offers more options and features (like AI Model Encryption).



## Nvidia™ Triton Server installation

Before running Kura's Triton Server Service, you must install the Triton Inference Server. Here you can find the necessary steps for the available suggested installation methods.

### Native Triton installation on Jetson devices

A release of Triton for JetPack is provided in the tar file in the Triton Inference Server [release notes](https://github.com/triton-inference-server/server/releases). Full documentation is available [here](https://github.com/triton-inference-server/server/blob/main/docs/jetson.md).

Installation steps:

- Before running the executable you need to install the [Runtime Dependencies for Triton](https://github.com/triton-inference-server/server/blob/main/docs/jetson.md#runtime-dependencies-for-triton).
- After doing so you can extract the tar file and run the executable in the `bin` folder.
- It is highly recommended to add the `tritonserver` executable to your path or symlinking the executable to `/usr/local/bin`.

### Triton Docker image installation

Before you can use the Triton Docker image you must install [Docker](https://docs.docker.com/engine/install). If you plan on using a GPU for inference you must also install the [NVIDIA Container Toolkit](https://github.com/NVIDIA/nvidia-docker).

Pull the image using the following command.

```
$ docker pull nvcr.io/nvidia/tritonserver:<xx.yy>-py3
```

Where `<xx.yy>` is the version of Triton that you want to pull.

### Native Triton installation on supported devices

The official docs mention the possibility to perform a native installation on supported platform by [extracting the binaries](https://github.com/triton-inference-server/server/blob/main/docs/build.md#extract-build-artifacts) from the Docker images. To do so you must install the necessary dependencies (some can be found in the Jetson [runtime dependencies docs](https://github.com/triton-inference-server/server/blob/main/docs/jetson.md#runtime-dependencies-for-triton)) on the system. For Triton to support NVIDIA GPUs you must install CUDA, cuBLAS and cuDNN referencing the [support matrix](https://docs.nvidia.com/deeplearning/frameworks/support-matrix/index.html).

!!! note
    For Python models the libraries available to the Python model are the ones available for the user running the Triton server. Therefore you'll need to install the libraries through `pip` for the `kurad` user.

### Triton Server setup

The Triton Inference Server serves models from one or more model repositories that are specified when the server is started. The model repository is the directory where you place the models that you want Triton to serve. Be sure to follow [the instructions](https://github.com/triton-inference-server/server/blob/main/docs/model_repository.md) to setup the model repository directory.

Further information about an example Triton Server setup can be found in the [official documentation](https://github.com/triton-inference-server/server/blob/main/docs/quickstart.md).



## Triton Server Remote Service component

The Kura Triton Server Remote Service component is the implementation of the inference engine APIs and provides methods for interacting with a remote (i.e. unmnanaged) Nvidia™ Triton Server. As presented below, the component enables the user to communicate to an external server to load specific models. With this component the server lifecycle (startup, shutdown) won't be handled by Kura and it's the user responsibility to make it available to Kura for connecting.

![Nvidia Triton Server Inference Engine](./images/nvidia-triton-server-inference-engine.png)

The parameters used to configure the Triton Service are the following:

- **Nvidia Triton Server address**: the address of the Nvidia Triton Server.
- **Nvidia Triton Server ports**: the ports used to connect to the server for HTTP, GRPC, and Metrics services.
- **Inference Models**: a comma-separated list of inference model names that the server will load. The models have to be already present in the filesystem where the server is running. This option simply tells the server to load the given models from a local or remote repository.
- **Timeout (in seconds) for time consuming tasks**: Timeout (in seconds) for time consuming tasks like server startup, shutdown or model load. If the task exceeds the timeout, the operation will be terminated with an error.
- **Max. GRPC message size (bytes)**: this field controls the maximum allowed size for the GRPC calls to the server instance. By default, size of 4194304 bytes (= 4.19 MB) is used. Increase this value to be able to send large amounts of data as input to the Triton server (like Full HD images). The Kura logs will show the following error when exceeding such limit:
    ```
    io.grpc.StatusRuntimeException: RESOURCE_EXHAUSTED: gRPC message exceeds maximum size 4194304
    ```

!!! note
    Pay attention on the ports used for communicating with the Triton Server. The default ports are the 8000-8002, but these are tipically used by Kura for debug purposes.



## Triton Server Native Service component

The Kura Triton Server component is the implementation of the inference engine APIs and provides methods for interacting with a local native Nvidia™ Triton Server. As presented below, the component enables the user to configure a local server running on the gateway and handles its lifecycle. This operating mode supports more features for interacting with the server like the [AI Model Encryption](#ai-model-encryption-support).

!!! note
    **Requirement**: `tritonserver` executable needs to be available in the path to the `kurad` user. Be sure to have a working Triton Server installation before configuring the local native Triton Server instance through Kura UI.

The parameters used to configure the Triton Service are the following:

- **Nvidia Triton Server ports**: the ports used to connect to the server for HTTP, GRPC, and Metrics services.
- **Local model repository path**: Specify the path on the filesystem where the models are stored.
- **Local model decryption password**: Specify the password to be used for decrypting models stored in the model repository. If none is specified, models are supposed to be plaintext.
- **Inference Models**: a comma-separated list of inference model names that the server will load. The models have to be already present in the filesystem where the server is running. This option simply tells the server to load the given models from a local or remote repository.
- **Local backends path**: Specify the path on the filesystem where the backends are stored.
- **Optional configuration for the local backends**: A semi-colon separated list of configuration for the backends. i.e. tensorflow,version=2;tensorflow,allow-soft-placement=false 
- **Timeout (in seconds) for time consuming tasks**: Timeout (in seconds) for time consuming tasks like server startup, shutdown or model load. If the task exceeds the timeout, the operation will be terminated with an error.
- **Max. GRPC message size (bytes)**: this field controls the maximum allowed size for the GRPC calls to the server instance.

!!! note
    Pay attention on the ports used for communicating with the Triton Server. The default ports are the 8000-8002, but these are tipically used by Kura for debug purposes.



## Triton Server Container Service component

The Kura Triton Server component is the implementation of the inference engine APIs and provides methods for interacting with a local container running the Nvidia™ Triton Server. As presented below, the component enables the user to configure a local server running on the gateway and handles its lifecycle. This operating mode supports more features for interacting with the server like the [AI Model Encryption](#ai-model-encryption-support).

!!! note
    **Requirement**:
    1. Triton Server container image already installed on the device. For instructions refer to the installation section in this page.
    2. Kura's Container Orchestration Service enabled.

The parameters used to configure the Triton Service are the following:

- **Container Image**: The image the container will be created with.
- **Container Image Tag**: Describes which image version that should be used for creating the container.
- **Nvidia Triton Server ports**: The ports used to connect to the server for HTTP, GRPC, and Metrics services.
- **Local model repository path**: Specify the path on the filesystem where the models are stored.
- **Local model decryption password**: Specify the password to be used for decrypting models stored in the model repository. If none is specified, models are supposed to be plaintext.
- **Inference Models**: A comma-separated list of inference model names that the server will load. The models have to be already present in the filesystem where the server is running. This option simply tells the server to load the given models from a local or remote repository.
- **Local Backends Path**: Specifies the host filesystem path where the backends are stored. This folder will be mounted as a volume inside the Triton container and will override the existing backends. If left blank, the backends provided by the Triton container will be used.
- **Optional configuration for the local backends**: A semi-colon separated list of configuration for the backends. i.e. tensorflow,version=2;tensorflow,allow-soft-placement=false 
- **Memory**: The maximum amount of memory the container can use in bytes. Set it as a positive integer, optionally followed by a suffix of b, k, m, g, to indicate bytes, kilobytes, megabytes, or gigabytes. The minimum allowed value is platform dependent (i.e. 6m). If left empty, the memory assigned to the container will be set to a default value by the native container orchestrator.
- **CPUs**: Specify how many CPUs the Triton container can use. Decimal values are allowed, so if set to 1.5, the container will use at most one and a half cpu resource.
- **GPUs**: Specify how many Nvidia GPUs the Triton container can use. Allowed values are 'all' or an integer number. If there's no Nvidia GPU installed, leave the field empty. If the Nvidia Container Runtime is used, leave the field empty.
- **Runtime**: Specifies the fully qualified name of an alternate OCI-compatible runtime, which is used to run commands specified by the 'run' instruction for the Triton container. Example: `nvidia` corresponds to `--runtime=nvidia`. Note:  when using the Nvidia Container Runtime, leave the **GPUs** field empty. The GPUs available on the system will be accessible from the container by default.
- **Devices**: A comma-separated list of device paths passed to the Triton server container (e.g. `/dev/video0`).
- **Timeout (in seconds) for time consuming tasks**: Timeout (in seconds) for time consuming tasks like server startup, shutdown or model load. If the task exceeds the timeout, the operation will be terminated with an error.
- **Max. GRPC message size (bytes)**: this field controls the maximum allowed size for the GRPC calls to the server instance.

!!! note
    Pay attention on the ports used for communicating with the Triton Server. The default ports are the 8000-8002, but these are typically used by Kura for debug purposes.



## AI Model Encryption Support

For ensuring inference integrity and providing copyright protection of deep-learning models on edge devices, Kura provides decryption capabilities for trained models to be served through the Triton Server.

### How it works

**Prerequisites**: a deep-learning trained model (or more) exists with the corresponding necessary configuration for running on the Triton Server without encryption. A folder containing the required files (model, configuration etc) has been tested on a Triton Server.

**Restrictions**: if model encryption is used, the following restrictions apply:

- model encryption support is only available for a *local* Triton Server instance
- all models in the folder containing the encrypted models *must* be encrypted
- all models *must* be encrypted with OpenPGP-compliant AES 256 cipher algorithm
- all models *must* be encrypted with the same password

Once the development of the deep-learning model is complete, the developer who wants to deploy the model on the edge device in a secure manner can proceed with encrypting the Triton model using the procedure detailed below. After encrypting the model he/she can transfer the file on the edge device using his/her preferred method.

![Nvidia Triton Server Inference Engine Model Encyption](./images/nvidia-triton-server-inference-engine-model-encryption.png)

Kura will keep the stored model protected at all times and have the model decrypted **in runtime only** for use by the Inference Server Runtime. As soon as the model is correctly loaded into memory the decrypted model will be removed from the filesystem.

As an additional security measure, the [Model Repository](https://github.com/triton-inference-server/server/blob/main/docs/model_repository.md) containing the decrypted models will be stored in a temporary subfolder and will feature restrictive permission such that only Kura, the Inference Server and the `root` user will be able to access it.

### Encryption procedure

Given a trained model inside the folder `tf_autoencoder_fp32` (for example) with the following layout (see the [official documentation](https://github.com/triton-inference-server/server/blob/main/docs/model_repository.md) for details):

```
tf_autoencoder_fp32
├── 1
│   └── model.savedmodel
│       ├── assets
│       ├── keras_metadata.pb
│       ├── saved_model.pb
│       └── variables
│           ├── variables.data-00000-of-00001
│           └── variables.index
└── config.pbtxt
```

Compress the model into a zip archive with the following command:

```bash
zip -vr tf_autoencoder_fp32.zip tf_autoencoder_fp32/
```

then encrypt it with the AES 256 algorithm using the following `gpg` command:

```bash
gpg --armor --symmetric --cipher-algo AES256 tf_autoencoder_fp32.zip
```

The resulting archive `tf_autoencoder_fp32.zip.asc` can be transferred to the _Local Model Repository Path_ on the target machine and will be decrypted by Kura.

## Triton Server Metrics and Statistics support

Since version 6.0.0, Eclipse Kura supports metrics and statistics reporting from a generic Inference Engine, leveraging the [InferenceEngineMetricsService](https://github.com/eclipse-kura/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/ai/inference/InferenceEngineMetricsService.java) APIs.

The implementation for the Triton Server allows to retrieve relevant metrics regarding GPU and models from the engine. It is based on the [Nvidia Triton Server Metrics feature](https://github.com/triton-inference-server/server/blob/r24.08/docs/user_guide/metrics.md) and the [Model Statistics Extension](https://github.com/triton-inference-server/server/blob/main/docs/protocol/extension_statistics.md). The feature is enabled using the `Enable Triton Server Metrics` parameter from the Eclipse Kura webUI or the `enable.metrics` property in the snapshot configuration. This property is available only for the Triton Server Native Service and Triton Server Container Service components. In the case of the Triton Server Remote Service, the metrics reporting cannot be configured but it can be available depending on the Triton Server setup.

More in details, the following GPU metrics are supported:

- Power Usage
- Power Limit
- Energy Consumption
- GPU Utilization
- GPU Total Memory
- GPU Used Memory

The metrics are provided in a key-value pairs, whose key is in the format `gpu.metrics.<GPU uuid>` where the `GPU uuid` is an unique identifier of the GPU. The value is in JSON format.
An example of GPU metrics is the following:

```
key : gpu.metrics.GPU-340cec52-80ba-c0df-8511-5f9680aae0ff 
value : 
{
    "gpuUuid" : "GPU-340cec52-80ba-c0df-8511-5f9680aae0ff",
    "gpuStats" : {
        "nvGpuMemoryTotalBytes" : "16101933056.000000",
        "nvGpuPowerUsage" : "20.085000",
        "nvGpuUtilization" : "0.000000",
        "nvGpuPowerLimit" : "60.000000",
        "nvGpuMemoryUsedBytes" : "617611264.000000"
    }
}
```

The format of the model statistic key is `model.metrics.<model name>.<model version>`. The value is in JSON format.
An example of model statistics is reported below:

```
key : model.metrics.preprocessor.1
value : 
{
    "name" : "preprocessor",
    "version" : "1",
    "lastInference" : "1740037894861",
    "inferenceCount" : "20",
    "executionCount" : "20",
    "inferenceStats" : {
        "success" : {
            "count" : "20",
            "ns" : "143434240"
        },
        "fail" : {
            "count" : "0",
            "ns" : "0"
        },
        "queue" : {
            "count" : "20",
            "ns" : "4805536"
        },
        "computeInput" : {
            "count" : "20",
            "ns" : "5873920"
        },
        "computeInfer" : {
            "count" : "20",
            "ns" : "119049856"
        },
        "computeOutput" : {
            "count" : "20",
            "ns" : "13182208"
        },
        "cacheHit" : {
            "count" : "0",
            "ns" : "0"
        },
        "cacheMiss" : {
            "count" : "0",
            "ns" : "0"
        }
    },
    "batchStats" : [
        {
            "batchSize" : "1",
            "computeInput" : {
                "count" : "20",
                "ns" : "5873920"
            },
            "computeInfer" : {
                "count" : "20",
                "ns" : "119049856"
            },
            "computeOutput" : {
                "count" : "20",
                "ns" : "13182208"
            }
        }
    ],
    "memoryUsage" : [],
    "responseStats" : {}
}
```