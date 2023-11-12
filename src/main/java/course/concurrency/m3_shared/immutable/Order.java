package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.stream.Collectors;

import static course.concurrency.m3_shared.immutable.Order.Status.DELIVERED;
import static course.concurrency.m3_shared.immutable.Order.Status.NEW;

public final class Order {

    public enum Status {NEW, IN_PROGRESS, DELIVERED}

    private final long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    public Order(long id, List<Item> items) {
        this.id = id;
        this.items = items.stream().collect(Collectors.toUnmodifiableList());
        this.paymentInfo = null;
        this.isPacked = false;
        this.status = NEW;
    }

    private Order(Order order, PaymentInfo paymentInfo) {
        this.id = order.id;
        this.items = order.items;
        this.paymentInfo = paymentInfo;
        this.isPacked = order.isPacked;
        this.status = Status.IN_PROGRESS;
    }

    private Order(Order order, boolean isPacked) {
        this.id = order.id;
        this.items = order.items;
        this.paymentInfo = order.paymentInfo;
        this.isPacked = isPacked;
        this.status = Status.IN_PROGRESS;
    }

    private Order(Order order, Status status) {
        this.id = order.id;
        this.items = order.items;
        this.paymentInfo = order.paymentInfo;
        this.isPacked = order.isPacked;
        this.status = status;
    }

    public boolean checkStatus() {
        return items != null && !items.isEmpty() && paymentInfo != null && isPacked;
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items;
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public Order withPaymentInfo(PaymentInfo paymentInfo) {
        if (this.paymentInfo != null) {
            throw new IllegalStateException("Can't reset paymentInfo");
        }
        return new Order(this, paymentInfo);
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Order withPacked() {
        if (this.isPacked) {
            return this;
        }
        return new Order(this, true);
    }

    public Status getStatus() {
        return status;
    }

    public Order withDeliveredStatus() {
        if (DELIVERED.equals(this.status)) {
            return this;
        }
        return new Order(this, DELIVERED);
    }
}
