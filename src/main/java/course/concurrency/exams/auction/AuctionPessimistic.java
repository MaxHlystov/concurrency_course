package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = Bid.LEAST_BID;

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
