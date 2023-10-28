package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private Bid latestBid;
    private boolean isRun = true;

    public synchronized boolean propose(Bid bid) {
        if (isRun &&
                (latestBid == null || bid.getPrice() > latestBid.getPrice())
        ) {
            notifier.sendOutdatedMessage(latestBid);
            latestBid = bid;
            return true;
        }
        return false;
    }

    public synchronized Bid getLatestBid() {
        return latestBid;
    }

    public synchronized Bid stopAuction() {
        isRun = false;
        notifier.shutdown();
        return latestBid;
    }
}
