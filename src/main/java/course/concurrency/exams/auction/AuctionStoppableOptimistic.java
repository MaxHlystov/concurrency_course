package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    // Флаг нужен, чтобы предотвратить изменение ссылки, после остановки аукциона.
    private final AtomicMarkableReference<Bid> latestBidWithRunningFlag =
            new AtomicMarkableReference<>(Bid.LEAST_BID, true);

    public boolean propose(Bid bid) {
        Bid latestTmp;
        do {
            latestTmp = latestBidWithRunningFlag.getReference();
            if (!latestBidWithRunningFlag.isMarked() || bid.getPrice() <= latestTmp.getPrice()) {
                return false;
            }
        } while (!latestBidWithRunningFlag.compareAndSet(latestTmp, bid, true, true));
        if (latestTmp != Bid.LEAST_BID) {
            notifier.sendOutdatedMessage(latestTmp);
        }
        return true;
    }

    public Bid getLatestBid() {
        return latestBidWithRunningFlag.getReference();
    }

    public Bid stopAuction() {
        Bid winner;
        do {
            if(!latestBidWithRunningFlag.isMarked()) {
                return latestBidWithRunningFlag.getReference();
            }
            winner = latestBidWithRunningFlag.getReference();
        } while (!latestBidWithRunningFlag.attemptMark(winner, false));
        notifier.shutdown();
        return winner;
    }
}
