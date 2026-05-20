import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryCatalogTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    private static Product sampleProduct() {
        return new Product("P-100", "Widget", new BigDecimal("19.99"), 10, 5);
    }

    @Test
    void productEqualityUsesProductIdOnly() {
        Product p1 = new Product("P-1", "A", new BigDecimal("1.00"), 1, 1);
        Product p2 = new Product("P-1", "B", new BigDecimal("2.00"), 9, 9);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void getPriceThrowsForUnknownProduct() {
        InventoryCatalog catalog = new InventoryCatalog();
        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> catalog.getPrice("UNKNOWN", USD));
        assertEquals("Unknown productId: UNKNOWN", ex.getMessage());
    }

    @Test
    void getPriceReturnsEmptyWhenCurrencyPriceMissingForKnownProduct() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(sampleProduct());

        assertEquals(java.util.Optional.empty(), catalog.getPrice("P-100", EUR));
    }

    @Test
    void getPriceReturnsValueForKnownProductAndCurrency() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(sampleProduct());
        catalog.setPrice("P-100", USD, new BigDecimal("11.50"));

        assertEquals(java.util.Optional.of(new BigDecimal("11.50")), catalog.getPrice("P-100", USD));
    }

    @Test
    void listProductsSortedByPriceAscending() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-1", "One", new BigDecimal("1.00"), 1, 1));
        catalog.addProduct(new Product("P-2", "Two", new BigDecimal("1.00"), 1, 1));
        catalog.addProduct(new Product("P-3", "Three", new BigDecimal("1.00"), 1, 1));

        catalog.setPrice("P-1", USD, new BigDecimal("12.00"));
        catalog.setPrice("P-2", USD, new BigDecimal("10.00"));
        catalog.setPrice("P-3", USD, new BigDecimal("11.00"));

        List<Product> sorted = catalog.listProductsSortedByPrice(USD);
        assertEquals(List.of("P-2", "P-3", "P-1"), sorted.stream().map(Product::getProductId).toList());
    }

    @Test
    void listProductsWhenSamePriceSortByPopularityDesc() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-1", "One", new BigDecimal("1.00"), 1, 10));
        catalog.addProduct(new Product("P-2", "Two", new BigDecimal("1.00"), 1, 50));
        catalog.addProduct(new Product("P-3", "Three", new BigDecimal("1.00"), 1, 30));

        catalog.setPrice("P-1", USD, new BigDecimal("10.00"));
        catalog.setPrice("P-2", USD, new BigDecimal("10.00"));
        catalog.setPrice("P-3", USD, new BigDecimal("10.00"));

        List<Product> sorted = catalog.listProductsSortedByPrice(USD);
        assertEquals(List.of("P-2", "P-3", "P-1"), sorted.stream().map(Product::getProductId).toList());
    }

    @Test
    void listProductsWhenSamePriceAndPopularitySortByProductIdAsc() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-2", "Two", new BigDecimal("1.00"), 1, 20));
        catalog.addProduct(new Product("P-1", "One", new BigDecimal("1.00"), 1, 20));

        catalog.setPrice("P-1", USD, new BigDecimal("10.00"));
        catalog.setPrice("P-2", USD, new BigDecimal("10.00"));

        List<Product> sorted = catalog.listProductsSortedByPrice(USD);
        assertEquals(List.of("P-1", "P-2"), sorted.stream().map(Product::getProductId).toList());
    }

    @Test
    void listProductsSortedByPriceExcludesProductsWithoutRequestedCurrency() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-1", "One", new BigDecimal("1.00"), 1, 1));
        catalog.addProduct(new Product("P-2", "Two", new BigDecimal("1.00"), 1, 1));

        catalog.setPrice("P-1", USD, new BigDecimal("10.00"));

        List<Product> sorted = catalog.listProductsSortedByPrice(USD);
        assertEquals(List.of("P-1"), sorted.stream().map(Product::getProductId).toList());
    }

    @Test
    void recordSaleShouldDecreaseStockAndTrackUnitsSold() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-1", "One", new BigDecimal("1.00"), 10, 1));

        catalog.recordSale("P-1", 3);
        catalog.recordSale("P-1", 2);

        assertEquals(5, catalog.getProduct("P-1").getQuantity());
        assertEquals(List.of(new ProductSales("P-1", 5)), catalog.topBestSellingProducts(10));
    }

    @Test
    void recordSaleValidationsShouldApply() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-1", "One", new BigDecimal("1.00"), 2, 1));

        IllegalArgumentException qEx = assertThrows(IllegalArgumentException.class,
                () -> catalog.recordSale("P-1", 0));
        assertEquals("recordSale quantity must be positive", qEx.getMessage());

        IllegalArgumentException stockEx = assertThrows(IllegalArgumentException.class,
                () -> catalog.recordSale("P-1", 5));
        assertEquals("Cannot remove more stock than available", stockEx.getMessage());

        NoSuchElementException missingEx = assertThrows(NoSuchElementException.class,
                () -> catalog.recordSale("UNKNOWN", 1));
        assertEquals("Unknown productId: UNKNOWN", missingEx.getMessage());
    }

    @Test
    void topBestSellingProductsShouldSortAndValidateN() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-2", "Two", new BigDecimal("1.00"), 50, 1));
        catalog.addProduct(new Product("P-1", "One", new BigDecimal("1.00"), 50, 1));
        catalog.addProduct(new Product("P-3", "Three", new BigDecimal("1.00"), 50, 1));

        catalog.recordSale("P-1", 7);
        catalog.recordSale("P-2", 9);
        catalog.recordSale("P-3", 9);

        assertEquals(
                List.of(new ProductSales("P-2", 9), new ProductSales("P-3", 9)),
                catalog.topBestSellingProducts(2)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalog.topBestSellingProducts(0));
        assertEquals("n must be positive", ex.getMessage());
    }

    @Test
    void lowStockProductsShouldFilterSortAndValidateThreshold() {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-2", "Two", new BigDecimal("1.00"), 1, 1));
        catalog.addProduct(new Product("P-1", "One", new BigDecimal("1.00"), 1, 1));
        catalog.addProduct(new Product("P-3", "Three", new BigDecimal("1.00"), 3, 1));

        List<Product> lowStock = catalog.lowStockProducts(2);
        assertEquals(List.of("P-1", "P-2"), lowStock.stream().map(Product::getProductId).toList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalog.lowStockProducts(-1));
        assertEquals("threshold cannot be negative", ex.getMessage());
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
}
