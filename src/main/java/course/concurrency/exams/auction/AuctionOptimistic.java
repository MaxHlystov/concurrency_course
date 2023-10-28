package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private AtomicReference<Bid> latestBid = new AtomicReference<>(null);

    public boolean propose(Bid bid) {
        for (var latestTmp = latestBid.get(); latestTmp == null || bid.getPrice() > latestTmp.getPrice(); latestTmp = latestBid.get()) {
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
}
