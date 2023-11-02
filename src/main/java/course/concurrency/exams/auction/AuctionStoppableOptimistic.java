package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    // Флаг нужен, чтобы предотвратить изменение ссылки, после остановки аукциона.
    private final AtomicMarkableReference<Bid> latestBidWithRunningFlag =
            new AtomicMarkableReference<>(Bid.LEAST_BID, true);

    // Флаг нужен, чтобы из нескольких потоков, которые попытаются остановить аукцион, победил один
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public boolean propose(Bid bid) {
        Bid latestTmp;
        do {
            latestTmp = latestBidWithRunningFlag.getReference();
            if (bid.getPrice() <= latestTmp.getPrice()) {
                return false;
            }
        } while (isRunning.get() &&
                // сюда может просочиться какая-то часть потоков. но новые после, isRunnint == false, нет
                !latestBidWithRunningFlag.compareAndSet(latestTmp, bid, true, true)
        );
        if (isRunning.get()) {
            if (latestTmp != Bid.LEAST_BID) {
                notifier.sendOutdatedMessage(latestTmp);
            }
            return true;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBidWithRunningFlag.getReference();
    }

    // Мы считаем аукцион остановленным, когда оба флага == false
    public Bid stopAuction() {
        if (isRunning.compareAndSet(true, false)) {
            Bid winner;
            do {
                winner = latestBidWithRunningFlag.getReference();

            } while (!latestBidWithRunningFlag.attemptMark(winner, false));
            notifier.shutdown();
            return winner;
        }
        // Ждем, когда победивший поток, зафиксирует победителя
        while (latestBidWithRunningFlag.isMarked()) ;
        return latestBidWithRunningFlag.getReference();
    }
}
