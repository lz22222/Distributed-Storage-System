# raft-java
Raft implementation library for Java.
# Supported Features
* Leader election
* Log replication
* Snapshots
* Dynamic cluster membership changes

## Quick Start
Deploy a set of 3 Raft instances on a single local machine by executing the following script:<br>
cd raft-java-example && sh deploy.sh <br>
This script will deploy three instances named example1, example2, and example3 in the raft-java-example/env directory;<br>
It will also create a client directory for testing the read and write functionality of the Raft cluster.<br>
After successful deployment, test writing operations with the following script:
cd env/client <br>
./bin/run_client.sh "list://127.0.0.1:8051,127.0.0.1:8052,127.0.0.1:8053" hello world <br>
To test reading operations, use the command:<br>
./bin/run_client.sh "list://127.0.0.1:8051,127.0.0.1:8052,127.0.0.1:8053" hello

# How to Use
use the raft-java dependency library to implement a distributed storage system

## Define Data Writing and Reading Interfaces
```protobuf
message SetRequest {
    string key = 1;
    string value = 2;
}
message SetResponse {
    bool success = 1;
}
message GetRequest {
    string key = 1;
}
message GetResponse {
    string value = 1;
}
```
```java
public interface ExampleService {
    Example.SetResponse set(Example.SetRequest request);
    Example.GetResponse get(Example.GetRequest request);
}
```

## Server Usage Method
1. Implement the StateMachine interface implementation class
```java
// The three methods of this interface are primarily called internally by Raft
public interface StateMachine {
    /**
     * Performs a snapshot of the data in the state machine, called locally by each node periodically
     * @param snapshotDir Directory for outputting snapshot data
     */
    void writeSnapshot(String snapshotDir);
    /**
     * Reads the snapshot into the state machine, called when a node starts
     * @param snapshotDir Directory containing snapshot data
     */
    void readSnapshot(String snapshotDir);
    /**
     * Applies data to the state machine
     * @param dataBytes Binary data
     */
    void apply(byte[] dataBytes);
}
```

2.  Implement the data writing and reading interfaces
```
// The ExampleService implementation class should include the following members
private RaftNode raftNode;
private ExampleStateMachine stateMachine;
```
```
// Main logic for data writing
byte[] data = request.toByteArray();
// Data is synchronously written to the Raft cluster
boolean success = raftNode.replicate(data, Raft.EntryType.ENTRY_TYPE_DATA);
Example.SetResponse response = Example.SetResponse.newBuilder().setSuccess(success).build();
```
```
// Main logic for data reading, implemented by the specific application state machine
Example.GetResponse response = stateMachine.get(request);
```

3. Server Startup Logic
```
// Initialize RPCServer for providing remote procedure call capabilities
RPCServer server = new RPCServer(localServer.getEndPoint().getPort());

// Application state machine, responsible for managing the business logic state
ExampleStateMachine stateMachine = new ExampleStateMachine();

// Configure Raft options, which define the behavior of the Raft protocol
RaftOptions.snapshotMinLogSize = 10 * 1024; // Set minimum log size (10 KB) to trigger a snapshot
RaftOptions.snapshotPeriodSeconds = 30;    // Trigger a snapshot every 30 seconds
RaftOptions.maxSegmentFileSize = 1024 * 1024; // Maximum log segment file size (1 MB)

// Initialize RaftNode, representing the current server's role in the Raft cluster (Follower, Candidate, or Leader)
RaftNode raftNode = new RaftNode(serverList, localServer, stateMachine);

// Register the service for communication between Raft nodes, used for Raft node communication and elections
RaftConsensusService raftConsensusService = new RaftConsensusServiceImpl(raftNode);
server.registerService(raftConsensusService);

// Register the Raft service for clients, allowing clients to interact with the cluster
RaftClientService raftClientService = new RaftClientServiceImpl(raftNode);
server.registerService(raftClientService);

// Register the application-specific service provided by the server
ExampleService exampleService = new ExampleServiceImpl(raftNode, stateMachine);
server.registerService(exampleService);

// Start the RPCServer and initialize the Raft node
server.start();
raftNode.init();
```

<img width="1440" alt="🌟" src="https://github.com/user-attachments/assets/01d05e88-45df-4129-910a-196ea658fbda" />
