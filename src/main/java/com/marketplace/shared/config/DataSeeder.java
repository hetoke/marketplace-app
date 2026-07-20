package com.marketplace.shared.config;

import com.marketplace.product.model.Category;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.CategoryRepository;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
        UserRepository userRepository,
        CategoryRepository categoryRepository,
        ProductRepository productRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdminAccount();
        seedSellersAndBuyer();
        seedProducts();
    }

    private void seedAdminAccount() {
        String email = System.getenv().getOrDefault("ADMIN_EMAIL", "admin@marketplace.com");
        String password = System.getenv().getOrDefault("ADMIN_PASSWORD", "Admin123!");

        if (userRepository.existsByEmail(email)) {
            log.info("Admin account already exists: {}", email);
            return;
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setVerified(true);
        admin.setAuthenticationType(User.AuthenticationType.LOCAL);
        admin.setDisplayName("Admin");
        userRepository.save(admin);

        log.info("Admin account created: {}", email);
    }

    private void seedSellersAndBuyer() {
        String password = System.getenv().getOrDefault("SEED_USER_PASSWORD", "Password123!");

        List<UserSeed> seeds = List.of(
            new UserSeed("seller1@marketplace.test", "Shop One", User.Role.SELLER),
            new UserSeed("seller2@marketplace.test", "Shop Two", User.Role.SELLER),
            new UserSeed("seller3@marketplace.test", "Shop Three", User.Role.SELLER),
            new UserSeed("buyer1@marketplace.test", "Test Buyer", User.Role.BUYER)
        );

        for (UserSeed seed : seeds) {
            if (userRepository.existsByEmail(seed.email())) {
                log.info("Seed user already exists: {}", seed.email());
                continue;
            }
            User user = new User();
            user.setEmail(seed.email());
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(seed.role());
            user.setVerified(true);
            user.setStatus(User.UserStatus.ACTIVE);
            user.setAuthenticationType(User.AuthenticationType.LOCAL);
            user.setDisplayName(seed.displayName());
            userRepository.save(user);
            log.info("Seed user created: {} ({})", seed.email(), seed.role());
        }
    }

    private void seedProducts() {
        List<String> sellerEmails = List.of(
            "seller1@marketplace.test",
            "seller2@marketplace.test",
            "seller3@marketplace.test"
        );

        List<ProductSeed> seeds = List.of(
            new ProductSeed("electronics", "Wireless Earbuds Pro", "Noise-cancelling wireless earbuds", new BigDecimal("1290000.00"), 50),
            new ProductSeed("electronics", "4K Action Camera", "Waterproof 4K action camera", new BigDecimal("2490000.00"), 30),
            new ProductSeed("clothing", "Cotton T-Shirt", "Soft 100% cotton t-shirt", new BigDecimal("150000.00"), 200),
            new ProductSeed("clothing", "Denim Jacket", "Classic blue denim jacket", new BigDecimal("450000.00"), 80),
            new ProductSeed("home-garden", "Ceramic Plant Pot", "Minimalist ceramic plant pot", new BigDecimal("99000.00"), 120),
            new ProductSeed("home-garden", "LED Desk Lamp", "Adjustable warm-white LED lamp", new BigDecimal("320000.00"), 60),
            new ProductSeed("books", "Clean Code", "Handbook of agile software craftsmanship", new BigDecimal("280000.00"), 150),
            new ProductSeed("books", "The Pragmatic Programmer", "Classic software engineering read", new BigDecimal("310000.00"), 90),
            new ProductSeed("sports", "Yoga Mat", "Non-slip eco-friendly yoga mat", new BigDecimal("220000.00"), 100),
            new ProductSeed("sports", "Dumbbell Set 10kg", "Pair of neoprene dumbbells", new BigDecimal("540000.00"), 40),
            new ProductSeed("beauty", "Vitamin C Serum", "Brightening facial serum", new BigDecimal("350000.00"), 75),
            new ProductSeed("beauty", "Matte Lipstick", "Long-lasting matte lipstick", new BigDecimal("180000.00"), 130),
            new ProductSeed("toys", "Wooden Puzzle", "Brain-teaser wooden puzzle", new BigDecimal("120000.00"), 110),
            new ProductSeed("toys", "Board Game Deluxe", "Strategy board game for families", new BigDecimal("490000.00"), 55),
            new ProductSeed("automotive", "Car Phone Mount", "Magnetic dashboard phone mount", new BigDecimal("160000.00"), 140),
            new ProductSeed("automotive", "Microfiber Towel 5pk", "Ultra-absorbent cleaning towels", new BigDecimal("90000.00"), 220)
        );

        int sellerIndex = 0;
        for (ProductSeed seed : seeds) {
            if (productRepository.findBySlug(toSlug(seed.name())).isPresent()) {
                log.info("Seed product already exists: {}", seed.name());
                continue;
            }
            Category category = categoryRepository.findBySlug(seed.categorySlug()).orElse(null);
            if (category == null) {
                log.warn("Skipping seed product '{}': category '{}' not found", seed.name(), seed.categorySlug());
                continue;
            }
            String sellerEmail = sellerEmails.get(sellerIndex % sellerEmails.size());
            User seller = userRepository.findByEmail(sellerEmail).orElse(null);
            if (seller == null) {
                log.warn("Skipping seed product '{}': seller '{}' not found", seed.name(), sellerEmail);
                continue;
            }

            Product product = new Product();
            product.setSellerId(seller.getId());
            product.setCategory(category);
            product.setName(seed.name());
            product.setSlug(toSlug(seed.name()));
            product.setDescription(seed.description());
            product.setPrice(seed.price());
            product.setStock(seed.stock());
            product.setActive(true);
            productRepository.save(product);
            log.info("Seed product created: {} ({})", seed.name(), seed.categorySlug());

            sellerIndex++;
        }
    }

    private String toSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }

    private record UserSeed(String email, String displayName, User.Role role) {}

    private record ProductSeed(
        String categorySlug,
        String name,
        String description,
        BigDecimal price,
        int stock
    ) {}
}
