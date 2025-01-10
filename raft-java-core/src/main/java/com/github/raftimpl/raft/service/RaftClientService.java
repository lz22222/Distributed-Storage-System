package com.github.raftimpl.raft.service;

import com.github.raftimpl.raft.proto.RaftProto;

/**
 * Interface for managing a Raft cluster.
 */
public interface RaftClientService {

    /**
     * Retrieves the leader node information of the Raft cluster.
     * @param request The request to get the leader information.
     * @return The leader node information.
     */
    RaftProto.GetLeaderResponse getLeader(RaftProto.GetLeaderRequest request);

    /**
     * Retrieves the configuration of the Raft cluster including information about all nodes.
     * @param request The request to get the cluster configuration.
     * @return The configuration of the cluster, including details of all nodes and their roles (leader/follower).
     */
    RaftProto.GetConfigurationResponse getConfiguration(RaftProto.GetConfigurationRequest request);

    /**
     * Adds nodes to the Raft cluster.
     * @param request The information about the nodes to be added.
     * @return The response indicating success or failure of the operation.
     */
    RaftProto.AddPeersResponse addPeers(RaftProto.AddPeersRequest request);

    /**
     * Removes nodes from the Raft cluster.
     * @param request The request to remove nodes.
     * @return The response indicating success or failure of the operation.
     */
    RaftProto.RemovePeersResponse removePeers(RaftProto.RemovePeersRequest request);
}
