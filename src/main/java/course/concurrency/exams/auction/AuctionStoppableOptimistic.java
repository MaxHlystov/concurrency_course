package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private AtomicReference<Bid> latestBid = new AtomicReference<>(null);
    private AtomicBoolean isRun = new AtomicBoolean(true);

    public boolean propose(Bid bid) {
        for (var latestTmp = latestBid.get();
             isRun.get() && (latestTmp == null || bid.getPrice() > latestTmp.getPrice());
             latestTmp = latestBid.get()
        ) {
            if (latestBid.compareAndSet(latestTmp, bid)) {
                notifier.sendOutdatedMessage(latestTmp);
                return true;
            }
        }

        return false;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }

    public Bid stopAuction() {
        if (isRun.compareAndSet(true, false)) {
            notifier.shutdown();
        }
        return latestBid.get();
    }
}
