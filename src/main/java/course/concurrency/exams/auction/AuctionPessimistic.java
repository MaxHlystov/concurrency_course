package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private final static Bid LEAST_BID = new Bid(-1L, -1L, Long.MIN_VALUE);
    private final Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = LEAST_BID;

    public boolean propose(Bid bid) {
        if (bid.getPrice() > latestBid.getPrice()) {
            boolean isSet = false;
            synchronized (this) {
                if (bid.getPrice() > latestBid.getPrice()) {
                    latestBid = bid;
                    isSet = true;
                }
            }
            if(isSet) {
                notifier.sendOutdatedMessage(latestBid);
            }
            return isSet;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
