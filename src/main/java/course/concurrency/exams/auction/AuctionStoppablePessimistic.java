package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = Bid.LEAST_BID;
    private volatile boolean isRun = true;

    public boolean propose(Bid bid) {
        if (isRun && bid.getPrice() > latestBid.getPrice()) {
            Bid outdated = null;
            synchronized (this) {
                if (isRun && bid.getPrice() > latestBid.getPrice()) {
                    outdated = latestBid;
                    latestBid = bid;
                }
            }
            if (outdated != null && outdated != Bid.LEAST_BID) {
                notifier.sendOutdatedMessage(outdated);
            }
            return outdated != null;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public Bid stopAuction() {
        if (isRun) {
            // флаг позволяет вынести операцию остановки нотифаера из синхронайз модуля
            boolean hasChanged = false;
            synchronized (this) {
                if (isRun) {
                    isRun = false;
                    hasChanged = true;
                }
            }
            if (hasChanged) {
                notifier.shutdown();
            }
        }
        return latestBid;
    }
}
