import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Клас представляващ отчет за грешка
 * Съдържа датата на създаване на отчета
 */
class BugReport {
    private LocalDate creationDate; // Дата на създаване на отчета за грешката

    /**
     * Конструктор за създаване на отчет за грешка
     * @param creationDate дата на създаване като стринг във формат yyyy-MM-dd
     */
    public BugReport(String creationDate) {
        // Парсираме датата от стринг във формат yyyy-MM-dd
        this.creationDate = LocalDate.parse(creationDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * Getter метод за получаване на датата на създаване
     * @return датата на създаване на отчета
     */
    public LocalDate getCreationDate() {
        return creationDate;
    }
}
