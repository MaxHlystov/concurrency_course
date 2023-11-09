package course.concurrency.m3_shared.collections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class RestaurantService {

    private static final int EXPECTED_NUMBER_OF_RESTAURANTS = 100;
    private final Map<String, Restaurant> restaurantMap = new ConcurrentHashMap<>(
            Map.of(
                    "A", new Restaurant("A"),
                    "B", new Restaurant("B"),
                    "C", new Restaurant("C")
            ));

    private final Map<String, LongAdder> stat = new ConcurrentHashMap<>(EXPECTED_NUMBER_OF_RESTAURANTS);

    public Restaurant getByName(String restaurantName) {
        addToStat(restaurantName);
        return restaurantMap.get(restaurantName);
    }

    public void addToStat(String restaurantName) {
        var restaurantCounter = stat.computeIfAbsent(restaurantName, name -> new LongAdder());
        restaurantCounter.increment();
    }

    public Set<String> printStat() {
        return stat.entrySet().stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue().sum())
                .collect(Collectors.toSet());
    }
}
