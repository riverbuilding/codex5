import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public class InventoryCatalog {
    private final Map<String, Product> products = new HashMap<>();
    private final PriceBook priceBook = new PriceBook();
    private final Map<String, Integer> unitsSoldByProductId = new HashMap<>();

    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }

        String productId = product.getProductId();
        validateProductId(productId);

        if (products.containsKey(productId)) {
            throw new IllegalArgumentException("Duplicate productId: " + productId);
        }
        products.put(productId, product.copy());
    }

    public Product getProduct(String productId) {
        validateProductId(productId);
        return getInternalProduct(productId).copy();
    }

    public void setPrice(String productId, Currency currency, BigDecimal price) {
        validateKnownProduct(productId);
        priceBook.setPrice(productId, currency, price);
    }

    public Optional<BigDecimal> getPrice(String productId, Currency currency) {
        Product product = validateKnownProduct(productId);
        return priceBook.getPrice(product.getProductId(), currency);
    }

    public List<Product> listProductsSortedByPrice(Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("currency must not be null");
        }

        Comparator<Product> byRules = Comparator
                .comparing((Product p) -> getPrice(p.getProductId(), currency).orElseThrow())
                .thenComparing(Comparator.comparingInt(Product::getPopularity).reversed())
                .thenComparing(Product::getProductId);

        return products.values().stream()
                .filter(p -> getPrice(p.getProductId(), currency).isPresent())
                .map(Product::copy)
                .sorted(byRules)
                .toList();
    }

    public void addStock(String productId, int quantity) {
        validateProductId(productId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("addStock quantity must be positive");
        }
        getInternalProduct(productId).increaseQuantity(quantity);
    }

    public void removeStock(String productId, int quantity) {
        validateProductId(productId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("removeStock quantity must be positive");
        }
        Product product = getInternalProduct(productId);
        if (quantity > product.getQuantity()) {
            throw new IllegalArgumentException("Cannot remove more stock than available");
        }
        product.decreaseQuantity(quantity);
    }

    public void recordSale(String productId, int quantity) {
        validateKnownProduct(productId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("recordSale quantity must be positive");
        }

        removeStock(productId, quantity);
        unitsSoldByProductId.merge(productId, quantity, Integer::sum);
    }

    public List<ProductSales> topBestSellingProducts(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }

        return unitsSoldByProductId.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(
                        Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                                .thenComparing(Map.Entry.comparingByKey())
                )
                .limit(n)
                .map(e -> new ProductSales(e.getKey(), e.getValue()))
                .toList();
    }

    public List<Product> lowStockProducts(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold cannot be negative");
        }

        return products.values().stream()
                .filter(p -> p.getQuantity() <= threshold)
                .sorted(
                        Comparator.comparingInt(Product::getQuantity)
                                .thenComparing(Product::getProductId)
                )
                .map(Product::copy)
                .toList();
    }

    private Product validateKnownProduct(String productId) {
        validateProductId(productId);
        return getInternalProduct(productId);
    }

    private Product getInternalProduct(String productId) {
        Product product = products.get(productId);
        if (product == null) {
            throw new NoSuchElementException("Unknown productId: " + productId);
        }
        return product;
    }

    private static void validateProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
    }
}
