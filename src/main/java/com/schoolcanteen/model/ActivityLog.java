package com.schoolcanteen.model;

public class ActivityLog {

    private int logId;
    private Integer userId;
    private String eventType;
    private String eventDescription;
    private String eventTimestamp;

    public ActivityLog(int logId, Integer userId,
                       String eventType, String eventDescription,
                       String eventTimestamp) {

        this.logId = logId;
        this.userId = userId;
        this.eventType = eventType;
        this.eventDescription = eventDescription;
        this.eventTimestamp = eventTimestamp;
    }

    public int getLogId() {
        return logId;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public String getEventTimestamp() {
        return eventTimestamp;
    }
}
