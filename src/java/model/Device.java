package model;

import java.sql.Timestamp;

public class Device {

    private int deviceId;
    private String deviceCode;
    private String deviceType;
    private String modelName;
    private String status;
    private Timestamp lastPing;

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    // What the device is doing right now: MAINTENANCE when set by hand, OFFLINE when it has not pinged lately, else ONLINE
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getLastPing() {
        return lastPing;
    }

    public void setLastPing(Timestamp lastPing) {
        this.lastPing = lastPing;
    }
}
