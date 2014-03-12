package org.opendaylight.controller.flowstoragemanager.web;

import java.util.List;
import java.util.Map;

public class FlowstoragemanagerJsonBean {
    private List<String> columnNames;
    private List<Map<String, String>> nodeData;

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<Map<String, String>> getNodeData() {
        return nodeData;
    }

    public void setNodeData(List<Map<String, String>> nodeData) {
        this.nodeData = nodeData;
    }
}
