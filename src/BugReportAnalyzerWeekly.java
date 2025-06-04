import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Анализатор на отчети за грешки по седмици
 * Чете CSV файл с отчети за грешки и генерира кумулативна статистика по седмици
 */
public class BugReportAnalyzerWeekly {

    public static void main(String[] args) {
        // Път към CSV файла с данните за отчетите за грешки
        String csvFile = "data/winehq_bug_report_data.csv";
        // Списък за съхранение на всички отчети за грешки
        List<BugReport> bugReports = new ArrayList<>();

        // Четене на CSV файла
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isHeader = true; // Флаг за пропускане на заглавния ред
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // Пропускаме първия ред (заглавия)
                    continue;
                }
                // Обработка на CSV парсинг с възможни запетайки в кавички
                String[] values = parseCSVLine(line);
                if (values.length > 1) {
                    String creationDate = values[1].trim(); // Втората колона е датата на създаване
                    try {
                        // Създаване на нов отчет за грешка с датата
                        bugReports.add(new BugReport(creationDate));
                    } catch (Exception e) {
                        // Обработка на грешки при парсиране на датата
                        System.err.println("Грешка при парсиране на дата: " + creationDate);
                    }
                }
            }
        } catch (IOException e) {
            // Обработка на грешки при четене на файла
            e.printStackTrace();
        }

        // Дефиниране на обхвата от седмици - можете да промените тези дати
        LocalDate startDate = LocalDate.of(2003, 10, 1); // Начална дата за анализа
        int numberOfWeeks = 19; // Анализираме 19 седмици (около 5 месеца)
        LocalDate endDate = startDate.plusWeeks(numberOfWeeks - 1).plusDays(6); // Завършваме последната седмица

        System.out.println("Анализираме грешки от " + startDate + " до " + endDate + " (" + numberOfWeeks + " седмици)");

        // Филтрираме отчетите за грешки в зададения обхват и ги броим по седмици
        Map<Integer, Long> weeklyBugCounts = bugReports.stream()
                .map(BugReport::getCreationDate) // Взимаме датата на създаване
                .filter(Objects::nonNull) // Премахваме null стойностите
                .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate)) // Филтрираме по дати
                .collect(Collectors.groupingBy(
                        date -> getWeekNumber(date, startDate), // Групираме по номер на седмицата
                        TreeMap::new, // Използваме TreeMap за сортирани ключове
                        Collectors.counting() // Броим грешките в всяка седмица
                ));

        // Генериране на кумулативна сума с номер на седмицата и кумулативен брой
        List<int[]> cumulativeResult = new ArrayList<>();
        int cumulativeSum = 0; // Кумулативна сума на грешките

        // Обхождаме всички седмици в зададения обхват
        for (int week = 1; week <= numberOfWeeks; week++) {
            long weeklyCount = weeklyBugCounts.getOrDefault(week, 0L); // Брой грешки за тази седмица
            cumulativeSum += weeklyCount; // Добавяме към кумулативната сума
            cumulativeResult.add(new int[]{week, cumulativeSum}); // Запазваме резултата
        }

        // Отпечатваме резултата в заявения формат
        System.out.println("Кумулативен седмичен резултат: " + formatResult(cumulativeResult));

        // Също така отпечатваме седмичните броения за справка
        System.out.println("\nСедмични броения на грешки:");
        for (int week = 1; week <= numberOfWeeks; week++) {
            long count = weeklyBugCounts.getOrDefault(week, 0L);
            LocalDate weekStart = startDate.plusWeeks(week - 1); // Начало на седмицата
            LocalDate weekEnd = weekStart.plusDays(6); // Край на седмицата
            System.out.println("Седмица " + week + " (" + weekStart + " до " + weekEnd + "): " + count + " грешки");
        }

        // Отпечатваме общия брой грешки в зададения период
        System.out.println("\nОбщо грешки в " + numberOfWeeks + " седмици: " + cumulativeSum);
    }

    /**
     * Помощен метод за изчисляване на номера на седмицата спрямо началната дата
     *
     * @param date      датата за която искаме да намерим номера на седмицата
     * @param startDate началната дата на анализа
     * @return номера на седмицата (започвайки от 1)
     */
    private static int getWeekNumber(LocalDate date, LocalDate startDate) {
        long daysDifference = date.toEpochDay() - startDate.toEpochDay(); // Разлика в дни
        return (int) (daysDifference / 7) + 1; // Преобразуваме в седмици и добавяме 1
    }

    /**
     * Помощен метод за парсиране на CSV редове, които могат да съдържат запетайки в кавички
     *
     * @param line редът от CSV файла за парсиране
     * @return масив от стрингове с разделените стойности
     */
    private static String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false; // Флаг дали сме вътре в кавички
        StringBuilder current = new StringBuilder(); // Текущата стойност

        // Обхождаме всеки символ от реда
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes; // Превключваме състоянието на кавичките
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString()); // Добавяме текущата стойност
                current = new StringBuilder(); // Започваме нова стойност
            } else {
                current.append(c); // Добавяме символа към текущата стойност
            }
        }
        result.add(current.toString()); // Добавяме последната стойност

        return result.toArray(new String[0]);
    }

    /**
     * Помощен метод за форматиране на резултата в заявения формат
     *
     * @param result списък с двойки числа [седмица, кумулативен брой]
     * @return форматиран стринг във вида {{1,5}, {2,12}, ...}
     */
    private static String formatResult(List<int[]> result) {
        StringBuilder sb = new StringBuilder("{");
        // Обхождаме всички двойки в резултата
        for (int i = 0; i < result.size(); i++) {
            int[] pair = result.get(i);
            sb.append("{").append(pair[0]).append(",").append(pair[1]).append("}");
            if (i < result.size() - 1) {
                sb.append(", "); // Добавяме запетайка между двойките
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
