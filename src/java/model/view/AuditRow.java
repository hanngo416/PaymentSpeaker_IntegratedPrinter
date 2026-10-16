package model.view;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.Timestamp;

public class AuditRow {

    private int logId;
    private String actor;
    private String action;
    private String entity;
    private String entityId;
    private String detail;
    private String prevHash;
    private String currHash;
    private Timestamp createdAt;

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }

    public String getCurrHash() { return currHash; }
    public void setCurrHash(String currHash) { this.currHash = currHash; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getSummary() {
        String who = entityId;
        switch (action) {
            case "INIT_SYSTEM": return "System started: first record of the chain";
            case "LOGIN": return "Logged in";
            case "LOGOUT": return "Logged out";
            case "CREATE_ORDER": return "Created order " + who + ": total " + field("amount") + " VND, cash "
                    + field("cash") + ", QR " + field("transfer");
            case "CANCEL_ORDER": return "Cancelled order " + who;
            case "TRANSACTION_INGEST":
                String res = field("status").isEmpty() ? field("label") : field("status");
                if ("LEGIT".equals(res)) { res = "COMPLETED"; }
                return "Received bank transfer " + who + " of " + field("amount") + " VND"
                        + (field("order").isEmpty() ? " (no order)" : " for order " + field("order"))
                        + ", status: " + res;
            case "PRINT_RECEIPT": return "Printed the receipt of order " + field("order");
            case "REPRINT_RECEIPT": return "Printed a copy of the receipt of order " + field("order");
            case "OPEN_SHIFT": return "Opened shift " + who;
            case "CLOSE_SHIFT": return "Closed shift " + who + (field("forced").isEmpty()
                    ? ", cash counted " + field("counted_cash") + " VND" : " by force (no cash count)");
            case "PRINT_REPORT": return "Printed report " + who;
            case "EXPORT_INVOICES": return "Exported invoices " + who;
            case "ALERT_START_REVIEW": return "Started reviewing alert #" + who;
            case "ALERT_RESOLVE": return "Resolved alert #" + who + notes();
            case "ALERT_DISMISS": return "Dismissed alert #" + who + notes();
            case "CREATE_USER": return "Created account " + who + " (" + field("role") + ")";
            case "UPDATE_USER": return "Edited account " + who + ": role " + field("role") + ", email " + field("email");
            case "LOCK_USER": return "Locked account " + who;
            case "UNLOCK_USER": return "Unlocked account " + who;
            case "DELETE_USER": return "Deleted account " + who;
            case "APPROVE_USER": return "Approved account " + who + " as " + field("role");
            case "UNLINK_GOOGLE": return "Unlinked the Google login of " + who;
            case "DEVICE_STATUS": return "Device " + who + " changed from " + field("from") + " to " + field("to");
            case "CREATE_TICKET": return "Reported a problem on device #" + field("deviceId") + ": " + field("issue");
            case "UPDATE_TICKET": return "Updated maintenance ticket #" + who + " to " + field("status");
            default: return action + " " + entity + " " + who;
        }
    }

    private String notes() {
        return field("notes").isEmpty() ? "" : " (" + field("notes") + ")";
    }

    private String field(String name) {
        try {
            JsonObject o = JsonParser.parseString(detail == null ? "{}" : detail).getAsJsonObject();
            return o.has(name) ? o.get(name).getAsString() : "";
        } catch (RuntimeException e) {
            return "";
        }
    }
}
