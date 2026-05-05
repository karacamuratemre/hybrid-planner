package com.hybridplanner.service;

import com.hybridplanner.model.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DocExportService {

    private static final String TITLE_COLOR = "2E3A8C";
    private static final String HEADER_COLOR = "4A6FBF";
    private static final String ROW_ALT_COLOR = "EEF2FF";
    private static final String TEAM_COLOR = "F0F4FF";

    public byte[] generatePlanDocument(DistributionService.DistributionResult result) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            setPageMargins(doc);

            // Başlık
            addTitle(doc, "Hybrid Çalışma Planı");
            addSubtitle(doc, "Oluşturulma tarihi: " +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr"))));

            addEmptyParagraph(doc);

            // Uyarılar
            if (result.hasWarnings()) {
                addSectionHeader(doc, "⚠ Kapasite Uyarıları", "C0392B");
                for (String w : result.warnings()) {
                    addBullet(doc, w, "C0392B");
                }
                addEmptyParagraph(doc);
            }

            // Özet istatistikler
            addSectionHeader(doc, "Özet", TITLE_COLOR);
            Map<String, Long> stats = Map.of(
                    "Toplam Personel", (long) result.allStaff().size(),
                    "Toplam Ekip", result.allStaff().stream().map(s -> s.getTeam().getId()).distinct().count(),
                    "Toplam Oda", result.allStaff().stream().map(s -> s.getTeam().getRoom().getId()).distinct().count()
            );
            addStatsTable(doc, stats);
            addEmptyParagraph(doc);

            // Gün bazlı plan
            addSectionHeader(doc, "Günlük Oturma Planı", TITLE_COLOR);
            addEmptyParagraph(doc);

            for (Map.Entry<WorkDay, Map<Room, List<Staff>>> dayEntry : result.plan().entrySet()) {
                WorkDay day = dayEntry.getKey();
                Map<Room, List<Staff>> roomMap = dayEntry.getValue();

                if (roomMap.isEmpty()) continue;

                int total = roomMap.values().stream().mapToInt(List::size).sum();
                addDayHeader(doc, day.getDisplayName() + " — " + total + " kişi");

                for (Map.Entry<Room, List<Staff>> roomEntry : roomMap.entrySet()) {
                    Room room = roomEntry.getKey();
                    List<Staff> staffList = roomEntry.getValue();
                    boolean overCapacity = staffList.size() > room.getCapacity();

                    addRoomTable(doc, room, staffList, overCapacity);
                    addEmptyParagraph(doc);
                }
            }

            // Oda bazlı özet
            addPageBreak(doc);
            addSectionHeader(doc, "Oda Bazlı Atama Özeti", TITLE_COLOR);
            addEmptyParagraph(doc);

            Map<Room, List<Staff>> roomSummary = new LinkedHashMap<>();
            for (Staff s : result.allStaff()) {
                roomSummary.computeIfAbsent(s.getTeam().getRoom(), k -> new ArrayList<>()).add(s);
            }

            for (Map.Entry<Room, List<Staff>> entry : roomSummary.entrySet()) {
                addRoomSummaryTable(doc, entry.getKey(), entry.getValue());
                addEmptyParagraph(doc);
            }

            doc.write(out);
            return out.toByteArray();
        }
    }

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(20);
        r.setColor(TITLE_COLOR);
        r.setFontFamily("Arial");
    }

    private void addSubtitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(10);
        r.setColor("666666");
        r.setFontFamily("Arial");
    }

    private void addSectionHeader(XWPFDocument doc, String text, String color) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(13);
        r.setColor(color);
        r.setFontFamily("Arial");
        // Alt çizgi
        CTBorder border = p.getCTP().getPPr() == null ?
                p.getCTP().addNewPPr().addNewPBdr().addNewBottom() :
                p.getCTP().getPPr().isSetPBdr() ?
                        p.getCTP().getPPr().getPBdr().addNewBottom() :
                        p.getCTP().getPPr().addNewPBdr().addNewBottom();
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setColor(HEADER_COLOR);
    }

    private void addDayHeader(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(160);
        p.setSpacingAfter(80);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(12);
        r.setColor(HEADER_COLOR);
        r.setFontFamily("Arial");
    }

    private void addBullet(XWPFDocument doc, String text, String color) {
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(720);
        XWPFRun r = p.createRun();
        r.setText("• " + text);
        r.setFontSize(10);
        r.setColor(color != null ? color : "333333");
        r.setFontFamily("Arial");
    }

    private void addStatsTable(XWPFDocument doc, Map<String, Long> stats) {
        XWPFTable table = doc.createTable(1, stats.size());
        table.setWidth("100%");
        table.getCTTbl().getTblPr().unsetTblBorders();

        XWPFTableRow row = table.getRow(0);
        int col = 0;
        for (Map.Entry<String, Long> e : stats.entrySet()) {
            XWPFTableCell cell = row.getCell(col++);
            setTableCellColor(cell, EEF2FF_COLOR);
            cell.removeParagraph(0);
            XWPFParagraph p = cell.addParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun numRun = p.createRun();
            numRun.setText(String.valueOf(e.getValue()));
            numRun.setBold(true);
            numRun.setFontSize(16);
            numRun.setColor(TITLE_COLOR);
            numRun.setFontFamily("Arial");
            numRun.addBreak();
            XWPFRun labelRun = p.createRun();
            labelRun.setText(e.getKey());
            labelRun.setFontSize(9);
            labelRun.setColor("666666");
            labelRun.setFontFamily("Arial");
        }
    }

    private static final String EEF2FF_COLOR = "EEF2FF";

    private void addRoomTable(XWPFDocument doc, Room room, List<Staff> staffList, boolean overCapacity) {
        // Oda başlığı
        XWPFParagraph roomTitle = doc.createParagraph();
        roomTitle.setIndentationLeft(360);
        XWPFRun rt = roomTitle.createRun();
        rt.setText(room.getName() + "  (" + staffList.size() + "/" + room.getCapacity() + " kişi)" +
                (overCapacity ? " ⚠ KAPASİTE AŞILDI" : ""));
        rt.setBold(false);
        rt.setFontSize(10);
        rt.setColor(overCapacity ? "C0392B" : "444444");
        rt.setFontFamily("Arial");

        // Personel tablosu
        XWPFTable table = doc.createTable(staffList.size() + 1, 3);
        table.setWidth("80%");

        // Header row
        String[] headers = {"Ad Soyad", "Ekip", "E-posta"};
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            setTableCellColor(cell, HEADER_COLOR);
            cell.removeParagraph(0);
            XWPFParagraph p = cell.addParagraph();
            XWPFRun r = p.createRun();
            r.setText(headers[i]);
            r.setBold(true);
            r.setFontSize(9);
            r.setColor("FFFFFF");
            r.setFontFamily("Arial");
        }

        // Data rows
        for (int i = 0; i < staffList.size(); i++) {
            Staff s = staffList.get(i);
            XWPFTableRow row = table.getRow(i + 1);
            String bg = (i % 2 == 0) ? "FFFFFF" : ROW_ALT_COLOR;

            setCell(row, 0, s.getName(), bg, false, "333333");
            setCell(row, 1, s.getTeam().getName(), bg, false, "555555");
            setCell(row, 2, s.getEmail() != null ? s.getEmail() : "—", bg, false, "777777");
        }
    }

    private void addRoomSummaryTable(XWPFDocument doc, Room room, List<Staff> staffList) {
        addDayHeader(doc, room.getName() + " (Kapasite: " + room.getCapacity() + ")");

        // Ekip bazında grupla
        Map<Team, List<Staff>> byTeam = new LinkedHashMap<>();
        for (Staff s : staffList) {
            byTeam.computeIfAbsent(s.getTeam(), k -> new ArrayList<>()).add(s);
        }

        for (Map.Entry<Team, List<Staff>> entry : byTeam.entrySet()) {
            Team team = entry.getKey();
            List<Staff> members = entry.getValue();

            XWPFTable table = doc.createTable(members.size() + 1, 4);
            table.setWidth("100%");

            // Team header
            XWPFTableRow headerRow = table.getRow(0);
            XWPFTableCell mergedCell = headerRow.getCell(0);
            setTableCellColor(mergedCell, TEAM_COLOR);
            mergedCell.removeParagraph(0);
            XWPFParagraph hp = mergedCell.addParagraph();
            XWPFRun hr = hp.createRun();
            hr.setText("Ekip: " + team.getName() + "  |  PYO: " + team.getPyo().getName());
            hr.setBold(true);
            hr.setFontSize(9);
            hr.setColor(TITLE_COLOR);
            hr.setFontFamily("Arial");
            // Diğer header hücrelerini temizle
            for (int c = 1; c < 4; c++) {
                setTableCellColor(headerRow.getCell(c), TEAM_COLOR);
                headerRow.getCell(c).removeParagraph(0);
                headerRow.getCell(c).addParagraph();
            }

            // Günler header
            String[] dayHeaders = {"Ad Soyad", "Pzt", "Sal", "Çar", "Per", "Cum"};
            // Yeni satır: gün bilgisi için 6 sütun isteriz ama tablo 4 sütun, basit tutalım
            for (int i = 0; i < members.size(); i++) {
                Staff s = members.get(i);
                XWPFTableRow row = table.getRow(i + 1);
                String bg = (i % 2 == 0) ? "FFFFFF" : ROW_ALT_COLOR;
                setCell(row, 0, s.getName(), bg, false, "333333");
                StringBuilder days = new StringBuilder();
                for (WorkDay d : WorkDay.values()) {
                    days.append(s.getWorkDays().contains(d) ? d.getShortName() : "—").append("  ");
                }
                setCell(row, 1, days.toString().trim(), bg, false, "555555");
                setCell(row, 2, s.getEmail() != null ? s.getEmail() : "—", bg, false, "777777");
                setCell(row, 3, s.isManualDays() ? "Manuel" : "Otomatik", bg, false, "999999");
            }

            addEmptyParagraph(doc);
        }
    }

    private void setCell(XWPFTableRow row, int col, String text, String bg, boolean bold, String color) {
        XWPFTableCell cell = row.getCell(col);
        if (cell == null) cell = row.addNewTableCell();
        setTableCellColor(cell, bg);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontSize(9);
        r.setColor(color);
        r.setFontFamily("Arial");
    }

    private void setTableCellColor(XWPFTableCell cell, String hexColor) {
        CTTc ctTc = cell.getCTTc();
        CTTcPr tcPr = ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(hexColor);
    }

    private void addEmptyParagraph(XWPFDocument doc) {
        doc.createParagraph();
    }

    private void addPageBreak(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setPageBreak(true);
    }

    private void setPageMargins(XWPFDocument doc) {
        CTSectPr sectPr = doc.getDocument().getBody().isSetSectPr() ?
                doc.getDocument().getBody().getSectPr() :
                doc.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(720));
        pageMar.setBottom(BigInteger.valueOf(720));
        pageMar.setLeft(BigInteger.valueOf(1080));
        pageMar.setRight(BigInteger.valueOf(1080));
    }
}
