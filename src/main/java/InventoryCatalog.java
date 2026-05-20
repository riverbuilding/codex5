import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class InventoryCatalog {
    private final Map<String, Product> products = new HashMap<>();

    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }

        String productId = product.getProductId();
        if (products.containsKey(productId)) {
            throw new IllegalArgumentException("Duplicate productId: " + productId);
        }
        products.put(productId, product);
    }

    public Product getProduct(String productId) {
        Product product = products.get(productId);
        if (product == null) {
            throw new NoSuchElementException("Unknown productId: " + productId);
        }
        return product;
    }

    public void addStock(String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("addStock quantity must be positive");
        }
        Product product = getProduct(productId);
        product.increaseQuantity(quantity);
    }

    public void removeStock(String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("removeStock quantity must be positive");
        }
        Product product = getProduct(productId);
        if (quantity > product.getQuantity()) {
            throw new IllegalArgumentException("Cannot remove more stock than available");
        }
        product.decreaseQuantity(quantity);
    }
}
