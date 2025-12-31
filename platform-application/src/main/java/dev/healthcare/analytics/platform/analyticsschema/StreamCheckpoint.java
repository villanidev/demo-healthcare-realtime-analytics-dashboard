package dev.healthcare.analytics.platform.analyticsschema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stream_checkpoint", schema = "analytics")
public class StreamCheckpoint {

    @Id
    @Column(name = "stream_name", nullable = false, length = 64)
    private String streamName;

    @Column(name = "last_processed_event_id", nullable = false)
    private long lastProcessedEventId;

    public StreamCheckpoint() {
    }

    public StreamCheckpoint(String streamName, long lastProcessedEventId) {
        this.streamName = streamName;
        this.lastProcessedEventId = lastProcessedEventId;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public long getLastProcessedEventId() {
        return lastProcessedEventId;
    }

    public void setLastProcessedEventId(long lastProcessedEventId) {
        this.lastProcessedEventId = lastProcessedEventId;
    }
}
