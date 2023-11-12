package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public class OrderService {

    // AtomicReference позволяет нам блочить currentOrders только в момент добавления заказа и только
    // на время добавления ссылки. Все остальные операции, которые в реальности могут занимать
    // некоторое время, мы выполняем в рамках блокировки отдельной ссылки на заказ.
    private final Map<Long, AtomicReference<Order>> currentOrders = new ConcurrentHashMap<>();
    private final Map<List<Item>, Order> orderByItemsList = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0L);

    public long createOrder(List<Item> items) {
        // Чтобы обеспечить идемпотентность этого метода проще всего добавить айди запроса.
        // Но т.к. я поменять сигнатуру этой функции не могу, то добавил проверку существования списка.
        // На всякий случай, здесь, я борюсь с тем, чтобы два вызова createOrder с одним и тем же списком не привел
        // к дублированию заказа.
        final var order = orderByItemsList.computeIfAbsent(items, key ->
                new Order(nextId(), items));
        currentOrders.putIfAbsent(order.getId(), new AtomicReference<>(order));
        return order.getId();
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        final var orderRef = currentOrders.get(orderId);
        if (updateRef(orderRef, order -> order.withPaymentInfo(paymentInfo))) {
            deliverIfCompleted(orderId);
        }
    }

    public void setPacked(long orderId) {
        final var orderRef = currentOrders.get(orderId);
        if (updateRef(orderRef, Order::withPacked)) {
            deliverIfCompleted(orderId);
        }
    }

    public boolean isDelivered(long orderId) {
        return isDelivered(currentOrders.get(orderId).get());
    }

    private static boolean updateRef(AtomicReference<Order> orderRef, UnaryOperator<Order> transition) {
        Order order = null;
        Order newOrder = null;
        do {
            order = orderRef.get();
            newOrder = transition.apply(order);
            if (order == newOrder) {
                return false;
            }
        } while (!orderRef.compareAndSet(order, newOrder));
        return true;
    }

    private boolean isDelivered(Order order) {
        return order != null && Order.Status.DELIVERED.equals(order.getStatus());
    }

    private void deliverIfCompleted(long orderId) {
        var order = currentOrders.get(orderId).get();
        if (order != null && order.checkStatus() && !isDelivered(order)) {
            final var orderRef = currentOrders.get(order.getId());
            if (updateRef(orderRef, Order::withDeliveredStatus)) {
                order = orderRef.get();
                // send message and so on
                System.out.println("Process of delivering the order with id " + order);
            }
        }
    }

    private long nextId() {
        return nextId.getAndIncrement();
    }
}
