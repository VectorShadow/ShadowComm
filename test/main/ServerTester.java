package main;

import link.DataLink;
import link.ServerTestDataHandler;

import java.io.IOException;
import java.util.ArrayList;

public class ServerTester {

    public static void main(String[] args) throws IOException {
        new Server(
                new ServerTestDataHandler(),
                new DataLinkAggregator() {
                    private ArrayList<DataLink> dataLinks = new ArrayList<>();
                    @Override
                    public void addDataLink(DataLink dl) {
                        dataLinks.add(dl);
                    }

                    @Override
                    public int countLinks() {
                        return dataLinks.size();
                    }
                },
                29387).start();
    }
}
