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
    void getProductShouldReturnCopy() {
        InventoryCatalog catalog = new InventoryCatalog();
        Product original = sampleProduct();
        catalog.addProduct(original);

        Product fromCatalog = catalog.getProduct("P-100");

        assertEquals(original, fromCatalog);
        assertNotSame(original, fromCatalog);
    }
}
