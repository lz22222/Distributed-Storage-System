package com.github.raftimpl.raft;

/**
 * Interface for the Raft state machine.
 */
public interface StateMachine {
    /**
     * Takes a snapshot of the state machine's data. This method is called periodically on each node locally.
     * @param snapshotDir The directory of the existing snapshot.
     * @param tmpSnapshotDataDir The directory for the new snapshot data.
     * @param raftNode The Raft node initiating the snapshot.
     * @param localLastAppliedIndex The highest log entry index that has been applied to the replicated state machine.
     */
    void writeSnapshot(String snapshotDir, String tmpSnapshotDataDir, RaftNode raftNode, long localLastAppliedIndex);

    /**
     * Reads a snapshot into the state machine. This method is called when a node starts up.
     * @param snapshotDir The directory where snapshot data is stored.
     */
    void readSnapshot(String snapshotDir);

    /**
     * Applies the given data to the state machine.
     * @param dataBytes The binary data to be applied.
     */
    void apply(byte[] dataBytes);

    /**
     * Reads data from the state machine.
     * @param dataBytes The binary key data.
     * @return The binary value data corresponding to the key.
     */
    byte[] get(byte[] dataBytes);
}
