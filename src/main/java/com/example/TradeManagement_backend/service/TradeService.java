package com.example.TradeManagement_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.example.TradeManagement_backend.entity.AccountBalance;
import com.example.TradeManagement_backend.entity.TradeEntry;
import com.example.TradeManagement_backend.respository.TradeRepository;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final AccountBalanceService balanceService;

    public TradeEntry addTrade(TradeEntry trade) {
        trade.setTimestamp(LocalDateTime.now()); 
        balanceService.applyChange(trade.getProfitLossAmount());
        return tradeRepository.save(trade);
    }

    public List<TradeEntry> getAllTrades() {
        return tradeRepository.findAll();
    }

    public TradeEntry updateTrade(Long id, TradeEntry trade) {

        TradeEntry existing = tradeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trade not found"));

        double oldPL = existing.getProfitLossAmount();
        double newPL = trade.getProfitLossAmount();
        double diff = newPL - oldPL;

        existing.setStockName(trade.getStockName());
        existing.setLots(trade.getLots());
        existing.setEntryTrade(trade.getEntryTrade());
        existing.setExitTrade(trade.getExitTrade());
        existing.setProfitLossAmount(newPL);
        existing.setResultStatus(trade.getResultStatus());

        TradeEntry saved = tradeRepository.save(existing);

        balanceService.applyChange(diff);

        return saved;
    }

    public void deleteTrade(Long id) {
        TradeEntry trade = tradeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trade not found"));

        balanceService.applyChange(-trade.getProfitLossAmount());

        tradeRepository.deleteById(id);
    }
    
    public List<TradeEntry> getTradesByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);
        return tradeRepository.findByTimestampBetween(start, end);
    }

    public byte[] generateTradesPdf(LocalDate date) {
        
        List<TradeEntry> trades;

        if (date != null) {
            trades = getTradesByDate(date);
        } else {
            trades = getAllTrades();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate()); // Landscape for better table view
            
            // Set margins
            document.setMargins(40, 40, 40, 60);

            // Professional Color Palette
            Color PRIMARY_COLOR = new DeviceRgb(25, 118, 210); // Material Blue
            Color SECONDARY_COLOR = new DeviceRgb(97, 97, 97); // Gray 700
            Color SUCCESS_COLOR = new DeviceRgb(46, 125, 50); // Green 800
            Color DANGER_COLOR = new DeviceRgb(198, 40, 40); // Red 700
            Color HEADER_BG = new DeviceRgb(13, 71, 161); // Dark Blue 900
            Color CARD_BORDER = new DeviceRgb(189, 189, 189); // Gray 400
            Color TEXT_COLOR = new DeviceRgb(66, 66, 66); // Dark gray
            Color LIGHT_GRAY_BG = new DeviceRgb(245, 245, 245); // Light gray for stripes
            Color SUCCESS_BG = new DeviceRgb(232, 245, 233); // Light green
            Color DANGER_BG = new DeviceRgb(255, 235, 238); // Light red
            Color LIGHT_BLUE_BG = new DeviceRgb(227, 242, 253); // Light blue
            Color BORDER_COLOR = new DeviceRgb(224, 224, 224); // Gray 300
            
            // Calculate statistics
            double totalPL = trades.stream()
                    .mapToDouble(TradeEntry::getProfitLossAmount)
                    .sum();

            long winTrades = trades.stream()
                    .filter(t -> t.getProfitLossAmount() > 0)
                    .count();
            
            long lossTrades = trades.stream()
                    .filter(t -> t.getProfitLossAmount() < 0)
                    .count();

            double winRate = trades.size() > 0
                    ? (winTrades * 100.0 / trades.size())
                    : 0;
            
            // Get current account balance
            AccountBalance balance = balanceService.getBalance();
            double currentBalance = balance.getBalance();
            // Add Professional Header
            addTradeReportHeader(document, date, PRIMARY_COLOR, SECONDARY_COLOR, HEADER_BG);
            
            addTradeSummaryCards(document, totalPL, winTrades, lossTrades, winRate, 
                               currentBalance, PRIMARY_COLOR, SECONDARY_COLOR, 
                               SUCCESS_COLOR, DANGER_COLOR, CARD_BORDER, SUCCESS_BG, DANGER_BG, 
                               LIGHT_BLUE_BG);
            
            document.add(new Paragraph("\n"));
            
            addTradeDetailsTable(document, trades, SECONDARY_COLOR, SUCCESS_COLOR, 
                               DANGER_COLOR, HEADER_BG, TEXT_COLOR, LIGHT_GRAY_BG, BORDER_COLOR, 
                               SUCCESS_BG, DANGER_BG);
            
            // Add signature section
            addTradeSignatureSection(document, SECONDARY_COLOR, BORDER_COLOR);
            
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private void addTradeReportHeader(Document document, LocalDate date, Color PRIMARY_COLOR, 
                                       Color SECONDARY_COLOR, Color HEADER_BG) throws IOException {
        // Main header container
        Table headerTable = new Table(new float[]{2.5f, 1.5f, 2f});
        headerTable.setWidth(UnitValue.createPercentValue(100));
        
        // Company logo and name cell
        Cell logoCell = new Cell();
        logoCell.setBorder(Border.NO_BORDER);
        logoCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        logoCell.setPaddingBottom(15);
        
        Paragraph companyName = new Paragraph("TRADE MANAGEMENT")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(20)
                .setFontColor(HEADER_BG);
        
        Paragraph tagline = new Paragraph(" Trading statistics")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(9)
                .setFontColor(SECONDARY_COLOR)
                .setMarginTop(2);
        
        logoCell.add(companyName);
        logoCell.add(tagline);
        
        // Document info cell
        Cell infoCell = new Cell();
        infoCell.setBorder(Border.NO_BORDER);
        infoCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        infoCell.setTextAlignment(TextAlignment.CENTER);
        infoCell.setPaddingBottom(15);
        
        Paragraph docType = new Paragraph("TRADING REPORT")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(10);
        
        infoCell.add(docType);
        
        // Date cell
        Cell dateCell = new Cell();
        dateCell.setBorder(Border.NO_BORDER);
        dateCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        dateCell.setTextAlignment(TextAlignment.RIGHT);
        dateCell.setPaddingBottom(15);
        
        Paragraph generatedLabel = new Paragraph("Generated Date")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(2);
        
        Paragraph generatedDate = new Paragraph(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(10)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(8);
        
        Paragraph timeLabel = new Paragraph("Time")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(2);
        
        Paragraph timeValue = new Paragraph(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")))
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(10)
                .setFontColor(SECONDARY_COLOR);
        
        dateCell.add(generatedLabel);
        dateCell.add(generatedDate);
        dateCell.add(timeLabel);
        dateCell.add(timeValue);
        
        headerTable.addCell(logoCell);
        headerTable.addCell(infoCell);
        headerTable.addCell(dateCell);
        
        document.add(headerTable);
        
        // Date range if specified
        if (date != null) {
            Table dateRangeTable = new Table(new float[]{1f});
            dateRangeTable.setWidth(UnitValue.createPercentValue(100));
            dateRangeTable.setMarginTop(5);
            
            Cell dateRangeCell = new Cell();
            dateRangeCell.setBackgroundColor(new DeviceRgb(232, 245, 253));
            dateRangeCell.setPadding(10);
            dateRangeCell.setBorder(new SolidBorder(PRIMARY_COLOR, 1));
            
            Paragraph datePara = new Paragraph();
            datePara.add(new Text("Date Range: ").setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(9).setFontColor(SECONDARY_COLOR));
            datePara.add(new Text(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(11).setFontColor(PRIMARY_COLOR));
            
            dateRangeCell.add(datePara);
            dateRangeTable.addCell(dateRangeCell);
            document.add(dateRangeTable);
            document.add(new Paragraph(" "));
        }
        
        // Separator line
        Table separatorTable = new Table(new float[]{1f});
        separatorTable.setWidth(UnitValue.createPercentValue(100));
        Cell separatorCell = new Cell();
        separatorCell.setBorder(new SolidBorder(PRIMARY_COLOR, 2));
        separatorCell.setHeight(1);
        separatorTable.addCell(separatorCell);
        document.add(separatorTable);
        document.add(new Paragraph(" "));
    }

    private void addTradeSummaryCards(Document document, double totalPL, 
                                       long winTrades, long lossTrades, double winRate, 
                                       double currentBalance,
                                       Color PRIMARY_COLOR, Color SECONDARY_COLOR, 
                                       Color SUCCESS_COLOR, Color DANGER_COLOR, Color CARD_BORDER,
                                       Color SUCCESS_BG, Color DANGER_BG, Color LIGHT_BLUE_BG) throws IOException {
        
        Table summaryTable = new Table(new float[]{1f, 1f, 1f, 1f});
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        summaryTable.setMarginTop(10);
        summaryTable.setMarginBottom(15);
        
        // Card 1: Total P&L
        Cell plCell = new Cell();
        plCell.setBackgroundColor(totalPL >= 0 ? SUCCESS_BG : DANGER_BG);
        plCell.setPadding(12);
        plCell.setBorder(new SolidBorder(CARD_BORDER, 0.5f));
        plCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        
        Paragraph plPara = new Paragraph();
        plPara.add(new Text("Total P&L\n").setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(9).setFontColor(SECONDARY_COLOR));
        plPara.add(new Text(String.format("Rs. %,.2f", totalPL)).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(16).setFontColor(totalPL >= 0 ? SUCCESS_COLOR : DANGER_COLOR));
        plCell.add(plPara);
        summaryTable.addCell(plCell);
        
        // Card 2: Win Rate
        Cell winRateCell = new Cell();
        winRateCell.setBackgroundColor(LIGHT_BLUE_BG);
        winRateCell.setPadding(12);
        winRateCell.setBorder(new SolidBorder(CARD_BORDER, 0.5f));
        winRateCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        
        Paragraph winRatePara = new Paragraph();
        winRatePara.add(new Text("Win Rate\n").setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(9).setFontColor(SECONDARY_COLOR));
        winRatePara.add(new Text(String.format("%.1f%%", winRate)).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(16).setFontColor(PRIMARY_COLOR));
        winRateCell.add(winRatePara);
        summaryTable.addCell(winRateCell);
        
        // Card 3: Win/Loss Count
        Cell countCell = new Cell();
        countCell.setBackgroundColor(LIGHT_BLUE_BG);
        countCell.setPadding(12);
        countCell.setBorder(new SolidBorder(CARD_BORDER, 0.5f));
        countCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        
        Paragraph countPara = new Paragraph();
        countPara.add(new Text("Win/Loss\n").setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(9).setFontColor(SECONDARY_COLOR));
        countPara.add(new Text(String.format("%dW / %dL", winTrades, lossTrades)).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(16).setFontColor(PRIMARY_COLOR));
        countCell.add(countPara);
        summaryTable.addCell(countCell);
        
        // Card 4: Current Account Balance
        Cell balanceCell = new Cell();
        balanceCell.setBackgroundColor(LIGHT_BLUE_BG);
        balanceCell.setPadding(12);
        balanceCell.setBorder(new SolidBorder(CARD_BORDER, 0.5f));
        balanceCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        
        Paragraph balancePara = new Paragraph();
        balancePara.add(new Text("Account Balance\n").setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(9).setFontColor(SECONDARY_COLOR));
        balancePara.add(new Text(String.format("Rs. %,.2f", currentBalance)).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(14).setFontColor(PRIMARY_COLOR));
        balanceCell.add(balancePara);
        summaryTable.addCell(balanceCell);
        
        document.add(summaryTable);
    }

    private void addTradeDetailsTable(Document document, List<TradeEntry> trades,
                                       Color SECONDARY_COLOR, Color SUCCESS_COLOR, Color DANGER_COLOR,
                                       Color HEADER_BG, Color TEXT_COLOR, Color LIGHT_GRAY_BG, 
                                       Color BORDER_COLOR, Color SUCCESS_BG, Color DANGER_BG) throws IOException {
        
        if (trades.isEmpty()) {
            // Empty state
            Table emptyTable = new Table(new float[]{1f});
            emptyTable.setWidth(UnitValue.createPercentValue(100));
            emptyTable.setMarginTop(20);
            emptyTable.setMarginBottom(20);
            
            Cell emptyCell = new Cell();
            emptyCell.setBorder(Border.NO_BORDER);
            emptyCell.setPadding(30);
            emptyCell.setTextAlignment(TextAlignment.CENTER);
            
            Paragraph emptyText = new Paragraph("No trading records found")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                    .setFontSize(12)
                    .setFontColor(SECONDARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER);
            
            emptyCell.add(emptyText);
            emptyTable.addCell(emptyCell);
            document.add(emptyTable);
            return;
        }
        
        // Section title
        Table titleTable = new Table(new float[]{1f});
        titleTable.setWidth(UnitValue.createPercentValue(100));
        titleTable.setMarginTop(15);
        titleTable.setMarginBottom(10);
        
        Cell titleCell = new Cell();
        titleCell.setBorder(Border.NO_BORDER);
        titleCell.setPaddingBottom(5);
        
        Paragraph sectionTitle = new Paragraph("Trade Details")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(14)
                .setFontColor(HEADER_BG);
        
        titleCell.add(sectionTitle);
        titleTable.addCell(titleCell);
        document.add(titleTable);
        
        // Create trade details table
        float[] columnWidths = {0.5f, 1.5f, 1f, 1f, 1f, 1.2f, 1f, 1.2f};
        Table table = new Table(columnWidths);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginTop(5);
        
        // Table headers
        String[] headers = {"SNO", "Stock Name", "Lots", "Entry", "Exit", "P&L", "Status", "Date"};
        TextAlignment[] headerAlignments = {
            TextAlignment.CENTER,  // #
            TextAlignment.LEFT,    // Stock Name
            TextAlignment.CENTER,  // Lots
            TextAlignment.RIGHT,   // Entry
            TextAlignment.RIGHT,   // Exit
            TextAlignment.RIGHT,   // P&L
            TextAlignment.CENTER,  // Status
            TextAlignment.CENTER   // Date
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell headerCell = new Cell();
            headerCell.setBackgroundColor(HEADER_BG);
            headerCell.setPadding(10);
            headerCell.setBorder(Border.NO_BORDER);
            headerCell.setTextAlignment(headerAlignments[i]);
            
            Paragraph headerPara = new Paragraph(headers[i])
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(9)
                    .setFontColor(ColorConstants.WHITE);
            
            headerCell.add(headerPara);
            table.addHeaderCell(headerCell);
        }
        
        // Data rows
        int index = 1;
        int rowIndex = 0;
        
        for (TradeEntry t : trades) {
            // Serial number - Center
            Cell serialCell = createTradeCell(String.format("%01d", index++), TextAlignment.CENTER, 
                                             9, TEXT_COLOR, false);
            
            // Stock name - Left
            Cell stockCell = createTradeCell(t.getStockName(), TextAlignment.LEFT, 
                                            9, TEXT_COLOR, true);
            
            // Lots - Center
            Cell lotsCell = createTradeCell(String.format("%.1f", t.getLots()), TextAlignment.CENTER, 
                                           9, TEXT_COLOR, false);
            
            // Entry - Right
            Cell entryCell = createTradeCell(String.format("%.2f", t.getEntryTrade()), TextAlignment.RIGHT, 
                                            9, TEXT_COLOR, false);
            
            // Exit - Right
            Cell exitCell = createTradeCell(String.format("%.2f", t.getExitTrade()), TextAlignment.RIGHT, 
                                           9, TEXT_COLOR, false);
            
            // P&L - Right with color (FIXED: Proper color coding)
            double pl = t.getProfitLossAmount();
            String plText = (pl >= 0 ? "+" : "") + String.format("%.2f", pl);
            Color plColor = pl > 0 ? SUCCESS_COLOR : (pl < 0 ? DANGER_COLOR : TEXT_COLOR);
            Cell plCell = createTradeCell(plText, TextAlignment.RIGHT, 
                                         9, plColor, true);
            
            // Status with colored badge (FIXED: Clear color distinction)
            Cell statusCell = new Cell();
            statusCell.setPadding(6);
            statusCell.setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
            statusCell.setTextAlignment(TextAlignment.CENTER);
            statusCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            
            // Convert status to string safely
            String status = t.getResultStatus() != null ? t.getResultStatus().toString() : "N/A";
            
            // FIXED: Determine if it's a win/loss based on P&L value, not status string
            boolean isWin = pl > 0;
            Color statusColor = isWin ? SUCCESS_COLOR : DANGER_COLOR;
            Color statusBg = isWin ? SUCCESS_BG : DANGER_BG;
            String displayStatus = isWin ? "PROFIT" : "LOSS";
            
            Table badgeTable = new Table(new float[]{1f});
            badgeTable.setWidth(UnitValue.createPercentValue(80));
            badgeTable.setHorizontalAlignment(HorizontalAlignment.CENTER);
            
            Cell badgeCell = new Cell();
            badgeCell.setBackgroundColor(statusBg);
            badgeCell.setBorder(new SolidBorder(statusColor, 0.5f));
            badgeCell.setPadding(3);
            badgeCell.setTextAlignment(TextAlignment.CENTER);
            
            Paragraph badgeText = new Paragraph(displayStatus)
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(8)
                    .setFontColor(statusColor);
            
            badgeCell.add(badgeText);
            badgeTable.addCell(badgeCell);
            statusCell.add(badgeTable);
            
            // Date - Center
            String dateStr = t.getTimestamp() != null ? 
                            t.getTimestamp().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "-";
            Cell dateCell = createTradeCell(dateStr, TextAlignment.CENTER, 
                                           9, TEXT_COLOR, false);
            
            // Apply alternating row background
            if (rowIndex % 2 == 0) {
                serialCell.setBackgroundColor(LIGHT_GRAY_BG);
                stockCell.setBackgroundColor(LIGHT_GRAY_BG);
                lotsCell.setBackgroundColor(LIGHT_GRAY_BG);
                entryCell.setBackgroundColor(LIGHT_GRAY_BG);
                exitCell.setBackgroundColor(LIGHT_GRAY_BG);
                plCell.setBackgroundColor(LIGHT_GRAY_BG);
                dateCell.setBackgroundColor(LIGHT_GRAY_BG);
            }
            
            table.addCell(serialCell);
            table.addCell(stockCell);
            table.addCell(lotsCell);
            table.addCell(entryCell);
            table.addCell(exitCell);
            table.addCell(plCell);
            table.addCell(statusCell);
            table.addCell(dateCell);
            
            rowIndex++;
        }
        
        document.add(table);
    }

    private Cell createTradeCell(String text, TextAlignment alignment, 
                                 int fontSize, Color fontColor, boolean isBold) throws IOException {
        Cell cell = new Cell();
        cell.setPadding(8);
        cell.setBorder(new SolidBorder(new DeviceRgb(224, 224, 224), 0.5f));
        cell.setTextAlignment(alignment);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        
        Paragraph para = new Paragraph(text)
                .setFont(isBold ? PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD) : PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(fontSize)
                .setFontColor(fontColor);
        
        cell.add(para);
        return cell;
    }

    private void addTradeSignatureSection(Document document, Color SECONDARY_COLOR, Color BORDER_COLOR) throws IOException {
        document.add(new Paragraph("\n\n"));
        
        Table signatureTable = new Table(new float[]{1f, 1f});
        signatureTable.setWidth(UnitValue.createPercentValue(100));
        signatureTable.setMarginTop(30);
        
        // Left side - Authorized By
        Cell leftCell = new Cell();
        leftCell.setBorder(Border.NO_BORDER);
        leftCell.setPadding(10);
        leftCell.setTextAlignment(TextAlignment.LEFT);
        
        Paragraph authorizedBy = new Paragraph("Authorized By:")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(9)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(30);
        
        leftCell.add(authorizedBy);
        
        // Right side - signatures
        Cell rightCell = new Cell();
        rightCell.setBorder(Border.NO_BORDER);
        rightCell.setPadding(5);
        rightCell.setTextAlignment(TextAlignment.RIGHT);
        
        // Signature 1 - Masthan Shareef
        Table sig1Table = createTradeSignatureBlock("Masthan Shareef", "Trader", BORDER_COLOR, SECONDARY_COLOR);
        rightCell.add(sig1Table);
        
        // Signature 2 - Saketh Ram
        Table sig2Table = createTradeSignatureBlock("Saketh Ram", "Trader", BORDER_COLOR, SECONDARY_COLOR);
        rightCell.add(sig2Table);
        
        signatureTable.addCell(leftCell);
        signatureTable.addCell(rightCell);
        
        // Line above signatures
        Table lineTable = new Table(new float[]{1f});
        lineTable.setWidth(UnitValue.createPercentValue(100));
        Cell lineCell = new Cell();
        lineCell.setBorder(new SolidBorder(BORDER_COLOR, 1));
        lineCell.setHeight(1);
        lineCell.setPadding(0);
        lineTable.addCell(lineCell);
        document.add(lineTable);
        
        document.add(signatureTable);
    }

    private Table createTradeSignatureBlock(String name, String designation, Color BORDER_COLOR, Color SECONDARY_COLOR) throws IOException {
        Table sigTable = new Table(new float[]{1f});
        sigTable.setWidth(UnitValue.createPercentValue(40));
        sigTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        
        Cell sigCell = new Cell();
        sigCell.setBorder(Border.NO_BORDER);
        sigCell.setTextAlignment(TextAlignment.CENTER);
        sigCell.setPadding(5);
        
        // Signature text
        Paragraph signature = new Paragraph(name)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(12)
                .setFontColor(new DeviceRgb(33, 33, 33))
                .setItalic()
                .setMarginBottom(5);
        
        sigCell.add(signature);
        
        // Line
        Paragraph line = new Paragraph(new String(new char[25]).replace('\0', '-'))
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(8)
                .setFontColor(BORDER_COLOR)
                .setMarginBottom(3);
        
        sigCell.add(line);
        
        // Designation
        Paragraph desig = new Paragraph(designation)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR);
        
        sigCell.add(desig);
        
        sigTable.addCell(sigCell);
        return sigTable;
    }

}