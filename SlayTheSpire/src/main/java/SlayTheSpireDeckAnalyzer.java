import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.io.image.ImageDataFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import javax.imageio.ImageIO;

public class SlayTheSpireDeckAnalyzer {
    private static final int DECK_ID_LENGTH = 9; // Length of the unique deck ID
    private static final int MAX_CARDS = 1000; // Maximum number of cards allowed
    private static final int MAX_INVALID_CARDS = 10; // Maximum number of invalid cards before voiding report

    // Predefined list of valid Slay the Spire card names
    private static final Set<String> VALID_CARDS = new HashSet<>(Arrays.asList(
            "Strike", "Genetic Algorithm", "Crush", "Biased Cognition", "All For One"
    ));

    /**
     * Generates a unique random 9-digit ID
     *
     * @return A unique 9-digit ID as a string
     */
    private static String generateDeckId() {
        return String.format("%09d", new Random().nextInt(1000000000));
    }

    /**
     * Reads card names and their energy costs from the file
     *
     * @param fileName The name of the input file containing card data
     * @return A list of valid card costs and a list of invalid card entries
     */
    public static List<Integer> readDeck(String fileName) {
        List<Integer> energyCosts = new ArrayList<>();
        List<String> invalidCards = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String cardName = parts[0].trim();
                    String costStr = parts[1].trim();
                    try {
                        int cost = Integer.parseInt(costStr);
                        // Validate card name and cost
                        if (cost >= 0 && cost <= 6 && !cardName.isEmpty() && VALID_CARDS.contains(cardName)) {
                            energyCosts.add(cost);
                        } else {
                            invalidCards.add(line);
                        }
                    } catch (NumberFormatException e) {
                        invalidCards.add(line);
                    }
                } else {
                    invalidCards.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return energyCosts;
    }

    /**
     * Calculates the total energy cost of the given energy costs
     *
     * @param energyCosts The list of energy costs
     * @return The total energy cost
     */
    public static int totalEnergyCost(List<Integer> energyCosts) {
        return energyCosts.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Creates a histogram of the energy costs
     *
     * @param energyCosts The list of energy costs
     * @throws IOException If there is an issue creating the histogram image
     */
    public static void createHistogram(List<Integer> energyCosts) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int[] costCount = new int[7];  // For energy costs 0 to 6

        for (int cost : energyCosts) {
            if (cost >= 0 && cost <= 6) {
                costCount[cost]++;
            }
        }

        for (int i = 0; i <= 6; i++) {
            dataset.addValue(costCount[i], "Energy Cost", Integer.toString(i));
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Energy Cost Distribution",
                "Energy Cost", "Number of Cards",
                dataset, PlotOrientation.VERTICAL,
                false, true, false);

        int width = 640;
        int height = 480;
        BufferedImage chartImage = barChart.createBufferedImage(width, height);
        File chartFile = new File("energy_histogram.png");
        ImageIO.write(chartImage, "png", chartFile);
    }

    /**
     * Generates a PDF report with the specified parameters
     *
     * @param deckId      The unique ID of the deck
     * @param totalCost   The total energy cost of the deck
     * @param invalidCards The list of invalid card entries
     * @param isVoid      Flag indicating whether the report should be void
     */
    public static void generatePdfReport(String deckId, int totalCost, List<String> invalidCards, boolean isVoid) {
        String fileName = "SpireDeck " + deckId + (isVoid ? "(VOID)" : "") + ".pdf";
        try {
            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Add title
            document.add(new Paragraph("Slay the Spire Deck Energy Cost Report").setBold().setFontSize(16));

            // Add deck ID
            document.add(new Paragraph("Deck ID: " + deckId).setFontSize(12));

            // Add total energy cost
            document.add(new Paragraph("Total Energy Cost: " + totalCost + " energy").setFontSize(12));

            // Add histogram image
            Image histogram = new Image(ImageDataFactory.create("energy_histogram.png"));
            document.add(histogram);

            // Add invalid cards if not void
            if (!isVoid && !invalidCards.isEmpty()) {
                document.add(new Paragraph("Invalid Cards:").setBold().setFontSize(12));
                for (String invalidCard : invalidCards) {
                    document.add(new Paragraph(invalidCard).setFontSize(12));
                }
            }

            document.close();
            System.out.println("PDF Report generated: " + fileName);
        } catch (Exception e) {
            System.out.println("Error generating PDF: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the deck file name: ");
        String fileName = scanner.nextLine();

        // Generate a unique deck ID
        String deckId = generateDeckId();

        // Read the deck and get energy costs
        List<Integer> energyCosts = readDeck(fileName);
        List<String> invalidCards = new ArrayList<>();

        // Check for invalid cards
        if (energyCosts.isEmpty()) {
            System.out.println("No valid data found in the file.");
            return;
        }

        // Calculate total energy cost
        int totalCost = totalEnergyCost(energyCosts);

        // Check for void conditions
        if (energyCosts.size() > MAX_CARDS || invalidCards.size() > MAX_INVALID_CARDS) {
            generatePdfReport(deckId, totalCost, invalidCards, true);
            return;
        }

        // Create the histogram
        try {
            createHistogram(energyCosts);
        } catch (IOException e) {
            System.out.println("Error creating histogram: " + e.getMessage());
        }

        // Generate the PDF report
        generatePdfReport(deckId, totalCost, invalidCards, false);
    }
}
