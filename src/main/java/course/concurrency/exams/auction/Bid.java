package course.concurrency.exams.auction;

public class Bid {

    public static final Bid LEAST_BID = new Bid(-1L, -1L, Long.MIN_VALUE);

    private Long id;
    private Long participantId;
    private Long price;

    public Bid(Long id, Long participantId, Long price) {
        this.id = id;
        this.participantId = participantId;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public Long getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "Bid{" + id + ", " + participantId + ", " + price + '}';
    }
}
