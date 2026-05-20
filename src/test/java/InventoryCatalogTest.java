import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryCatalogTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    private static Product sampleProduct() {
        return new Product("P-100", "Widget", 10, 5);
    }

    @Test
    void productEqualityUsesProductIdOnly() {
        Product p1 = new Product("P-1", "A", 1, 1);
        Product p2 = new Product("P-1", "B", 9, 9);
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
        catalog.addProduct(new Product("P-1", "One", 1, 1));
        catalog.addProduct(new Product("P-2", "Two", 1, 1));
        catalog.addProduct(new Product("P-3", "Three", 1, 1));

        catalog.setPrice("P-1", USD, new BigDecimal("12.00"));
        catalog.setPrice("P-2", USD, new BigDecimal("10.00"));
        catalog.setPrice("P-3", USD, new BigDecimal("11.00"));

        List<Product> sorted = catalog.listProductsSortedByPrice(USD);
        assertEquals(List.of("P-2", "P-3", "P-1"), sorted.stream().map(Product::getProductId).toList());
    }

    @Test
    void recordSaleConcurrentShouldBeAtomic() throws Exception {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-1", "One", 100, 1));

        int threads = 10;
        int eachQty = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                start.await();
                catalog.recordSale("P-1", eachQty);
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) futures.add(pool.submit(task));
        start.countDown();
        for (Future<Void> f : futures) f.get();
        pool.shutdown();

        assertEquals(50, catalog.getProduct("P-1").getQuantity());
        assertEquals(List.of(new ProductSales("P-1", 50)), catalog.topBestSellingProducts(1));
    }

    @Test
    void recordSaleConcurrentOversaleShouldThrowForSomeRequests() throws Exception {
        InventoryCatalog catalog = new InventoryCatalog();
        catalog.addProduct(new Product("P-1", "One", 10, 1));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                start.await();
                try {
                    catalog.recordSale("P-1", 2);
                    return true;
                } catch (IllegalArgumentException ex) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) futures.add(pool.submit(task));
        start.countDown();

        int success = 0;
        int failed = 0;
        for (Future<Boolean> f : futures) {
            if (f.get()) success++; else failed++;
        }
        pool.shutdown();

        assertEquals(5, success);
        assertEquals(3, failed);
        assertEquals(0, catalog.getProduct("P-1").getQuantity());
        assertEquals(List.of(new ProductSales("P-1", 10)), catalog.topBestSellingProducts(1));
        assertTrue(failed > 0);
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
