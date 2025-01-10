package com.github.raftimpl.raft.example.server;

import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import com.github.raftimpl.raft.RaftNode;
import com.github.raftimpl.raft.RaftOptions;
import com.github.raftimpl.raft.StateMachine;
import com.github.raftimpl.raft.example.server.machine.LevelDBStateMachine;
import com.github.raftimpl.raft.example.server.service.ExampleService;
import com.github.raftimpl.raft.example.server.service.impl.ExampleServiceImpl;
import com.github.raftimpl.raft.proto.RaftProto;
import com.github.raftimpl.raft.service.RaftClientService;
import com.github.raftimpl.raft.service.RaftConsensusService;
import com.github.raftimpl.raft.service.impl.RaftClientServiceImpl;
import com.github.raftimpl.raft.service.impl.RaftConsensusServiceImpl;

import java.util.ArrayList;
import java.util.List;

public class ServerMain {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.printf("Usage: ./run_server.sh DATA_PATH CLUSTER CURRENT_NODE\n");
            System.exit(-1);
        }
        // parse args
        // raft data dir
        System.out.print(args);
        String dataPath = args[0];
        // peers, format is "host:port:serverId,host2:port2:serverId2"
        String servers = args[1];
        String[] splitArray = servers.split(",");
        List<RaftProto.Server> serverList = new ArrayList<>();
        for (String serverString : splitArray) {
            RaftProto.Server server = parseServer(serverString);
            serverList.add(server);
        }
        // local server
        RaftProto.Server localServer = parseServer(args[2]);

        // Initialize RPCServer
        RpcServerOptions options = new RpcServerOptions();
        options.setIoThreadNum(Runtime.getRuntime().availableProcessors() * 10);
        options.setWorkThreadNum(Runtime.getRuntime().availableProcessors() * 10);
        RpcServer server = new RpcServer(localServer.getEndpoint().getPort(), options);

        // Set Raft options, for example:
        // just for test snapshot
        RaftOptions raftOptions = new RaftOptions();
        raftOptions.setDataDir(dataPath);
        raftOptions.setSnapshotMinLogSize(10 * 1024); // Set the minimum log size for snapshot
        raftOptions.setSnapshotPeriodSeconds(30); // Set the snapshot period in seconds
        raftOptions.setMaxSegmentFileSize(1024 * 1024); // Set maximum file size for a segment

        // Apply state machine
        StateMachine stateMachine =
                //    new HashMapStateMachine(raftOptions.getDataDir());
                new LevelDBStateMachine(raftOptions.getDataDir()); // Using LevelDB state machine
//                new BTreeStateMachine(raftOptions.getDataDir());
//                new BitCaskStateMachine(raftOptions.getDataDir());

        // Initialize RaftNode
        RaftNode raftNode = new RaftNode(raftOptions, serverList, localServer, stateMachine);

        // Register the service for Raft nodes to call each other
        RaftConsensusService raftConsensusService = new RaftConsensusServiceImpl(raftNode);
        server.registerService(raftConsensusService);

        // Register the Raft service for client calls
        RaftClientService raftClientService = new RaftClientServiceImpl(raftNode);
        server.registerService(raftClientService);

        // Register the application's own service provided
        ExampleService exampleService = new ExampleServiceImpl(raftNode, stateMachine);
        server.registerService(exampleService);

        // Start RPCServer and initialize Raft node
        server.start();
        raftNode.init();
    }

    private static RaftProto.Server parseServer(String serverString) {
        String[] splitServer = serverString.split(":");
        String host = splitServer[0];
        Integer port = Integer.parseInt(splitServer[1]);
        Integer serverId = Integer.parseInt(splitServer[2]);
        RaftProto.Endpoint endPoint = RaftProto.Endpoint.newBuilder()
                .setHost(host).setPort(port).build();
        RaftProto.Server.Builder serverBuilder = RaftProto.Server.newBuilder();
        RaftProto.Server server = serverBuilder.setServerId(serverId).setEndpoint(endPoint).build();
        return server;
    }
}
