package com.github.raftimpl.raft.example.server.service.impl;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.instance.Endpoint;
import com.github.raftimpl.raft.Peer;
import com.github.raftimpl.raft.RaftNode;
import com.github.raftimpl.raft.StateMachine;
import com.github.raftimpl.raft.example.server.service.ExampleProto;
import com.github.raftimpl.raft.example.server.service.ExampleService;
import com.github.raftimpl.raft.proto.RaftProto;
import com.googlecode.protobuf.format.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ExampleServiceImpl implements ExampleService {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleServiceImpl.class);
    private static JsonFormat jsonFormat = new JsonFormat();

    private RaftNode raftNode;
    private StateMachine stateMachine;
    private int leaderId = -1;
    private RpcClient leaderRpcClient = null;
    private ExampleService leaderService = null;
    private Lock leaderLock = new ReentrantLock();

    public ExampleServiceImpl(RaftNode raftNode, StateMachine stateMachine) {
        this.raftNode = raftNode;
        this.stateMachine = stateMachine;
    }

    private void onLeaderChangeEvent() {
        if (raftNode.getLeaderId() != -1
                && raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()
                && leaderId != raftNode.getLeaderId()) {
            leaderLock.lock();
            if (leaderId != -1 && leaderRpcClient != null) {
                leaderRpcClient.stop();
                leaderRpcClient = null;
                leaderId = -1;
            }
            leaderId = raftNode.getLeaderId();
            Peer peer = raftNode.getPeerMap().get(leaderId);
            Endpoint endpoint = new Endpoint(peer.getServer().getEndpoint().getHost(),
                    peer.getServer().getEndpoint().getPort());
            RpcClientOptions rpcClientOptions = new RpcClientOptions();
            rpcClientOptions.setGlobalThreadPoolSharing(true);
            leaderRpcClient = new RpcClient(endpoint, rpcClientOptions);
            leaderService = BrpcProxy.getProxy(leaderRpcClient, ExampleService.class);
            leaderLock.unlock();
        }
    }

    @Override
    public ExampleProto.SetResponse set(ExampleProto.SetRequest request) {
        ExampleProto.SetResponse.Builder responseBuilder = ExampleProto.SetResponse.newBuilder();
        // If not the leader, forward write requests to the leader
        if (raftNode.getLeaderId() <= 0) {
            responseBuilder.setSuccess(false);
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            onLeaderChangeEvent();
            ExampleProto.SetResponse responseFromLeader = leaderService.set(request);
            responseBuilder.mergeFrom(responseFromLeader);
        } else {
            // Data synchronously written to the Raft cluster
            byte[] data = request.toByteArray();
            boolean success = raftNode.replicate(data, RaftProto.EntryType.ENTRY_TYPE_DATA);

            responseBuilder.setSuccess(success);
        }

        ExampleProto.SetResponse response = responseBuilder.build();
        LOG.info("set request, request={}, response={}", jsonFormat.printToString(request),
                jsonFormat.printToString(response));
        return response;
    }

    /*@Override
    public ExampleProto.GetResponse get(ExampleProto.GetRequest request) {
        // Follower-read Eventual consistency
        ExampleProto.GetResponse.Builder responseBuilder = ExampleProto.GetResponse.newBuilder();
        byte[] keyBytes = request.getKey().getBytes();
        byte[] valueBytes = stateMachine.get(keyBytes);
        if (valueBytes != null) {
            String value = new String(valueBytes);
            responseBuilder.setValue(value);
        }
        ExampleProto.GetResponse response = responseBuilder.build();
        LOG.info("get request, request={}, response={}", jsonFormat.printToString(request),
                jsonFormat.printToString(response));
        return response;
    }*/

    @Override
    public ExampleProto.GetResponse get(ExampleProto.GetRequest request) {
        // Follower-read, non-strong consistency
        ExampleProto.GetResponse.Builder responseBuilder = ExampleProto.GetResponse.newBuilder();
        byte[] keyBytes = request.getKey().getBytes();
        // Obtain the Read Index from the Leader node and wait for the log entries before the Read Index to be applied to the replicated state machine
        if (raftNode.waitForLeaderCommitIndex()) {
            byte[] valueBytes = stateMachine.get(keyBytes);
            if (valueBytes != null) {
                String value = new String(valueBytes);
                responseBuilder.setValue(value);
            }
        } else {
            LOG.warn("read failed, meet error");
        }
        ExampleProto.GetResponse response = responseBuilder.build();
        LOG.info("get request, request={}, response={}", jsonFormat.printToString(request),
                jsonFormat.printToString(response));
        return response;
    }

    /*@Override
public ExampleProto.GetResponse get(ExampleProto.GetRequest request) {
    // Leader-read for strong consistency (using Read Index)
    ExampleProto.GetResponse.Builder responseBuilder = ExampleProto.GetResponse.newBuilder();
    if (raftNode.getLeaderId() > 0) {
        // If this node is not the leader, forward the read request to the leader
        if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            onLeaderChangeEvent();
            // Perform a get operation on the leader's service and merge the result
            ExampleProto.GetResponse responseFromLeader = leaderService.get(request);
            responseBuilder.mergeFrom(responseFromLeader);
        } else {
            byte[] keyBytes = request.getKey().getBytes();
            // Confirm this node is still the leader and wait for the log entries before the Read Index to be applied to the replicated state machine
            if (raftNode.waitUntilApplied()) {
                // Read from the state machine
                byte[] valueBytes = stateMachine.get(keyBytes);
                if (valueBytes != null) {
                    String value = new String(valueBytes);
                    responseBuilder.setValue(value);
                }
            } else {
                LOG.warn("read failed, meet error");
            }
        }
    }
    return responseBuilder.build();
}


        ExampleProto.GetResponse response = responseBuilder.build();
        LOG.info("get request, request={}, response={}", jsonFormat.printToString(request),
                jsonFormat.printToString(response));
        return response;
    }*/
}
