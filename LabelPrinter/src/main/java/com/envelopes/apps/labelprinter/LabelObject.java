package com.envelopes.apps.labelprinter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Manu on 7/12/2016.
 */
public class LabelObject {

    protected String labelPath;
    protected String labelPDFPath;
    protected String productIdOrOrderId;
    protected boolean orderLabels = false;
    protected boolean packLabel = false;
    protected List<LabelObject> relatedLabelObjects = new ArrayList<>();

    public LabelObject(String productIdOrOrderId) {
        this.productIdOrOrderId = productIdOrOrderId;
        this.orderLabels = LabelHelper.isOrderId(productIdOrOrderId);
    }

    public String getLabelPath() {
        return labelPath;
    }

    public void setLabelPath(String labelPath) {
        this.labelPath = labelPath;
    }

    public String getLabelPDFPath() {
        return labelPDFPath;
    }

    public void setLabelPDFPath(String labelPDFPath) {
        this.labelPDFPath = labelPDFPath;
    }

    public String getProductIdOrOrderId() {
        return productIdOrOrderId;
    }

    public boolean isOrderLabels() {
        return orderLabels;
    }

    public boolean isPackLabel() {
        return packLabel;
    }

    public List<LabelObject> getRelatedLabelObjects() {
        return relatedLabelObjects;
    }
}
