import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class StudentLibraryManagement {
    private final Scanner scanner;
    private final Library library;

    private StudentLibraryManagement(Scanner scanner) {
        this.scanner = scanner;
        this.library = new Library();
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            new StudentLibraryManagement(scanner).run();
        }
    }

    private void run() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readNumber("Select an option: ", 0, 7);
            switch (choice) {
                case 1:
                    addBook();
                    break;
                case 2:
                    library.listBooks();
                    break;
                case 3:
                    registerStudent();
                    break;
                case 4:
                    borrowBook();
                    break;
                case 5:
                    returnBook();
                    break;
                case 6:
                    library.listStudents();
                    break;
                case 7:
                    searchBooks();
                    break;
                case 0:
                    running = false;
                    System.out.println("Goodbye.");
                    break;
                default:
                    throw new IllegalStateException("Unexpected menu option: " + choice);
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== Student Library Management ===");
        System.out.println("1. Add book");
        System.out.println("2. List books");
        System.out.println("3. Register student");
        System.out.println("4. Borrow book");
        System.out.println("5. Return book");
        System.out.println("6. List students");
        System.out.println("7. Search books");
        System.out.println("0. Exit");
    }

    private void addBook() {
        String id = readText("Book ID: ");
        String title = readText("Title: ");
        String author = readText("Author: ");
        int copies = readNumber("Number of copies: ", 1, Integer.MAX_VALUE);
        System.out.println(library.addBook(id, title, author, copies));
    }

    private void registerStudent() {
        String id = readText("Student ID: ");
        String name = readText("Student name: ");
        System.out.println(library.registerStudent(id, name));
    }

    private void borrowBook() {
        String studentId = readText("Student ID: ");
        String bookId = readText("Book ID: ");
        System.out.println(library.borrowBook(studentId, bookId));
    }

    private void returnBook() {
        String studentId = readText("Student ID: ");
        String bookId = readText("Book ID: ");
        System.out.println(library.returnBook(studentId, bookId));
    }

    private void searchBooks() {
        String query = readText("Search title or author: ");
        library.searchBooks(query);
    }

    private String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    private int readNumber(String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= minimum && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Enter a whole number from " + minimum + " to " + maximum + ".");
        }
    }

    private static class Library {
        private static final int MAX_BOOKS_PER_STUDENT = 5;
        private final Map<String, Book> books = new LinkedHashMap<>();
        private final Map<String, Student> students = new LinkedHashMap<>();

        private synchronized String addBook(String id, String title, String author, int copies) {
            String key = normalize(id);
            if (books.containsKey(key)) {
                return "A book with that ID already exists.";
            }
            books.put(key, new Book(key, title, author, copies));
            return "Book added.";
        }

        private synchronized String registerStudent(String id, String name) {
            String key = normalize(id);
            if (students.containsKey(key)) {
                return "A student with that ID is already registered.";
            }
            students.put(key, new Student(key, name));
            return "Student registered.";
        }

        private synchronized String borrowBook(String studentId, String bookId) {
            Student student = students.get(normalize(studentId));
            if (student == null) {
                return "Student not found. Register the student first.";
            }

            Book book = books.get(normalize(bookId));
            if (book == null) {
                return "Book not found.";
            }
            if (book.availableCopies == 0) {
                return "No copies of this book are currently available.";
            }

            int borrowedCount = student.borrowedCopies.values().stream().mapToInt(Integer::intValue).sum();
            if (borrowedCount >= MAX_BOOKS_PER_STUDENT) {
                return "A student may borrow up to " + MAX_BOOKS_PER_STUDENT + " books.";
            }

            book.availableCopies--;
            student.borrowedCopies.put(book.id, student.borrowedCopies.getOrDefault(book.id, 0) + 1);
            return "Borrowed: " + book.title;
        }

        private synchronized String returnBook(String studentId, String bookId) {
            Student student = students.get(normalize(studentId));
            if (student == null) {
                return "Student not found.";
            }

            String key = normalize(bookId);
            Book book = books.get(key);
            int borrowedCopies = student.borrowedCopies.getOrDefault(key, 0);
            if (book == null || borrowedCopies == 0) {
                return "This student has no recorded loan for that book.";
            }

            if (borrowedCopies == 1) {
                student.borrowedCopies.remove(key);
            } else {
                student.borrowedCopies.put(key, borrowedCopies - 1);
            }
            book.availableCopies++;
            return "Returned: " + book.title;
        }

        private synchronized void listBooks() {
            if (books.isEmpty()) {
                System.out.println("No books have been added.");
                return;
            }
            for (Book book : books.values()) {
                printBook(book);
            }
        }

        private synchronized void searchBooks(String query) {
            String search = query.toLowerCase(Locale.ROOT);
            boolean found = false;
            for (Book book : books.values()) {
                if (book.title.toLowerCase(Locale.ROOT).contains(search)
                        || book.author.toLowerCase(Locale.ROOT).contains(search)) {
                    printBook(book);
                    found = true;
                }
            }
            if (!found) {
                System.out.println("No matching books found.");
            }
        }

        private synchronized void listStudents() {
            if (students.isEmpty()) {
                System.out.println("No students have been registered.");
                return;
            }
            for (Student student : students.values()) {
                int borrowedCount = student.borrowedCopies.values().stream().mapToInt(Integer::intValue).sum();
                System.out.println(student.id + " | " + student.name + " | Books borrowed: " + borrowedCount);
            }
        }

        private void printBook(Book book) {
            System.out.println(book.id + " | " + book.title + " | " + book.author
                    + " | Available: " + book.availableCopies + "/" + book.totalCopies);
        }

        private static String normalize(String id) {
            return id.trim().toUpperCase(Locale.ROOT);
        }
    }

    private static class Book {
        private final String id;
        private final String title;
        private final String author;
        private final int totalCopies;
        private int availableCopies;

        private Book(String id, String title, String author, int copies) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.totalCopies = copies;
            this.availableCopies = copies;
        }
    }

    private static class Student {
        private final String id;
        private final String name;
        private final Map<String, Integer> borrowedCopies = new LinkedHashMap<>();

        private Student(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
