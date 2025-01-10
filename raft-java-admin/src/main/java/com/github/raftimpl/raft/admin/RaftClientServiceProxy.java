package com.github.raftimpl.raft.admin;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.instance.Endpoint;
import com.github.raftimpl.raft.proto.RaftProto;
import com.github.raftimpl.raft.service.RaftClientService;
import com.googlecode.protobuf.format.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class not thread-safe
 * This class acts as a proxy for interacting with Raft cluster nodes.
 */
public class RaftClientServiceProxy implements RaftClientService {
    // Logger for debugging and log output
    private static final Logger LOG = LoggerFactory.getLogger(RaftClientServiceProxy.class);
    // Formatter for outputting protobuf messages in JSON format
    private static final JsonFormat jsonFormat = new JsonFormat();

    // List of servers in the cluster
    private List<RaftProto.Server> cluster;
    // RPC client for the entire cluster
    private RpcClient clusterRPCClient;
    // Raft client service for the entire cluster
    private RaftClientService clusterRaftClientService;

    // Server that is currently the leader
    private RaftProto.Server leader;
    // RPC client for the leader
    private RpcClient leaderRPCClient;
    // Raft client service for the leader
    private RaftClientService leaderRaftClientService;

    // RPC client options
    private RpcClientOptions rpcClientOptions = new RpcClientOptions();

    /**
     * Constructor initializing RPC clients with the server addresses provided in ipPorts.
     * @param ipPorts Comma-separated list of IP addresses and ports of the servers.
     */
    public RaftClientServiceProxy(String ipPorts) {
        rpcClientOptions.setConnectTimeoutMillis(1000); // Set connection timeout to 1 second
        rpcClientOptions.setReadTimeoutMillis(3600000); // Set read timeout to 1 hour
        rpcClientOptions.setWriteTimeoutMillis(1000); // Set write timeout to 1 second
        // Initialize RPC client for the cluster using the provided IP addresses and ports
        clusterRPCClient = new RpcClient(ipPorts, rpcClientOptions);
        // Get a proxy to the Raft client service
        clusterRaftClientService = BrpcProxy.getProxy(clusterRPCClient, RaftClientService.class);
        // Update the cluster configuration (detect leader, etc.)
        updateConfiguration();
    }

    /**
     * Retrieves the current leader of the Raft cluster.
     */
    @Override
    public RaftProto.GetLeaderResponse getLeader(RaftProto.GetLeaderRequest request) {
        return clusterRaftClientService.getLeader(request);
    }

    /**
     * Retrieves the current configuration of the Raft cluster.
     */
    @Override
    public RaftProto.GetConfigurationResponse getConfiguration(RaftProto.GetConfigurationRequest request) {
        return clusterRaftClientService.getConfiguration(request);
    }

    /**
     * Attempts to add peers to the Raft cluster. If not the leader, updates the leader and retries.
     */
    @Override
    public RaftProto.AddPeersResponse addPeers(RaftProto.AddPeersRequest request) {
        RaftProto.AddPeersResponse response = leaderRaftClientService.addPeers(request);
        // If the current node is not the leader, find the new leader and retry
        if (response != null && response.getResCode() == RaftProto.ResCode.RES_CODE_NOT_LEADER) {
            updateConfiguration();
            response = leaderRaftClientService.addPeers(request);
        }
        return response;
    }

    /**
     * Attempts to remove peers from the Raft cluster. If not the leader, updates the leader and retries.
     */
    @Override
    public RaftProto.RemovePeersResponse removePeers(RaftProto.RemovePeersRequest request) {
        RaftProto.RemovePeersResponse response = leaderRaftClientService.removePeers(request);
        // If the current node is not the leader, find the new leader and retry
        if (response != null && response.getResCode() == RaftProto.ResCode.RES_CODE_NOT_LEADER) {
            updateConfiguration();
            response = leaderRaftClientService.removePeers(request);
        }
        return response;
    }

    /**
     * Stops the RPC clients.
     */
    public void stop() {
        if (leaderRPCClient != null) {
            leaderRPCClient.stop();
        }
        if (clusterRPCClient != null) {
            clusterRPCClient.stop();
        }
    }

    /**
     * Updates the configuration of the cluster, identifying the current leader.
     */
    private boolean updateConfiguration() {
        RaftProto.GetConfigurationRequest request = RaftProto.GetConfigurationRequest.newBuilder().build();
        RaftProto.GetConfigurationResponse response = clusterRaftClientService.getConfiguration(request);
        if (response != null && response.getResCode() == RaftProto.ResCode.RES_CODE_SUCCESS) {
            if (leaderRPCClient != null) {
                leaderRPCClient.stop();
            }
            leader = response.getLeader();
            leaderRPCClient = new RpcClient(convertEndPoint(leader.getEndpoint()), rpcClientOptions);
            leaderRaftClientService = BrpcProxy.getProxy(leaderRPCClient, RaftClientService.class);
            return true;
        }
        return false;
    }

    /**
     * Converts a Raft endpoint to an RPC endpoint.
     */
    private Endpoint convertEndPoint(RaftProto.Endpoint endPoint) {
        return new Endpoint(endPoint.getHost(), endPoint.getPort());
    }
}
