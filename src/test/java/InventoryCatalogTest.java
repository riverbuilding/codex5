import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryCatalogTest {

    private static Product sampleProduct() {
        return new Product("P-100", "Widget", new BigDecimal("19.99"), 10);
    }

    @Test
    void addProductWithDuplicateIdShouldFail() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(sampleProduct());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalog.addProduct(new Product("P-100", "Duplicate", new BigDecimal("5.00"), 1)));

        assertEquals("Duplicate productId: P-100", ex.getMessage());
    }

    @Test
    void getProductWithInvalidIdShouldFailClearly() {
        InventoryCatalog catalog = new InventoryCatalog();

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> catalog.getProduct("INVALID"));

        assertEquals("Unknown productId: INVALID", ex.getMessage());
    }

    @Test
    void getProductShouldReturnCopy() {
        InventoryCatalog catalog = new InventoryCatalog();
        Product original = sampleProduct();
        catalog.addProduct(original);

        Product fromCatalog = catalog.getProduct("P-100");

        assertEquals(original, fromCatalog);
        assertNotSame(original, fromCatalog);
    }

    @Test
    void addProductWithNegativePriceShouldFail() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Product("P-101", "BadPrice", new BigDecimal("-1.00"), 1));

        assertEquals("price cannot be negative", ex.getMessage());
    }

    @Test
    void addStockWithNegativeQuantityShouldFail() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(sampleProduct());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalog.addStock("P-100", -5));

        assertEquals("addStock quantity must be positive", ex.getMessage());
    }

    @Test
    void addStockWithInvalidProductIdShouldFail() {
        InventoryCatalog catalog = new InventoryCatalog();

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> catalog.addStock("INVALID", 5));

        assertEquals("Unknown productId: INVALID", ex.getMessage());
    }

    @Test
    void catalogApiShouldRejectBlankProductId() {
        InventoryCatalog catalog = new InventoryCatalog();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalog.getProduct("  "));

        assertEquals("productId must not be blank", ex.getMessage());
    }

    @Test
    void removeStockWithNegativeQuantityShouldFail() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(sampleProduct());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalog.removeStock("P-100", -1));

        assertEquals("removeStock quantity must be positive", ex.getMessage());
    }

    @Test
    void removeStockLargerThanAvailableShouldFail() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(sampleProduct());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalog.removeStock("P-100", 99));

        assertEquals("Cannot remove more stock than available", ex.getMessage());
    }

    @Test
    void removeStockWithInvalidProductIdShouldFail() {
        InventoryCatalog catalog = new InventoryCatalog();

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> catalog.removeStock("INVALID", 1));

        assertEquals("Unknown productId: INVALID", ex.getMessage());
    }
}
