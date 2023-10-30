package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private AtomicReference<Bid> latestBid = new AtomicReference<>(null);

    public boolean propose(Bid bid) {
        Bid latestTmp = latestBid.get();
        if (latestTmp == null && latestBid.compareAndSet(latestTmp, bid)) {
            return true;
        }
        do {
            latestTmp = latestBid.get();
            if (bid.getPrice() <= latestTmp.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(latestTmp, bid));
        notifier.sendOutdatedMessage(latestTmp);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
