# Copyright (c) 2022 Eurotech and/or its affiliates and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#  Eurotech

import numpy as np
import json

import triton_python_backend_utils as pb_utils


class TritonPythonModel:

    def initialize(self, args):
        self.model_config = model_config = json.loads(args['model_config'])

        output0_config = pb_utils.get_output_config_by_name(
            model_config, "ANOMALY_SCORE0")
        output1_config = pb_utils.get_output_config_by_name(
            model_config, "ANOMALY0")

        self.output0_dtype = pb_utils.triton_string_to_numpy(
            output0_config['data_type'])
        self.output1_dtype = pb_utils.triton_string_to_numpy(
            output1_config['data_type'])

    def execute(self, requests):
        output0_dtype = self.output0_dtype
        output1_dtype = self.output1_dtype

        responses = []

        for request in requests:
            THRESHOLD = 0.20

            # Get input
            x_recon = pb_utils.get_input_tensor_by_name(request, "RECONSTR0").as_numpy()
            x_orig = pb_utils.get_input_tensor_by_name(request, "ORIG0").as_numpy()

            # Get Mean square error between reconstructed input and original input
            reconstruction_score = np.mean((x_orig - x_recon)**2, axis=1)

            anomaly = reconstruction_score > THRESHOLD

            # Create output tensors
            out_tensor_0 = pb_utils.Tensor("ANOMALY_SCORE0",
                                           reconstruction_score.astype(output0_dtype))
            out_tensor_1 = pb_utils.Tensor("ANOMALY0",
                                           anomaly.astype(output1_dtype))

            inference_response = pb_utils.InferenceResponse(
                output_tensors=[out_tensor_0, out_tensor_1])
            responses.append(inference_response)

        return responses
