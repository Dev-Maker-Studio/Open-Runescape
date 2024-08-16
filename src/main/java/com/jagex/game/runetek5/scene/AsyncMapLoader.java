package com.jagex.game.runetek5.scene;

import java.util.LinkedList;

import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.loc.LocTypeList;

/**
 * (identical to 910)
 * @author Brainpower - 11/03/2023
 *
 */
public class AsyncMapLoader implements Runnable {

    public MapRegion region = new MapRegion(true);

    public volatile boolean loadingRegion;

    public final LinkedList<RegionLoadRequest> regionLoadRequests = new LinkedList<RegionLoadRequest>();

    public void requestMapUpdate(RegionLoadRequest regionLoadRequest) {
        synchronized (this.regionLoadRequests) {
        	this.regionLoadRequests.add(regionLoadRequest);
        	this.regionLoadRequests.notify();
        }
    }

    void setMapRegion(MapRegion mapRegion) {
        this.region = mapRegion;
    }

    public boolean isLoadingRegion() {
        return this.loadingRegion;
    }

    public MapRegion getMapRegion() {
        return this.region;
    }

    public void decodeAll() {
        RegionLoadRequest request;
        synchronized (this.regionLoadRequests) {
            try {
                this.regionLoadRequests.wait();
            } catch (InterruptedException ie) {
            	//NOOP
            }
            request = this.regionLoadRequests.pollFirst();
        }
        try {
            try {
                if (request != null) {
                    this.loadingRegion = true;
                    decode(request);
                }
            } catch (Exception exception) {
                this.loadingRegion = false;
                return;
            }
            this.loadingRegion = false;
        } catch (Exception exception) {
            this.loadingRegion = false;
        }
    }

    public void decode(RegionLoadRequest regionLoadRequest) {
        if (regionLoadRequest.loadingType == RegionLoadingType.RELOAD) {
            this.region.reload();
        } else {
            this.region.initialize(regionLoadRequest);
        }
        for (boolean regionLoaded = this.region.loadRegion(); !regionLoaded; regionLoaded = this.region.loadRegion()) {}
        
        this.region.setLoadingRegion();
        Resource.mapRegion.setLoadingRegion();
    }

    public void initializeRegion(LocTypeList loctypelist) {
        this.region.setLoctypelist(loctypelist);
    }

    @Override
    public void run() {
        while (true) {
            decodeAll();
        }
    }
}