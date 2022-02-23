package org.eclipse.kura.wire.ai.component.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.ai.inference.InferenceEngineService;
import org.eclipse.kura.ai.inference.ModelInfo;
import org.eclipse.kura.ai.inference.Tensor;
import org.eclipse.kura.ai.inference.TensorDescriptor;

public class InferenceEngineServiceMock implements InferenceEngineService {

    private List<ModelInfo> models;
    private boolean isEngineReady;
    private int timesCalled = 0;

    public InferenceEngineServiceMock() {
        this.models = new ArrayList<>();
        this.timesCalled = 0;
    }

    public void setEngineReady() {
        this.isEngineReady = true;
    }

    public void setEngineNotReady() {
        this.isEngineReady = false;
    }

    public void addModelInfo(ModelInfo info) {
        this.models.add(info);
    }

    public int wasCalledTimes() {
        return this.timesCalled;
    }

    @Override
    public List<Tensor> infer(ModelInfo modelInfo, List<Tensor> inputData) throws KuraException {
        this.timesCalled++;

        if (!this.isEngineReady) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        List<Tensor> inferResult = new ArrayList<>();

        for (ModelInfo model : this.models) {
            for (TensorDescriptor outDesc : model.getOutputs()) {
                List<Object> tensorData = new ArrayList<>();

                for (int dimensions = 0; dimensions < outDesc.getShape().size(); dimensions++) {
                    for (int i = 0; i < outDesc.getShape().get(dimensions); i++) {
                        tensorData.add((Object) 0);
                    }
                }

                inferResult.add(new Tensor(Object.class, outDesc, tensorData));
            }
        }

        return inferResult;
    }

    @Override
    public Optional<ModelInfo> getModelInfo(String modelName) throws KuraException {
        if (!this.isEngineReady) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        for (ModelInfo model : this.models) {
            if (model.getName().equals(modelName)) {
                return Optional.of(model);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isEngineReady() throws KuraException {
        return this.isEngineReady;
    }

    /*
     * Not needed methods
     */

    @Override
    public void loadModel(String modelName, Optional<String> modelPath) throws KuraException {
        // not needed
    }

    @Override
    public void unloadModel(String modelName) throws KuraException {
        // not needed
    }

    @Override
    public boolean isModelLoaded(String modelName) throws KuraException {
        // not needed
        return false;
    }

    @Override
    public List<String> getModelNames() throws KuraException {
        // not needed
        return null;
    }
}
