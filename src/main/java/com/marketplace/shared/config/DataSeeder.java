package com.marketplace.shared.config;

import com.marketplace.product.model.Category;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.CategoryRepository;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.review.model.Review;
import com.marketplace.review.repository.ReviewRepository;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
        UserRepository userRepository,
        CategoryRepository categoryRepository,
        ProductRepository productRepository,
        ReviewRepository reviewRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdminAccount();
        seedSellersAndBuyer();
        seedProducts();
        seedReviews();
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
            new UserSeed("seller1@marketplace.test", "Shop One", User.Role.SELLER, "123 Nguyen Hue", "Thua Thien Hue", "Thua Thien Hue", "Phu Hoi"),
            new UserSeed("seller2@marketplace.test", "Shop Two", User.Role.SELLER, "456 Le Loi", "Da Nang", "Hai Chau", "Thach Thang"),
            new UserSeed("seller3@marketplace.test", "Shop Three", User.Role.SELLER, "789 Tran Phu", "Ho Chi Minh", "Quan 1", "Ben Nghe"),
            new UserSeed("buyer1@marketplace.test", "Test Buyer", User.Role.BUYER, "101 Bach Dang", "Hai Phong", "Hong Bang", "Quang Trung"),
            new UserSeed("buyer2@marketplace.test", "Nguyen Van A", User.Role.BUYER, "202 Dien Bien Phu", "Ho Chi Minh", "Quan 3", "Vo Thi Sau"),
            new UserSeed("buyer3@marketplace.test", "Tran Thi B", User.Role.BUYER, "303 Hai Ba Trung", "Ha Noi", "Hoan Kiem", "Hang Bai"),
            new UserSeed("buyer4@marketplace.test", "Le Van C", User.Role.BUYER, "404 Ly Thuong Kiet", "Da Nang", "Thanh Khe", "Hoa Cu Bac"),
            new UserSeed("buyer5@marketplace.test", "Pham Thi D", User.Role.BUYER, "505 Nguyen Trai", "Can Tho", "Ninh Kieu", "An Binh")
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
            user.setDefaultStreet(seed.street());
            user.setDefaultProvince(seed.province());
            user.setDefaultDistrict(seed.district());
            user.setDefaultWard(seed.ward());
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
            new ProductSeed("electronics", "Wireless Earbuds Pro", "Noise-cancelling wireless earbuds", new BigDecimal("1290000.00"), 50, 156),
            new ProductSeed("electronics", "4K Action Camera", "Waterproof 4K action camera", new BigDecimal("2490000.00"), 30, 89),
            new ProductSeed("clothing", "Cotton T-Shirt", "Soft 100% cotton t-shirt", new BigDecimal("150000.00"), 200, 312),
            new ProductSeed("clothing", "Denim Jacket", "Classic blue denim jacket", new BigDecimal("450000.00"), 80, 45),
            new ProductSeed("home-garden", "Ceramic Plant Pot", "Minimalist ceramic plant pot", new BigDecimal("99000.00"), 120, 203),
            new ProductSeed("home-garden", "LED Desk Lamp", "Adjustable warm-white LED lamp", new BigDecimal("320000.00"), 60, 78),
            new ProductSeed("books", "Clean Code", "Handbook of agile software craftsmanship", new BigDecimal("280000.00"), 150, 445),
            new ProductSeed("books", "The Pragmatic Programmer", "Classic software engineering read", new BigDecimal("310000.00"), 90, 267),
            new ProductSeed("sports", "Yoga Mat", "Non-slip eco-friendly yoga mat", new BigDecimal("220000.00"), 100, 134),
            new ProductSeed("sports", "Dumbbell Set 10kg", "Pair of neoprene dumbbells", new BigDecimal("540000.00"), 40, 56),
            new ProductSeed("beauty", "Vitamin C Serum", "Brightening facial serum", new BigDecimal("350000.00"), 75, 189),
            new ProductSeed("beauty", "Matte Lipstick", "Long-lasting matte lipstick", new BigDecimal("180000.00"), 130, 234),
            new ProductSeed("toys", "Wooden Puzzle", "Brain-teaser wooden puzzle", new BigDecimal("120000.00"), 110, 98),
            new ProductSeed("toys", "Board Game Deluxe", "Strategy board game for families", new BigDecimal("490000.00"), 55, 67),
            new ProductSeed("automotive", "Car Phone Mount", "Magnetic dashboard phone mount", new BigDecimal("160000.00"), 140, 321),
            new ProductSeed("automotive", "Microfiber Towel 5pk", "Ultra-absorbent cleaning towels", new BigDecimal("90000.00"), 220, 178)
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
            product.setSoldCount(seed.soldCount());
            product.setActive(true);
            productRepository.save(product);
            log.info("Seed product created: {} (sold={})", seed.name(), seed.soldCount());

            sellerIndex++;
        }
    }

    private void seedReviews() {
        List<User> buyers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.BUYER)
                .toList();
        if (buyers.isEmpty()) {
            log.warn("Skipping review seeding: no buyers found");
            return;
        }

        String[] reviewTexts = {
            "Sản phẩm rất tốt, đáng giá tiền!",
            "Chất lượng tuyệt vời, giao hàng nhanh.",
            "Đúng như mô tả, rất hài lòng.",
            "Sẽ mua lại lần sau.",
            "Tốt nhưng giao hàng hơi chậm.",
            "Sản phẩm đẹp, đóng gói cẩn thận.",
            "Rất ưng ý với sản phẩm này.",
            "Chất lượng ổn, giá cả phải chăng.",
            "Hoàn hảo cho nhu cầu của tôi.",
            "Đánh giá 5 sao, rất recommend!"
        };

        List<Product> products = productRepository.findAll();
        int reviewIndex = 0;
        for (Product product : products) {
            for (User buyer : buyers) {
                if (reviewRepository.existsByProductIdAndBuyerId(product.getId(), buyer.getId())) {
                    continue;
                }
                int rating = ThreadLocalRandom.current().nextInt(3, 6);
                String comment = reviewTexts[reviewIndex % reviewTexts.length];
                Review review = new Review(product.getId(), buyer.getId(), rating, comment, true);
                reviewRepository.save(review);
                reviewIndex++;
            }

            List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
            product.setAverageRating(Math.round(avg * 10.0) / 10.0);
            product.setReviewCount(reviews.size());
            productRepository.save(product);
            log.info("Product {} updated: rating={}, reviews={}", product.getName(), product.getAverageRating(), product.getReviewCount());
        }
    }

    private String toSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }

    private record UserSeed(String email, String displayName, User.Role role, String street, String province, String district, String ward) {}

    private record ProductSeed(
        String categorySlug,
        String name,
        String description,
        BigDecimal price,
        int stock,
        int soldCount
    ) {}
}
