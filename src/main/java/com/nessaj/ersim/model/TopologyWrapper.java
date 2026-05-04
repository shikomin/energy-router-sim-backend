package com.nessaj.ersim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TopologyWrapper {
    private List<Topology> topologies;

    public TopologyWrapper() {}

    public List<Topology> getTopologies() { return topologies; }
    public void setTopologies(List<Topology> topologies) { this.topologies = topologies; }
}