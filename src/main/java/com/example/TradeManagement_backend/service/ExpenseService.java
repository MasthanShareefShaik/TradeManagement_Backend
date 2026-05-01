// ExpenseService.java (Updated with Professional PDF Styling)

package com.example.TradeManagement_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.TradeManagement_backend.entity.ExpenseEntry;
import com.example.TradeManagement_backend.respository.ExpenseRepository;
import com.example.TradeManagement_backend.utils.TransactionType;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.Font.FontFamily;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository repo;
    private final AccountBalanceService balanceService;

    // Colors - Professional Color Palette
    private static final BaseColor PRIMARY_COLOR = new BaseColor(25, 118, 210); // Material Blue
    private static final BaseColor SECONDARY_COLOR = new BaseColor(97, 97, 97); // Gray 700
    private static final BaseColor SUCCESS_COLOR = new BaseColor(46, 125, 50); // Green 800
    private static final BaseColor DANGER_COLOR = new BaseColor(198, 40, 40); // Red 700
    private static final BaseColor HEADER_BG = new BaseColor(13, 71, 161); // Dark Blue 900
    private static final BaseColor TABLE_HEADER_BG = new BaseColor(25, 118, 210); // Primary Blue
    private static final BaseColor ROW_STRIPE = new BaseColor(245, 245, 245); // Light Gray 100
    private static final BaseColor BORDER_COLOR = new BaseColor(224, 224, 224); // Gray 300
    private static final BaseColor CARD_BORDER = new BaseColor(189, 189, 189); // Gray 400
    private static final BaseColor WARNING_COLOR = new BaseColor(245, 124, 0); // Orange 700
    private static final BaseColor SIGNATURE_COLOR = new BaseColor(33, 33, 33); // Gray 900

    public ExpenseEntry add(ExpenseEntry entry) {
        entry.setTimestamp(LocalDateTime.now());

        double amount = entry.getAmount();

        if (entry.getType() == TransactionType.ADD) {
            balanceService.applyChange(amount); 
        } else {
            balanceService.applyChange(-amount); 
        }

        return repo.save(entry);
    }

    public List<ExpenseEntry> getAll() {
        return repo.findAll();
    }

    public ExpenseEntry update(Long id, ExpenseEntry entry) {
        ExpenseEntry existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        double oldAmount = existing.getType() == TransactionType.ADD
                ? existing.getAmount()
                : -existing.getAmount();

        double newAmount = entry.getType() == TransactionType.ADD
                ? entry.getAmount()
                : -entry.getAmount();

        double diff = newAmount - oldAmount;

        existing.setPersonName(entry.getPersonName());
        existing.setAmount(entry.getAmount());
        existing.setType(entry.getType());
        existing.setDescription(entry.getDescription());
        existing.setDate(entry.getDate());

        ExpenseEntry saved = repo.save(existing);

        balanceService.applyChange(diff);

        return saved;
    }

    public void delete(Long id) {
        ExpenseEntry entry = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        double amount = entry.getType() == TransactionType.ADD
                ? entry.getAmount()
                : -entry.getAmount();

        balanceService.applyChange(-amount); 

        repo.deleteById(id);
    }

    public List<ExpenseEntry> getByDate(LocalDate date) {
        return repo.findByDate(date);
    }

    public List<ExpenseEntry> getByPerson(String name) {
        return repo.findByPersonNameIgnoreCase(name);
    }

    // Get summary statistics for PDF
    private Map<String, Object> getSummaryStats(List<ExpenseEntry> entries) {
        double totalAdded = entries.stream()
                .filter(e -> e.getType() == TransactionType.ADD)
                .mapToDouble(ExpenseEntry::getAmount)
                .sum();
        
        double totalWithdrawn = entries.stream()
                .filter(e -> e.getType() == TransactionType.WITHDRAW)
                .mapToDouble(ExpenseEntry::getAmount)
                .sum();
        
        double netBalance = totalAdded - totalWithdrawn;
        
        Map<String, Double> personSummary = entries.stream()
                .collect(Collectors.groupingBy(
                    ExpenseEntry::getPersonName,
                    Collectors.summingDouble(e -> e.getType() == TransactionType.ADD ? e.getAmount() : -e.getAmount())
                ));
        
        return Map.of(
            "totalAdded", totalAdded,
            "totalWithdrawn", totalWithdrawn,
            "netBalance", netBalance,
            "totalTransactions", entries.size(),
            "totalPersons", personSummary.size(),
            "personSummary", personSummary
        );
    }

    public byte[] generatePdf() {
        List<ExpenseEntry> list = repo.findAll();
        return generatePdfWithFilters(list, null, null);
    }

    public byte[] generatePdfWithFilters(List<ExpenseEntry> entries, LocalDate startDate, LocalDate endDate) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate()); // Landscape for better table view
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            // Add metadata
            document.addTitle("Expense Report");
            document.addSubject("Transaction History");
            document.addAuthor("Trade Management System");
            document.addCreationDate();
            
            // Set margins
            document.setMargins(40, 40, 40, 60);
            
            document.open();
            
            // Add professional header with company branding
            addProfessionalHeader(document);
            
            // Add date range if specified
            if (startDate != null || endDate != null) {
                addDateRange(document, startDate, endDate);
            }
            
            // Add summary cards
            addSummaryCards(document, entries);
            
            // Add person-wise summary table
            addPersonSummaryTable(document, entries);
            
            // Add spacing
            document.newPage();
            
            // Add transaction details table
            addTransactionTable(document, entries);
            
            // Add signatures at the bottom
            addSignatureSection(document);
            
            // Add professional footer
            addProfessionalFooter(document, writer);
            
            document.close();
            return out.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("PDF generation error: " + e.getMessage(), e);
        }
    }

    private void addProfessionalHeader(Document document) throws DocumentException {
        // Main header container
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2.5f, 1.5f, 2f});
        
        // Company logo and name cell
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPaddingBottom(15);
        
        Font companyFont = new Font(FontFamily.HELVETICA, 20, Font.BOLD, HEADER_BG);
        Paragraph companyName = new Paragraph("TRADE MANAGEMENT", companyFont);
        companyName.setSpacingAfter(2);
        
        Font taglineFont = new Font(FontFamily.HELVETICA, 9, Font.NORMAL, SECONDARY_COLOR);
        Paragraph tagline = new Paragraph("Trading Statistics", taglineFont);
        tagline.setSpacingAfter(5);
        
        logoCell.addElement(companyName);
        logoCell.addElement(tagline);
        
        // Divider line
        Paragraph divider = new Paragraph("─".repeat(40), new Font(FontFamily.HELVETICA, 6, Font.NORMAL, BORDER_COLOR));
        logoCell.addElement(divider);
        
        // Document info cell
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        infoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoCell.setPaddingBottom(15);
        
        Font docTypeFont = new Font(FontFamily.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR);
        Paragraph docType = new Paragraph("EXPENSE REPORT", docTypeFont);
        docType.setSpacingAfter(10);
        
        
        // Date cell
        PdfPCell dateCell = new PdfPCell();
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.setPaddingBottom(15);
        
        Font dateLabelFont = new Font(FontFamily.HELVETICA, 8, Font.NORMAL, SECONDARY_COLOR);
        Font dateValueFont = new Font(FontFamily.HELVETICA, 10, Font.BOLD, PRIMARY_COLOR);
        
        Paragraph generatedLabel = new Paragraph("Generated Date", dateLabelFont);
        generatedLabel.setSpacingAfter(2);
        Paragraph generatedDate = new Paragraph(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), dateValueFont);
        generatedDate.setSpacingAfter(8);
        
        Paragraph timeLabel = new Paragraph("Time", dateLabelFont);
        timeLabel.setSpacingAfter(2);
        Paragraph timeValue = new Paragraph(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")), new Font(FontFamily.HELVETICA, 10, Font.NORMAL, SECONDARY_COLOR));
        
        dateCell.addElement(generatedLabel);
        dateCell.addElement(generatedDate);
        dateCell.addElement(timeLabel);
        dateCell.addElement(timeValue);
        
        headerTable.addCell(logoCell);
        headerTable.addCell(infoCell);
        headerTable.addCell(dateCell);
        
        document.add(headerTable);
        
        // Full width separator line
        PdfPTable separatorTable = new PdfPTable(1);
        separatorTable.setWidthPercentage(100);
        PdfPCell separator = new PdfPCell();
        separator.setBorder(Rectangle.BOTTOM);
        separator.setBorderWidth(2f);
        separator.setBorderColor(PRIMARY_COLOR);
        separator.setFixedHeight(1);
        separatorTable.addCell(separator);
        document.add(separatorTable);
        document.add(new Paragraph(" "));
    }

    private void addDateRange(Document document, LocalDate startDate, LocalDate endDate) throws DocumentException {
        PdfPTable dateTable = new PdfPTable(1);
        dateTable.setWidthPercentage(100);
        
        PdfPCell dateCell = new PdfPCell();
        dateCell.setBackgroundColor(new BaseColor(232, 245, 253)); // Light blue background
        dateCell.setPadding(10);
        dateCell.setBorder(Rectangle.BOX);
        dateCell.setBorderColor(PRIMARY_COLOR);
        dateCell.setBorderWidth(1f);
        
        Font dateLabelFont = new Font(FontFamily.HELVETICA, 9, Font.BOLD, SECONDARY_COLOR);
        Font dateValueFont = new Font(FontFamily.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR);
        
        Paragraph datePara = new Paragraph();
        datePara.add(new Chunk("Date Range: ", dateLabelFont));
        
        if (startDate != null && endDate != null) {
            datePara.add(new Chunk(
                startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + " to " + 
                endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), 
                dateValueFont
            ));
        } else if (startDate != null) {
            datePara.add(new Chunk(
                "From " + startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), 
                dateValueFont
            ));
        } else if (endDate != null) {
            datePara.add(new Chunk(
                "Until " + endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), 
                dateValueFont
            ));
        } else {
            datePara.add(new Chunk("All Transactions", dateValueFont));
        }
        
        dateCell.addElement(datePara);
        dateTable.addCell(dateCell);
        document.add(dateTable);
        document.add(new Paragraph(" "));
    }

    private void addSummaryCards(Document document, List<ExpenseEntry> entries) throws DocumentException {
        Map<String, Object> stats = getSummaryStats(entries);
        
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);
        summaryTable.setSpacingAfter(15);
        summaryTable.setWidths(new float[]{1f, 1f, 1f, 1f});
        
        // Card 1: Total Added
        PdfPCell addedCell = createSummaryCard(
            "Total Credits",
            "₹" + String.format("%,.2f", (double) stats.get("totalAdded")),
            "▲",
            SUCCESS_COLOR,
            new BaseColor(232, 245, 233) // Light green
        );
        summaryTable.addCell(addedCell);
        
        // Card 2: Total Withdrawn
        PdfPCell withdrawnCell = createSummaryCard(
            "Total Debits",
            "₹" + String.format("%,.2f", (double) stats.get("totalWithdrawn")),
            "▼",
            DANGER_COLOR,
            new BaseColor(255, 235, 238) // Light red
        );
        summaryTable.addCell(withdrawnCell);
        
        // Card 3: Net Balance
        double netBalance = (double) stats.get("netBalance");
        BaseColor netColor = netBalance >= 0 ? SUCCESS_COLOR : DANGER_COLOR;
        BaseColor netBg = netBalance >= 0 ? new BaseColor(232, 245, 233) : new BaseColor(255, 235, 238);
        String netIcon = netBalance >= 0 ? "●" : "○";
        PdfPCell netCell = createSummaryCard(
            "Net Balance",
            "₹" + String.format("%,.2f", netBalance),
            netIcon,
            netColor,
            netBg
        );
        summaryTable.addCell(netCell);
        
        // Card 4: Statistics
        PdfPCell statsCell = new PdfPCell();
        statsCell.setBackgroundColor(new BaseColor(227, 242, 253)); // Light blue
        statsCell.setPadding(12);
        statsCell.setBorder(Rectangle.BOX);
        statsCell.setBorderColor(CARD_BORDER);
        statsCell.setBorderWidth(0.5f);
        
        Font statsTitleFont = new Font(FontFamily.HELVETICA, 9, Font.BOLD, SECONDARY_COLOR);
        Font statsValueFont = new Font(FontFamily.HELVETICA, 18, Font.BOLD, PRIMARY_COLOR);
        Font statsUnitFont = new Font(FontFamily.HELVETICA, 8, Font.NORMAL, SECONDARY_COLOR);
        
        Paragraph statsPara = new Paragraph();
        statsPara.add(new Chunk("Transactions", statsTitleFont));
        statsPara.add(Chunk.NEWLINE);
        statsPara.add(new Chunk(String.valueOf(stats.get("totalTransactions")), statsValueFont));
        statsPara.add(new Chunk("  entries", statsUnitFont));
        statsPara.add(Chunk.NEWLINE);
        statsPara.add(Chunk.NEWLINE);
        statsPara.add(new Chunk("Participants", statsTitleFont));
        statsPara.add(Chunk.NEWLINE);
        statsPara.add(new Chunk(String.valueOf(stats.get("totalPersons")), statsValueFont));
        statsPara.add(new Chunk("  persons", statsUnitFont));
        
        statsCell.addElement(statsPara);
        summaryTable.addCell(statsCell);
        
        document.add(summaryTable);
    }

    private PdfPCell createSummaryCard(String title, String value, String icon, BaseColor valueColor, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setPadding(12);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(CARD_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Font titleFont = new Font(FontFamily.HELVETICA, 9, Font.BOLD, SECONDARY_COLOR);
        Font valueFont = new Font(FontFamily.HELVETICA, 16, Font.BOLD, valueColor);
        Font iconFont = new Font(FontFamily.HELVETICA, 14, Font.NORMAL, valueColor);
        
        Paragraph para = new Paragraph();
        para.add(new Chunk(icon + " ", iconFont));
        para.add(new Chunk(title, titleFont));
        para.add(Chunk.NEWLINE);
        para.add(new Chunk(value, valueFont));
        
        cell.addElement(para);
        return cell;
    }

    private void addPersonSummaryTable(Document document, List<ExpenseEntry> entries) throws DocumentException {
        Map<String, Object> stats = getSummaryStats(entries);
        Map<String, Double> personSummary = (Map<String, Double>) stats.get("personSummary");
        
        if (personSummary.isEmpty()) {
            return;
        }
        
        // Section title with icon
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(15);
        titleTable.setSpacingAfter(10);
        
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingBottom(5);
        
        Font sectionFont = new Font(FontFamily.HELVETICA, 14, Font.BOLD, HEADER_BG);
        Paragraph sectionTitle = new Paragraph("👥 Person-wise Summary", sectionFont);
        titleCell.addElement(sectionTitle);
        titleTable.addCell(titleCell);
        document.add(titleTable);
        
        // Summary table
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 2f, 1.5f});
        table.setSpacingBefore(5);
        
        // Table header
        addPersonSummaryHeader(table);
        
        // Table body with alternating row colors
        int rowIndex = 0;
        for (Map.Entry<String, Double> entry : personSummary.entrySet()) {

            double balance = entry.getValue();

            // Person Name (LEFT aligned)
            PdfPCell nameCell = new PdfPCell(new Phrase(
                entry.getKey(),
                new Font(FontFamily.HELVETICA, 10, Font.BOLD)
            ));
            nameCell.setPadding(8);
            nameCell.setBorder(Rectangle.BOTTOM);
            nameCell.setBorderColor(BORDER_COLOR);
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nameCell.setHorizontalAlignment(Element.ALIGN_LEFT); // ✅ FIX

            // Net Balance (RIGHT aligned)
            Font balanceFont = new Font(
                FontFamily.HELVETICA,
                10,
                Font.BOLD,
                balance >= 0 ? SUCCESS_COLOR : DANGER_COLOR
            );

            PdfPCell balanceCell = new PdfPCell(new Phrase(
                String.format("₹%,.2f", balance),
                balanceFont
            ));
            balanceCell.setPadding(8);
            balanceCell.setBorder(Rectangle.BOTTOM);
            balanceCell.setBorderColor(BORDER_COLOR);
            balanceCell.setHorizontalAlignment(Element.ALIGN_RIGHT); // ✅ FIX
            balanceCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            // Status (CENTER aligned)
            String status = balance >= 0 ? "Active" : "Due";
            BaseColor statusColor = balance >= 0 ? SUCCESS_COLOR : DANGER_COLOR;

            PdfPCell statusCell = new PdfPCell(new Phrase(
                status,
                new Font(FontFamily.HELVETICA, 9, Font.BOLD, statusColor)
            ));
            statusCell.setPadding(8);
            statusCell.setBorder(Rectangle.BOTTOM);
            statusCell.setBorderColor(BORDER_COLOR);
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER); // ✅ FIX
            statusCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            // Alternate row color
            if (rowIndex % 2 == 0) {
                nameCell.setBackgroundColor(ROW_STRIPE);
                balanceCell.setBackgroundColor(ROW_STRIPE);
                statusCell.setBackgroundColor(ROW_STRIPE);
            }

            table.addCell(nameCell);
            table.addCell(balanceCell);
            table.addCell(statusCell);

            rowIndex++;
        }
        
        document.add(table);
    }

    private void addTransactionTable(Document document, List<ExpenseEntry> entries) throws DocumentException {
        if (entries.isEmpty()) {
            // Empty state with icon
            PdfPTable emptyTable = new PdfPTable(1);
            emptyTable.setWidthPercentage(100);
            emptyTable.setSpacingBefore(20);
            emptyTable.setSpacingAfter(20);
            
            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setBorder(Rectangle.NO_BORDER);
            emptyCell.setPadding(30);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            Font emptyIconFont = new Font(FontFamily.HELVETICA, 48, Font.NORMAL, BORDER_COLOR);
            Font emptyFont = new Font(FontFamily.HELVETICA, 12, Font.ITALIC, SECONDARY_COLOR);
            
            Paragraph emptyPara = new Paragraph();
            emptyPara.add(new Chunk("📄\n\n", emptyIconFont));
            emptyPara.add(new Chunk("No transactions found", emptyFont));
            emptyPara.setAlignment(Element.ALIGN_CENTER);
            
            emptyCell.addElement(emptyPara);
            emptyTable.addCell(emptyCell);
            document.add(emptyTable);
            return;
        }
        
        // Section title with icon
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(15);
        titleTable.setSpacingAfter(10);
        
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingBottom(5);
        
        Font sectionFont = new Font(FontFamily.HELVETICA, 14, Font.BOLD, HEADER_BG);
        Paragraph sectionTitle = new Paragraph("📋 Transaction Details", sectionFont);
        titleCell.addElement(sectionTitle);
        titleTable.addCell(titleCell);
        document.add(titleTable);
        
        // Create table with 7 columns (added ID column)
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 1.5f, 1f, 1.2f, 1.8f, 1.2f, 1f});
        table.setSpacingBefore(5);
        
        // Add table header
        addTableHeader(table, new String[]{"SNO", "Person Name", "Type", "Amount", "Description", "Date", "Time"});
        
        // Add data rows
        int rowNumber = 1;
        int rowIndex = 0;
        
        for (ExpenseEntry entry : entries) {
            // Serial number
            PdfPCell serialCell = createTableCell(String.format("%01d", rowNumber++), Font.NORMAL);
            serialCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            // Person name
            PdfPCell nameCell = createTableCell(entry.getPersonName(), Font.BOLD);
            
            // Transaction type with badge
            PdfPCell typeCell = new PdfPCell();
            typeCell.setPadding(6);
            typeCell.setBorder(Rectangle.BOTTOM);
            typeCell.setBorderColor(BORDER_COLOR);
            typeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            typeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            String typeText = entry.getType() == TransactionType.ADD ? "CREDIT" : "DEBIT";
            BaseColor typeColor = entry.getType() == TransactionType.ADD ? SUCCESS_COLOR : DANGER_COLOR;
            BaseColor typeBg = entry.getType() == TransactionType.ADD ? new BaseColor(232, 245, 233) : new BaseColor(255, 235, 238);
            
            // Create badge effect
            PdfPTable badgeTable = new PdfPTable(1);
            badgeTable.setWidthPercentage(80);
            PdfPCell badgeCell = new PdfPCell(new Phrase(typeText, new Font(FontFamily.HELVETICA, 8, Font.BOLD, typeColor)));
            badgeCell.setBackgroundColor(typeBg);
            badgeCell.setBorder(Rectangle.BOX);
            badgeCell.setBorderColor(typeColor);
            badgeCell.setBorderWidth(0.5f);
            badgeCell.setPadding(3);
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeTable.addCell(badgeCell);
            
            typeCell.addElement(badgeTable);
            
            // Amount with +/- sign
            String amountText = (entry.getType() == TransactionType.ADD ? "+" : "-") + 
                               String.format("₹%,.2f", entry.getAmount());
            BaseColor amountColor = entry.getType() == TransactionType.ADD ? SUCCESS_COLOR : DANGER_COLOR;
            PdfPCell amountCell = createTableCell(amountText, Font.BOLD, amountColor);
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            // Description
            String description = entry.getDescription() != null && !entry.getDescription().isEmpty() ? 
                                entry.getDescription() : "—";
            PdfPCell descCell = createTableCell(description, Font.NORMAL);
            
            // Date
            String dateStr = entry.getDate() != null ? 
                            entry.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
            PdfPCell dateCell = createTableCell(dateStr, Font.NORMAL);
            dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            // Time (from timestamp)
            String timeStr = entry.getTimestamp() != null ? 
                            entry.getTimestamp().format(DateTimeFormatter.ofPattern("hh:mm a")) : "—";
            PdfPCell timeCell = createTableCell(timeStr, Font.NORMAL);
            timeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            // Apply alternating row background
            if (rowIndex % 2 == 0) {
                BaseColor stripeColor = new BaseColor(250, 250, 250);
                serialCell.setBackgroundColor(stripeColor);
                nameCell.setBackgroundColor(stripeColor);
                amountCell.setBackgroundColor(stripeColor);
                descCell.setBackgroundColor(stripeColor);
                dateCell.setBackgroundColor(stripeColor);
                timeCell.setBackgroundColor(stripeColor);
            }
            
            table.addCell(serialCell);
            table.addCell(nameCell);
            table.addCell(typeCell);
            table.addCell(amountCell);
            table.addCell(descCell);
            table.addCell(dateCell);
            table.addCell(timeCell);
            
            rowIndex++;
        }
        
        document.add(table);
        
        // Add summary footer
        addTableFooter(document, entries);
    }
    private void addPersonSummaryHeader(PdfPTable table) {

        String[] headers = {"Person Name", "Net Balance", "Status"};

        for (int i = 0; i < headers.length; i++) {

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(HEADER_BG);
            headerCell.setPadding(10);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Font headerFont = new Font(FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
            Paragraph para = new Paragraph(headers[i], headerFont);

            // ✅ Match EXACT alignment like body
            switch (i) {
                case 0: para.setAlignment(Element.ALIGN_LEFT); break;   // Person
                case 1: para.setAlignment(Element.ALIGN_RIGHT); break;  // Balance
                case 2: para.setAlignment(Element.ALIGN_CENTER); break; // Status
            }

            headerCell.addElement(para);
            table.addCell(headerCell);
        }
    }
    private void addTableHeader(PdfPTable table, String[] headers) {
        for (int i = 0; i < headers.length; i++) {

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(HEADER_BG);
            headerCell.setPadding(10);
            headerCell.setBorder(Rectangle.NO_BORDER);
            headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Font headerFont = new Font(FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
            Paragraph headerPara = new Paragraph(headers[i], headerFont);

            switch (i) {
                case 0: // #
                    headerPara.setAlignment(Element.ALIGN_CENTER);
                    break;
                case 1: // Person Name
                    headerPara.setAlignment(Element.ALIGN_LEFT);
                    break;
                case 2: // Type
                    headerPara.setAlignment(Element.ALIGN_CENTER);
                    break;
                case 3: // Amount
                    headerPara.setAlignment(Element.ALIGN_RIGHT);
                    break;
                case 4: // Description
                    headerPara.setAlignment(Element.ALIGN_LEFT);
                    break;
                case 5: // Date
                case 6: // Time
                    headerPara.setAlignment(Element.ALIGN_CENTER);
                    break;
                default:
                    headerPara.setAlignment(Element.ALIGN_CENTER);
            }

            headerCell.addElement(headerPara);
            table.addCell(headerCell);
        }
    }

    private PdfPCell createTableCell(String text, int fontStyle) {
        return createTableCell(text, fontStyle, new BaseColor(66, 66, 66)); // Dark gray text
    }

    private PdfPCell createTableCell(String text, int fontStyle, BaseColor color) {
        Font font = new Font(FontFamily.HELVETICA, 9, fontStyle, color);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER_COLOR);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private void addTableFooter(Document document, List<ExpenseEntry> entries) throws DocumentException {
        double totalAdded = entries.stream()
                .filter(e -> e.getType() == TransactionType.ADD)
                .mapToDouble(ExpenseEntry::getAmount)
                .sum();
        
        double totalWithdrawn = entries.stream()
                .filter(e -> e.getType() == TransactionType.WITHDRAW)
                .mapToDouble(ExpenseEntry::getAmount)
                .sum();
        
        double netTotal = totalAdded - totalWithdrawn;
        
        // Summary box
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(50);
        footerTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footerTable.setSpacingBefore(15);
        
        // Border and background
        PdfPCell containerCell = new PdfPCell();
        containerCell.setColspan(2);
        containerCell.setBackgroundColor(new BaseColor(248, 249, 250));
        containerCell.setBorder(Rectangle.BOX);
        containerCell.setBorderColor(CARD_BORDER);
        containerCell.setBorderWidth(1f);
        containerCell.setPadding(12);
        
        PdfPTable innerTable = new PdfPTable(2);
        innerTable.setWidthPercentage(100);
        innerTable.setWidths(new float[]{1.5f, 1f});
        
        Font labelFont = new Font(FontFamily.HELVETICA, 9, Font.BOLD);
        Font valueFont = new Font(FontFamily.HELVETICA, 9, Font.BOLD);
        
        // Total Credits
        PdfPCell creditLabel = new PdfPCell(new Phrase("Total Credits:", labelFont));
        creditLabel.setBorder(Rectangle.NO_BORDER);
        creditLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        creditLabel.setPadding(4);
        
        PdfPCell creditValue = new PdfPCell(new Phrase(String.format("+₹%,.2f", totalAdded), 
            new Font(FontFamily.HELVETICA, 9, Font.BOLD, SUCCESS_COLOR)));
        creditValue.setBorder(Rectangle.NO_BORDER);
        creditValue.setPadding(4);
        
        // Total Debits
        PdfPCell debitLabel = new PdfPCell(new Phrase("Total Debits:", labelFont));
        debitLabel.setBorder(Rectangle.NO_BORDER);
        debitLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        debitLabel.setPadding(4);
        
        PdfPCell debitValue = new PdfPCell(new Phrase(String.format("-₹%,.2f", totalWithdrawn), 
            new Font(FontFamily.HELVETICA, 9, Font.BOLD, DANGER_COLOR)));
        debitValue.setBorder(Rectangle.NO_BORDER);
        debitValue.setPadding(4);
        
        // Net Total
        PdfPCell netLabel = new PdfPCell(new Phrase("Net Balance:", new Font(FontFamily.HELVETICA, 10, Font.BOLD)));
        netLabel.setBorder(Rectangle.TOP);
        netLabel.setBorderColor(BORDER_COLOR);
        netLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netLabel.setPadding(4);
        netLabel.setPaddingTop(8);
        
        BaseColor netColor = netTotal >= 0 ? SUCCESS_COLOR : DANGER_COLOR;
        PdfPCell netValue = new PdfPCell(new Phrase(String.format("₹%,.2f", netTotal), 
            new Font(FontFamily.HELVETICA, 10, Font.BOLD, netColor)));
        netValue.setBorder(Rectangle.TOP);
        netValue.setBorderColor(BORDER_COLOR);
        netValue.setPadding(4);
        netValue.setPaddingTop(8);
        
        innerTable.addCell(creditLabel);
        innerTable.addCell(creditValue);
        innerTable.addCell(debitLabel);
        innerTable.addCell(debitValue);
        innerTable.addCell(netLabel);
        innerTable.addCell(netValue);
        
        containerCell.addElement(innerTable);
        footerTable.addCell(containerCell);
        
        document.add(footerTable);
    }

    private void addSignatureSection(Document document) throws DocumentException {

        // Minimal spacing before signatures
        document.add(new Paragraph(" "));

        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(100);
        signatureTable.setWidths(new float[]{1f, 1f});
        signatureTable.setSpacingBefore(5); 

        // Left side
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(5);

        Font labelFont = new Font(FontFamily.HELVETICA, 9, Font.NORMAL, SECONDARY_COLOR);
        Paragraph authorizedBy = new Paragraph("Authorized By:", labelFont);
        authorizedBy.setSpacingAfter(5); 
        leftCell.addElement(authorizedBy);

        // Right side
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(5);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        // Signature 1
        PdfPTable sig1Table = createSignatureBlock("Masthan Shareef", "Founder & Director");
        rightCell.addElement(sig1Table);

        // Small spacing between signatures
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(5); 
        rightCell.addElement(spacer);

        // Signature 2
        PdfPTable sig2Table = createSignatureBlock("Saketh Ram", "Founder & Managing Director");
        rightCell.addElement(sig2Table);

        signatureTable.addCell(leftCell);
        signatureTable.addCell(rightCell);

        // Thin line above signatures
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.TOP);
        lineCell.setBorderWidth(0.5f); 
        lineCell.setBorderColor(BORDER_COLOR);
        lineCell.setFixedHeight(1);
        lineCell.setPadding(0);
        lineTable.addCell(lineCell);

        document.add(lineTable);
        document.add(signatureTable);
    }

    private PdfPTable createSignatureBlock(String name, String designation) {
        PdfPTable sigTable = new PdfPTable(1);
        sigTable.setWidthPercentage(40);
        sigTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        PdfPCell sigCell = new PdfPCell();
        sigCell.setBorder(Rectangle.NO_BORDER);
        sigCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        sigCell.setPadding(5);
        
        // Signature line
        Font signatureFont = new Font(FontFamily.HELVETICA, 12, Font.ITALIC, SIGNATURE_COLOR);
        Paragraph signature = new Paragraph(name, signatureFont);
        signature.setSpacingAfter(2);
        sigCell.addElement(signature);
        
        // Line under signature
        Paragraph line = new Paragraph("─".repeat(25), new Font(FontFamily.HELVETICA, 8, Font.NORMAL, BORDER_COLOR));
        line.setSpacingAfter(2);
        sigCell.addElement(line);
        
        // Designation
        Font desigFont = new Font(FontFamily.HELVETICA, 8, Font.NORMAL, SECONDARY_COLOR);
        Paragraph desig = new Paragraph(designation, desigFont);
        sigCell.addElement(desig);
        
        sigTable.addCell(sigCell);
        return sigTable;
    }

    private void addProfessionalFooter(Document document, PdfWriter writer) throws DocumentException {
        // Footer content
        PdfPTable footer = new PdfPTable(3);
        footer.setWidthPercentage(100);
        footer.setTotalWidth(document.right() - document.left());
        footer.setLockedWidth(true);
        footer.getDefaultCell().setFixedHeight(20);
        footer.getDefaultCell().setBorder(Rectangle.TOP);
        footer.getDefaultCell().setBorderColor(BORDER_COLOR);
        footer.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Font footerFont = new Font(FontFamily.HELVETICA, 8, Font.NORMAL, SECONDARY_COLOR);

        
        // Center: Company name
        PdfPCell centerFooter = new PdfPCell(new Phrase("© 2026 Trade Management System", footerFont));
        centerFooter.setBorder(Rectangle.TOP);
        centerFooter.setBorderColor(BORDER_COLOR);
        centerFooter.setHorizontalAlignment(Element.ALIGN_CENTER);
        centerFooter.setPadding(8);
        
        // Right: Page number
        PdfPCell rightFooter = new PdfPCell(new Phrase("Page " + writer.getPageNumber(), footerFont));
        rightFooter.setBorder(Rectangle.TOP);
        rightFooter.setBorderColor(BORDER_COLOR);
        rightFooter.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightFooter.setPadding(8);
        
       // footer.addCell(leftFooter);
        footer.addCell(centerFooter);
        footer.addCell(rightFooter);
        
        // Position footer at the bottom of the page
        footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 10, writer.getDirectContent());
    }
}