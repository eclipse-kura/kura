---
layout: page
title:  "Nvidia™ Triton Server inference engine"
categories: [builtin]
---

The [Nvidia™ Triton Server](https://developer.nvidia.com/nvidia-triton-inference-server) is an open-source inference service software that enables the user to deploy trained AI models from any framework on GPU or CPU infrastructure. It supports all major frameworks like TensorFlow, TensorRT, PyTorch, ONNX Runtime, and even custom framework backend. With specific backends, it is also possible to run Python scripts, mainly for pre-and post-processing purposes, and exploit the [DALI](https://github.com/triton-inference-server/dali_backend) building block for optimized operations. For more detail about the Triton Server, please refer to the official [website](https://github.com/triton-inference-server/server).

The ESF Triton Server component is the implementation of the inference engine APIs and provides methods for interacting with a local or remote Nvidia™ Triton Server. As presented below, the component enables the user to configure a local server running on the gateway or to communicate to an external server to load specific models.

![triton_server]({{ site.baseurl }}/assets/images/builtin/triton_server.png)

The parameters used to configure the Triton Service are the following:

 - **Nvidia Triton Server address**: the address of the Nvidia Triton Server.
 - **Nvidia Triton Server ports**: the ports used to connect to the server for HTTP, GRPC, and Metrics services.
 - **Inference Models**: a comma-separated list of inference model names that the server will load. The models have to be already present in the filesystem where the server is running. This option simply tells the server to load the given models from a local or remote repository.
 - **Local Nvidia Triton Server**: If enabled, a local native Nvidia Triton Server is started on the gateway. In this case, the model repository and backends path are mandatory. Moreover, the server address property is overridden and set to localhost. Be aware that the Triton Server has to be already installed on the system.
 - **Local model repository path**: Only for a local instance, specify the path on the filesystem where the models are stored.
 - **Local backends path**: Only for a local instance, specify the path on the filesystem where the backends are stored.
 - **Optional configuration for the local backends**: Only for local instance, a semi-colon separated list of configuration for the backends. i.e. tensorflow,version=2;tensorflow,allow-soft-placement=false 
 
> :warning: Warning   
> Pay attention on the ports used for communicating with the Triton Server. The default ports are the 8000-8002, but these are tipically used by ESF for debug purposes.
> Pay attention on the ports used for communicating with the Triton Server. The default ports are the 8000-8002, but these are tipically used by ESF for debug purposes. 
 
## Configuration for a local native Triton Server

When the **Local Nvidia Triton Server** option is set to true, a local instance of the Nvidia™ Triton Server is started on the gateway. The following configuration is required:

 - **Nvidia Triton Server address**: localhost
 - **Nvidia Triton Server ports**: \<mandatory\>
 - **Inference Models**: \<mandatory\>. Note that the models have to be already present on the filesystem.
 - **Local Nvidia Triton Server**: true
 - **Local model repository path**: \<mandatory\>
 - **Local backends path**: \<mandatory\>

The typical command used to start the Triton Server is like this: 
 
```bash
tritonserver --model-repository=<model_repository_path> \
--backend-directory=<backend_repository_path> \
--backend-config=<backend_config> \
--http-port=<http_port> \
--grpc-port=<grpc_port> \
--metrics-port=<metrics_port> \
--model-control-mode=explicit \
--load-model=<model_name_1> \
--load-model=<model_name_2> \
...
```

## Configuration for a local Triton Server running in a Docker container

If the Nvidia™ Triton Server is running as a Docker container in the gateway, the following configuration is required:

 - **Nvidia Triton Server address**: localhost
 - **Nvidia Triton Server ports**: \<mandatory\>
 - **Inference Models**: \<mandatory\>. The models have to be already present on the filesystem.
 - **Local Nvidia Triton Server**: false

In order to correctly load the models at runtime, configure the server with the `--model-control-mode=explicit` option. The typical command used for running the docker container is as follows. Note the forward of the ports to not interfere with ESF.
 
```bash
docker run --rm \
-p4000:8000 \
-p4001:8001 \
-p4002:8002 \
--shm-size=150m \
-v path/to/models:/models \
nvcr.io/nvidia/tritonserver:[version] \
tritonserver --model-repository=/models --model-control-mode=explicit
```

## Configuration for a remote Triton Server

When the Nvidia™ Triton Server is running on a remote server, the following configuration is needed:

 - **Nvidia Triton Server address**: \<mandatory\>
 - **Nvidia Triton Server ports**: \<mandatory\>
 - ** Inference Models**: \<mandatory\>. The models have to be already present on the filesystem.
 - **Local Nvidia Triton Server**: false
