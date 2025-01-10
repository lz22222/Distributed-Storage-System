package com.github.raftimpl.raft;

import lombok.Getter;
import lombok.Setter;

/**
 *  Raft configuration options
 */

@Getter
@Setter
public class RaftOptions {

    // Timeout in milliseconds for a follower to become a candidate if no messages are received from the leader
    private int electionTimeoutMilliseconds = 5000;

    // Frequency in milliseconds at which the leader sends heartbeat RPCs, even if no data needs to be sent
    private int heartbeatPeriodMilliseconds = 500;

    // Interval for the snapshot timer execution in seconds
    private int snapshotPeriodSeconds = 3600;
    // Minimum log size in bytes before a snapshot is considered
    private int snapshotMinLogSize = 100 * 1024 * 1024;
    // Maximum bytes per snapshot request; limits data size in each snapshot chunk
    private int maxSnapshotBytesPerRequest = 500 * 1024; // 500k

    // Maximum number of log entries per request
    private int maxLogEntriesPerRequest = 5000;

    // Maximum size of a single segment file in bytes, default is 100MB
    private int maxSegmentFileSize = 100 * 1000 * 1000;

    // Follower must be within this margin of the leader's log index to participate in elections and provide service
    private long catchupMargin = 500;

    // Maximum timeout in milliseconds for waiting to replicate data
    private long maxAwaitTimeout = 1000;

    // Number of threads in the pool for tasks like synchronization and leader election
    private int raftConsensusThreadNum = 20;

    // Whether data writing is asynchronous; true means the leader node returns immediately after saving,
    // and then synchronizes with the follower nodes asynchronously;
    // false means the leader node waits to synchronize with the majority of follower nodes before returning.
    private boolean asyncWrite = false;

    // The parent directory for raft's log and snapshot storage, must be an absolute path
    private String dataDir = System.getProperty("com.github.raftimpl.raft.data.dir");


}
