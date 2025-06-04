import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Анализатор на отчети за грешки по месеци
 * Чете CSV файл с отчети за грешки и генерира кумулативна статистика по месеци
 */
public class BugReportAnalyzerMonthly {

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

        // Дефиниране на обхвата от месеци - можете да промените тези стойности
        int numberOfMonths = 19;
        YearMonth startMonth = YearMonth.of(2015, 1); // Начален месец (променете по нужда)
        YearMonth endMonth = startMonth.plusMonths(numberOfMonths - 1); // Краен месец

        System.out.println("Анализираме грешки от " + startMonth + " до " + endMonth + " (" + numberOfMonths + " месеца)");

        // Филтрираме отчетите за грешки в зададения обхват и ги броим по месеци
        Map<YearMonth, Long> monthlyBugCounts = bugReports.stream()
                .map(BugReport::getCreationDate) // Взимаме датата на създаване
                .filter(Objects::nonNull) // Премахваме null стойностите
                .filter(date -> {
                    YearMonth bugMonth = YearMonth.from(date); // Преобразуваме в година-месец
                    // Филтрираме само датите в зададения период
                    return !bugMonth.isBefore(startMonth) && !bugMonth.isAfter(endMonth);
                })
                .collect(Collectors.groupingBy(
                        date -> YearMonth.from(date), // Групираме по година-месец
                        TreeMap::new, // Използваме TreeMap за сортирани ключове
                        Collectors.counting() // Броим грешките в всеки месец
                ));

        // Генериране на всички месеци в обхвата (включително месеци с 0 грешки)
        List<YearMonth> allMonthsInRange = new ArrayList<>();
        YearMonth current = startMonth; // Започваме от началния месец
        while (!current.isAfter(endMonth)) {
            allMonthsInRange.add(current); // Добавяме месеца в списъка
            current = current.plusMonths(1); // Преминаваме към следващия месец
        }

        // Генериране на кумулативна сума с номер на месеца и кумулативен брой
        List<int[]> cumulativeResult = new ArrayList<>();
        int cumulativeSum = 0; // Кумулативна сума на грешките

        // Обхождаме всички месеци в зададения обхват
        for (int i = 0; i < allMonthsInRange.size(); i++) {
            YearMonth month = allMonthsInRange.get(i);
            long monthlyCount = monthlyBugCounts.getOrDefault(month, 0L); // Брой грешки за този месец
            cumulativeSum += monthlyCount; // Добавяме към кумулативната сума
            cumulativeResult.add(new int[]{i + 1, cumulativeSum}); // Индексът на месеца започва от 1
        }

        // Отпечатваме резултата в заявения формат
        System.out.println("Месечен кумулативен резултат: " + formatResult(cumulativeResult));

        // Също така отпечатваме месечните броения за справка
        System.out.println("\nМесечни броения на грешки:");
        for (int i = 0; i < allMonthsInRange.size(); i++) {
            YearMonth month = allMonthsInRange.get(i);
            long count = monthlyBugCounts.getOrDefault(month, 0L);
            System.out.println("Месец " + (i + 1) + " (" + month + "): " + count + " грешки");
        }

        // Отпечатваме общия брой намерени грешки в обхвата
        System.out.println("\nОбщо грешки в " + numberOfMonths + " месеца: " + cumulativeSum);
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
     * @param result списък с двойки числа [месец, кумулативен брой]
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
