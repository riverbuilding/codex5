import java.math.BigDecimal;
import java.util.Objects;

public class Product {
    private final String productId;
    private final String name;
    private final BigDecimal price;
    private final int popularity;
    private int quantity;

    public Product(String productId, String name, BigDecimal price, int quantity) {
        this(productId, name, price, quantity, 0);
    }

    public Product(String productId, String name, BigDecimal price, int quantity, int popularity) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (price == null) {
            throw new IllegalArgumentException("price must not be null");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("price cannot be negative");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        if (popularity < 0) {
            throw new IllegalArgumentException("popularity cannot be negative");
        }

        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.popularity = popularity;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getPopularity() { return popularity; }

    void increaseQuantity(int delta) { this.quantity = Math.addExact(this.quantity, delta); }
    void decreaseQuantity(int delta) { this.quantity = Math.subExact(this.quantity, delta); }

    Product copy() {
        return new Product(productId, name, price, quantity, popularity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return popularity == product.popularity
                && Objects.equals(productId, product.productId)
                && Objects.equals(name, product.name)
                && Objects.equals(price, product.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, name, price, popularity);
    }
}
