import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PriceBook {
    private final Map<String, Map<Currency, BigDecimal>> pricesByProduct = new HashMap<>();

    public void setPrice(String productId, Currency currency, BigDecimal price) {
        validateProductId(productId);
        if (currency == null) {
            throw new IllegalArgumentException("currency must not be null");
        }
        if (price == null) {
            throw new IllegalArgumentException("price must not be null");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("price cannot be negative");
        }

        pricesByProduct.computeIfAbsent(productId, ignored -> new HashMap<>()).put(currency, price);
    }

    public Optional<BigDecimal> getPrice(String productId, Currency currency) {
        validateProductId(productId);
        if (currency == null) {
            throw new IllegalArgumentException("currency must not be null");
        }

        return Optional.ofNullable(pricesByProduct.getOrDefault(productId, Map.of()).get(currency));
    }

    private static void validateProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
    }
}
