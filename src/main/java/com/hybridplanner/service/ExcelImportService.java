package com.hybridplanner.service;

import com.hybridplanner.model.*;
import com.hybridplanner.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final StaffRepository staffRepository;
    private final TeamRepository teamRepository;
    private final PyoRepository pyoRepository;
    private final RoomRepository roomRepository;

    // ─────────────────────────────────────────────
    //  PERSONEL IMPORT
    //  A: Ad Soyad*  B: E-posta  C: Ekip Adı*  D: Günler
    // ─────────────────────────────────────────────
    @Transactional
    public ImportResult importStaff(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();

        Map<String, Team> teamByName = new HashMap<>();
        teamRepository.findAllWithDetails().forEach(t ->
                teamByName.put(t.getName().trim().toLowerCase(), t));

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name     = cell(row, 0);
                String email    = cell(row, 1);
                String teamName = cell(row, 2);
                String daysStr  = cell(row, 3);
                if (name.isBlank()) continue;
                if (teamName.isBlank()) { errors.add("Satır " + (i+1) + ": Ekip boş – " + name); continue; }
                Team team = teamByName.get(teamName.trim().toLowerCase());
                if (team == null) { errors.add("Satır " + (i+1) + ": Ekip bulunamadı '" + teamName + "' – " + name); continue; }

                Staff s = new Staff();
                s.setName(name.trim());
                s.setEmail(email.isBlank() ? null : email.trim());
                s.setTeam(team);
                List<WorkDay> days = parseDays(daysStr);
                if (days.size() == 3) { s.setWorkDays(days); s.setManualDays(true); }
                else { s.setManualDays(false); }
                staffRepository.save(s);
                successes.add(name);
            }
        }
        return new ImportResult("Personel", successes.size(), errors, successes);
    }

    // ─────────────────────────────────────────────
    //  PYO IMPORT
    //  A: Ad Soyad*  B: Kısa Kod
    // ─────────────────────────────────────────────
    @Transactional
    public ImportResult importPyo(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name = cell(row, 0);
                String code = cell(row, 1);
                if (name.isBlank()) continue;
                if (code.isBlank()) code = initials(name);
                Pyo pyo = new Pyo(name.trim(), code.trim().toUpperCase());
                pyoRepository.save(pyo);
                successes.add(name);
            }
        }
        return new ImportResult("PYO", successes.size(), errors, successes);
    }

    // ─────────────────────────────────────────────
    //  ODA IMPORT
    //  A: Oda Adı*  B: Kapasite*
    // ─────────────────────────────────────────────
    @Transactional
    public ImportResult importRoom(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name    = cell(row, 0);
                String capStr  = cell(row, 1);
                if (name.isBlank()) continue;
                int cap = 4;
                try { cap = Integer.parseInt(capStr); } catch (NumberFormatException e) {
                    errors.add("Satır " + (i+1) + ": Kapasite sayısal değil '" + capStr + "', varsayılan 4 kullanıldı.");
                }
                roomRepository.save(new Room(name.trim(), cap));
                successes.add(name);
            }
        }
        return new ImportResult("Oda", successes.size(), errors, successes);
    }

    // ─────────────────────────────────────────────
    //  EKİP IMPORT
    //  A: Ekip Adı*  B: PYO Adı*  C: Oda Adı*
    // ─────────────────────────────────────────────
    @Transactional
    public ImportResult importTeam(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();

        Map<String, Pyo> pyoByName = new HashMap<>();
        pyoRepository.findAll().forEach(p -> pyoByName.put(p.getName().trim().toLowerCase(), p));

        Map<String, Room> roomByName = new HashMap<>();
        roomRepository.findAll().forEach(r -> roomByName.put(r.getName().trim().toLowerCase(), r));

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String teamName = cell(row, 0);
                String pyoName  = cell(row, 1);
                String roomName = cell(row, 2);
                if (teamName.isBlank()) continue;
                if (pyoName.isBlank())  { errors.add("Satır " + (i+1) + ": PYO boş – " + teamName); continue; }
                if (roomName.isBlank()) { errors.add("Satır " + (i+1) + ": Oda boş – " + teamName); continue; }

                Pyo pyo = pyoByName.get(pyoName.trim().toLowerCase());
                if (pyo == null) { errors.add("Satır " + (i+1) + ": PYO bulunamadı '" + pyoName + "' – " + teamName); continue; }

                Room room = roomByName.get(roomName.trim().toLowerCase());
                if (room == null) { errors.add("Satır " + (i+1) + ": Oda bulunamadı '" + roomName + "' – " + teamName); continue; }

                teamRepository.save(new Team(teamName.trim(), pyo, room));
                successes.add(teamName);
            }
        }
        return new ImportResult("Ekip", successes.size(), errors, successes);
    }

    // ─────────────────────────────────────────────
    //  YARDIMCILAR
    // ─────────────────────────────────────────────
    private List<WorkDay> parseDays(String daysStr) {
        if (daysStr == null || daysStr.isBlank()) return Collections.emptyList();
        List<WorkDay> result = new ArrayList<>();
        for (String part : daysStr.split("[,;\\s]+")) {
            WorkDay d = mapDay(part.trim());
            if (d != null && !result.contains(d)) result.add(d);
        }
        return result;
    }

    private WorkDay mapDay(String s) {
        return switch (s.toLowerCase()) {
            case "pzt","pazartesi","mon","monday"              -> WorkDay.PAZARTESI;
            case "sal","salı","sali","tue","tuesday"           -> WorkDay.SALI;
            case "çar","car","çarşamba","carsamba","wed","wednesday" -> WorkDay.CARSAMBA;
            case "per","perşembe","persembe","thu","thursday"  -> WorkDay.PERSEMBE;
            case "cum","cuma","fri","friday"                   -> WorkDay.CUMA;
            default -> null;
        };
    }

    private String cell(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            default -> "";
        };
    }

    private String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String p : name.trim().split("\\s+")) if (!p.isEmpty()) sb.append(p.charAt(0));
        return sb.toString().toUpperCase().substring(0, Math.min(2, sb.length()));
    }

    public record ImportResult(String type, int imported, List<String> errors, List<String> successes) {
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
}