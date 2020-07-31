package main;

import link.DataLink;

public interface DataLinkAggregator {
    void addDataLink(DataLink dl);
    int countLinks();
}
