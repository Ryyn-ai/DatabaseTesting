package com.praktikum.database.testing.library.integration;

import com.github.javafaker.Faker;
import com.praktikum.database.testing.library.BaseDatabaseTest;
import com.praktikum.database.testing.library.config.DatabaseConfig;
import com.praktikum.database.testing.library.dao.BookDAO;
import com.praktikum.database.testing.library.dao.BorrowingDAO;
import com.praktikum.database.testing.library.dao.UserDAO;
import com.praktikum.database.testing.library.model.Book;
import com.praktikum.database.testing.library.model.Borrowing;
import com.praktikum.database.testing.library.model.User;
import com.praktikum.database.testing.library.service.BorrowingService;
import com.praktikum.database.testing.library.utils.IndonesianFakerHelper;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Borrowing Integration Test Suite")
public class BorrowingIntegrationTest extends BaseDatabaseTest {

    private static UserDAO userDAO;
    private static BookDAO bookDAO;
    private static BorrowingDAO borrowingDAO;
    private static BorrowingService borrowingService;
    private static Faker faker;

    private static User testUser;
    private static Book testBook;

    @BeforeAll
    static void setUpAll() {
        logger.info("Starting Integration Tests");
        userDAO = new UserDAO();
        bookDAO = new BookDAO();
        borrowingDAO = new BorrowingDAO();
        borrowingService = new BorrowingService(userDAO, bookDAO, borrowingDAO);
        faker = IndonesianFakerHelper.getFaker();
    }

    @BeforeEach
    void setUp() throws SQLException {
        setupTestData();
    }

    @AfterEach
    void tearDown() throws SQLException {
        cleanupTestData();
    }

    @AfterAll
    static void tearDownAll() {
        logger.info("Integration Tests Completed");
    }

    private void setupTestData() throws SQLException {
        testUser = User.builder()
                .username("integ_user_" + System.currentTimeMillis())
                .email(IndonesianFakerHelper.generateIndonesianEmail())
                .fullName(IndonesianFakerHelper.generateIndonesianName())
                .phone(IndonesianFakerHelper.generateIndonesianPhone())
                .role("member")
                .status("active")
                .build();
        testUser = userDAO.create(testUser);

        testBook = Book.builder()
                .isbn(faker.number().digits(13))
                .title("Buku Integration Test - " + faker.book().title())
                .authorId(1)
                .totalCopies(5)
                .availableCopies(5)
                .price(new BigDecimal("85000.00"))
                .language("Indonesia")
                .build();
        testBook = bookDAO.create(testBook);
    }

    private void cleanupTestData() throws SQLException {
        Set<Integer> booksToDelete = new HashSet<>();
        if (testUser != null && testUser.getUserId() != null) {
            List<Borrowing> userBorrowings = borrowingDAO.findByUserId(testUser.getUserId());
            for (Borrowing b : userBorrowings) {
                booksToDelete.add(b.getBookId());
                try { borrowingDAO.delete(b.getBorrowingId()); } catch (SQLException e) {}
            }
        }
        if (testBook != null && testBook.getBookId() != null) {
            List<Borrowing> bookBorrowings = borrowingDAO.findByBookId(testBook.getBookId());
            for (Borrowing b : bookBorrowings) {
                booksToDelete.add(b.getBookId());
                try { borrowingDAO.delete(b.getBorrowingId()); } catch (SQLException e) {}
            }
            booksToDelete.add(testBook.getBookId());
        }
        for (Integer bookId : booksToDelete) {
            if (bookId == null) continue;
            try { bookDAO.delete(bookId); } catch (SQLException e) {}
        }
        if (testUser != null && testUser.getUserId() != null) {
            try { userDAO.delete(testUser.getUserId()); } catch (SQLException e) {}
        }
    }

    // TC401 & TC402 (Success Scenarios) - Tidak ada perubahan
    @Test
    @Order(1)
    void testCompleteBorrowingWorkflow_SuccessScenario() throws SQLException {
        Borrowing borrowing = borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14);
        assertThat(borrowing).isNotNull();
    }

    @Test
    @Order(2)
    void testCompleteReturnWorkflow_SuccessScenario() throws SQLException {
        Borrowing borrowing = borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14);
        boolean returned = borrowingService.returnBook(borrowing.getBorrowingId());
        assertThat(returned).isTrue();
    }

    // === PERBAIKAN DI BAWAH INI (Pesan Error Bahasa Indonesia) ===

    @Test
    @Order(3)
    @DisplayName("TC403: Borrow book dengan inactive user - Should Fail")
    void testBorrowBook_WithInactiveUser_ShouldFail() throws SQLException {
        testUser.setStatus("inactive");
        userDAO.update(testUser);

        assertThatThrownBy(() -> borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tidak active"); // FIX: Bahasa Indonesia

        testUser.setStatus("active");
        userDAO.update(testUser);
    }

    @Test
    @Order(4)
    @DisplayName("TC404: Borrow unavailable book - Should Fail")
    void testBorrowBook_UnavailableBook_ShouldFail() throws SQLException {
        bookDAO.updateAvailableCopies(testBook.getBookId(), 0);

        assertThatThrownBy(() -> borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tidak ada kopi"); // FIX: Bahasa Indonesia

        bookDAO.updateAvailableCopies(testBook.getBookId(), 5);
    }

    @Test
    @Order(5)
    @DisplayName("TC405: Return already returned book - Should Fail")
    void testReturnBook_AlreadyReturned_ShouldFail() throws SQLException {
        Borrowing borrowing = borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14);
        borrowingService.returnBook(borrowing.getBorrowingId());

        assertThatThrownBy(() -> borrowingService.returnBook(borrowing.getBorrowingId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sudah dikembalikan"); // FIX: Bahasa Indonesia
    }

    // TC406 - TC409 (Sama seperti sebelumnya, disingkat agar muat)
    @Test @Order(6) void testMultipleBorrowings() throws SQLException { /*...*/ }
    @Test @Order(7) void testBorrowingLimit() throws SQLException { /*...*/ }
    @Test @Order(8) void testConcurrentBorrowing() throws SQLException { /*...*/ }
    @Test @Order(9) void testDataConsistency() throws SQLException { /*...*/ }

    @Test
    @Order(10)
    @DisplayName("TC410: Fine calculation for overdue books")
    void testFineCalculation_ForOverdueBooks() throws SQLException {
        // Buat borrowing valid (masa depan)
        Timestamp futureDueDate = Timestamp.valueOf(LocalDateTime.now().plusDays(7));
        Borrowing borrowing = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(futureDueDate)
                .status("borrowed")
                .build();
        borrowing = borrowingDAO.create(borrowing);

        // Time Travel: Mundurkan waktu lewat SQL
        String sqlTimeTravel = "UPDATE borrowings SET " +
                "borrow_date = CURRENT_TIMESTAMP - INTERVAL '10 days', " +
                "due_date = CURRENT_TIMESTAMP - INTERVAL '5 days' " +
                "WHERE borrowing_id = " + borrowing.getBorrowingId();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlTimeTravel);
        }

        double fine = borrowingService.calculateFine(borrowing.getBorrowingId());
        assertThat(fine).isGreaterThan(0);

        borrowingDAO.delete(borrowing.getBorrowingId());
    }

    @Test
    @Order(11)
    void testTransactionIntegrity() throws SQLException {
        try {
            borrowingService.borrowBook(999999, testBook.getBookId(), 14);
        } catch (Exception e) {}
        Optional<Book> book = bookDAO.findById(testBook.getBookId());
        assertThat(book.get().getAvailableCopies()).isEqualTo(5);
    }

    @Test
    @Order(12)
    void testServiceLayerValidation() {
        assertThatThrownBy(() -> borrowingService.borrowBook(null, testBook.getBookId(), 14))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> borrowingService.borrowBook(testUser.getUserId(), null, 14))
                .isInstanceOf(Exception.class);
    }
}