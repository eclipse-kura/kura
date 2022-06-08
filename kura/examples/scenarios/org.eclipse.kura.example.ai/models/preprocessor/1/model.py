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
            model_config, "INPUT0")

        self.output0_dtype = pb_utils.triton_string_to_numpy(
            output0_config['data_type'])

    def execute(self, requests):
        output0_dtype = self.output0_dtype

        responses = []

        for request in requests:
            acc_x      = pb_utils.get_input_tensor_by_name(request, "ACC_X").as_numpy()
            acc_y      = pb_utils.get_input_tensor_by_name(request, "ACC_Y").as_numpy()
            acc_z      = pb_utils.get_input_tensor_by_name(request, "ACC_Z").as_numpy()
            gyro_x     = pb_utils.get_input_tensor_by_name(request, "GYRO_X").as_numpy()
            gyro_y     = pb_utils.get_input_tensor_by_name(request, "GYRO_Y").as_numpy()
            gyro_z     = pb_utils.get_input_tensor_by_name(request, "GYRO_Z").as_numpy()
            humidity   = pb_utils.get_input_tensor_by_name(request, "HUMIDITY").as_numpy()
            pressure   = pb_utils.get_input_tensor_by_name(request, "PRESSURE").as_numpy()
            temp_hum   = pb_utils.get_input_tensor_by_name(request, "TEMP_HUM").as_numpy()
            temp_press = pb_utils.get_input_tensor_by_name(request, "TEMP_PRESS").as_numpy()

            out_0 = np.array([acc_y, acc_x, acc_z, pressure, temp_press, temp_hum, humidity, gyro_x, gyro_y, gyro_z]).transpose()

            #                  ACC_Y     ACC_X     ACC_Z    PRESSURE   TEMP_PRESS   TEMP_HUM   HUMIDITY    GYRO_X    GYRO_Y    GYRO_Z
            min = np.array([-0.132551, -0.049693, 0.759847, 976.001709, 38.724998, 40.220890, 13.003981, -1.937896, -0.265019, -0.250647])
            max = np.array([ 0.093099, 0.150289, 1.177543, 1007.996338, 46.093750, 48.355824, 23.506138, 1.923712, 0.219204, 0.671759])

            # MinMax scaling
            out_0_scaled = (out_0 - min)/(max - min)

            # Create output tensor
            out_tensor_0 = pb_utils.Tensor("INPUT0",
                                           out_0_scaled.astype(output0_dtype))

            inference_response = pb_utils.InferenceResponse(
                output_tensors=[out_tensor_0])
            responses.append(inference_response)

        return responses
