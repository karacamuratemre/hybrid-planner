package com.hybridplanner.controller;

import com.hybridplanner.model.*;
import com.hybridplanner.repository.*;
import com.hybridplanner.service.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final PyoRepository pyoRepository;
    private final RoomRepository roomRepository;
    private final TeamRepository teamRepository;
    private final StaffRepository staffRepository;
    private final DistributionService distributionService;
    private final ExcelImportService excelImportService;
    private final DocExportService docExportService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("staffCount", staffRepository.count());
        model.addAttribute("teamCount", teamRepository.count());
        model.addAttribute("roomCount", roomRepository.count());
        model.addAttribute("pyoCount", pyoRepository.count());
        return "index";
    }

    // ── PYO ──────────────────────────────────────
    @GetMapping("/pyo")
    public String pyoList(Model model) {
        model.addAttribute("pyos", pyoRepository.findAll());
        return "pyo/list";
    }

    @PostMapping("/pyo")
    public String addPyo(@RequestParam String name, @RequestParam String code, RedirectAttributes ra) {
        if (name.isBlank()) { ra.addFlashAttribute("error", "PYO adı boş olamaz"); return "redirect:/pyo"; }
        pyoRepository.save(new Pyo(name.trim(), code.isBlank() ? initials(name) : code.trim().toUpperCase()));
        ra.addFlashAttribute("success", "PYO eklendi: " + name);
        return "redirect:/pyo";
    }

    @PostMapping("/pyo/{id}/delete")
    public String deletePyo(@PathVariable Long id, RedirectAttributes ra) {
        pyoRepository.deleteById(id);
        ra.addFlashAttribute("success", "PYO silindi");
        return "redirect:/pyo";
    }

    @PostMapping("/pyo/import")
    public String importPyo(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) { ra.addFlashAttribute("error", "Dosya seçiniz"); return "redirect:/pyo"; }
        try {
            ExcelImportService.ImportResult r = excelImportService.importPyo(file);
            ra.addFlashAttribute("success", r.imported() + " PYO import edildi");
            if (r.hasErrors()) ra.addFlashAttribute("importErrors", r.errors());
        } catch (IOException e) { ra.addFlashAttribute("error", "Dosya okunamadı: " + e.getMessage()); }
        return "redirect:/pyo";
    }

    // ── ROOM ─────────────────────────────────────
    @GetMapping("/room")
    public String roomList(Model model) {
        List<Room> rooms = roomRepository.findAll();
        Map<Long, Integer> roomUsage = new HashMap<>();
        teamRepository.findAllWithDetails().forEach(t ->
                roomUsage.merge(t.getRoom().getId(), t.getStaffList().size(), Integer::sum));
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomUsage", roomUsage);
        return "room/list";
    }

    @PostMapping("/room")
    public String addRoom(@RequestParam String name, @RequestParam int capacity, RedirectAttributes ra) {
        if (name.isBlank()) { ra.addFlashAttribute("error", "Oda adı boş olamaz"); return "redirect:/room"; }
        roomRepository.save(new Room(name.trim(), capacity));
        ra.addFlashAttribute("success", "Oda eklendi: " + name);
        return "redirect:/room";
    }

    @PostMapping("/room/{id}/delete")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes ra) {
        roomRepository.deleteById(id);
        ra.addFlashAttribute("success", "Oda silindi");
        return "redirect:/room";
    }

    @PostMapping("/room/import")
    public String importRoom(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) { ra.addFlashAttribute("error", "Dosya seçiniz"); return "redirect:/room"; }
        try {
            ExcelImportService.ImportResult r = excelImportService.importRoom(file);
            ra.addFlashAttribute("success", r.imported() + " oda import edildi");
            if (r.hasErrors()) ra.addFlashAttribute("importErrors", r.errors());
        } catch (IOException e) { ra.addFlashAttribute("error", "Dosya okunamadı: " + e.getMessage()); }
        return "redirect:/room";
    }

    // ── TEAM ─────────────────────────────────────
    @GetMapping("/team")
    public String teamList(Model model) {
        model.addAttribute("teams", teamRepository.findAllWithDetails());
        model.addAttribute("pyos", pyoRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        return "team/list";
    }

    @PostMapping("/team")
    public String addTeam(@RequestParam String name, @RequestParam Long pyoId,
                          @RequestParam Long roomId, RedirectAttributes ra) {
        if (name.isBlank()) { ra.addFlashAttribute("error", "Ekip adı boş olamaz"); return "redirect:/team"; }
        teamRepository.save(new Team(name.trim(), pyoRepository.findById(pyoId).orElseThrow(),
                roomRepository.findById(roomId).orElseThrow()));
        ra.addFlashAttribute("success", "Ekip eklendi: " + name);
        return "redirect:/team";
    }

    @PostMapping("/team/{id}/delete")
    public String deleteTeam(@PathVariable Long id, RedirectAttributes ra) {
        teamRepository.deleteById(id);
        ra.addFlashAttribute("success", "Ekip silindi");
        return "redirect:/team";
    }

    @PostMapping("/team/import")
    public String importTeam(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) { ra.addFlashAttribute("error", "Dosya seçiniz"); return "redirect:/team"; }
        try {
            ExcelImportService.ImportResult r = excelImportService.importTeam(file);
            ra.addFlashAttribute("success", r.imported() + " ekip import edildi");
            if (r.hasErrors()) ra.addFlashAttribute("importErrors", r.errors());
        } catch (IOException e) { ra.addFlashAttribute("error", "Dosya okunamadı: " + e.getMessage()); }
        return "redirect:/team";
    }

    // ── STAFF ────────────────────────────────────
    @GetMapping("/staff")
    public String staffList(Model model) {
        model.addAttribute("staffList", staffRepository.findAllWithDetails());
        model.addAttribute("teams", teamRepository.findAllWithDetails());
        model.addAttribute("allDays", WorkDay.values());
        return "staff/list";
    }

    @PostMapping("/staff")
    public String addStaff(@RequestParam String name,
                           @RequestParam(required = false) String email,
                           @RequestParam Long teamId,
                           @RequestParam(required = false) List<String> days,
                           RedirectAttributes ra) {
        if (name.isBlank()) { ra.addFlashAttribute("error", "Ad boş olamaz"); return "redirect:/staff"; }
        Staff staff = new Staff();
        staff.setName(name.trim());
        staff.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        staff.setTeam(teamRepository.findById(teamId).orElseThrow());
        if (days != null && days.size() == 3) {
            staff.setWorkDays(days.stream().map(WorkDay::valueOf).toList());
            staff.setManualDays(true);
        }
        staffRepository.save(staff);
        ra.addFlashAttribute("success", "Personel eklendi: " + name);
        return "redirect:/staff";
    }

    @PostMapping("/staff/{id}/delete")
    public String deleteStaff(@PathVariable Long id, RedirectAttributes ra) {
        staffRepository.deleteById(id);
        ra.addFlashAttribute("success", "Personel silindi");
        return "redirect:/staff";
    }

    @PostMapping("/staff/import")
    public String importStaff(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) { ra.addFlashAttribute("error", "Dosya seçiniz"); return "redirect:/staff"; }
        try {
            ExcelImportService.ImportResult r = excelImportService.importStaff(file);
            ra.addFlashAttribute("success", r.imported() + " personel import edildi");
            if (r.hasErrors()) ra.addFlashAttribute("importErrors", r.errors());
        } catch (IOException e) { ra.addFlashAttribute("error", "Dosya okunamadı: " + e.getMessage()); }
        return "redirect:/staff";
    }

    // ── DISTRIBUTE ───────────────────────────────
    @GetMapping("/distribute")
    public String distributePage(Model model) {
        model.addAttribute("staffCount", staffRepository.count());
        model.addAttribute("teamCount", teamRepository.count());
        model.addAttribute("roomCount", roomRepository.count());
        return "distribute/index";
    }

    @PostMapping("/distribute")
    public String distribute(Model model) {
        DistributionService.DistributionResult result = distributionService.distribute();
        model.addAttribute("result", result);
        model.addAttribute("allDays", WorkDay.values());
        model.addAttribute("distributed", true);
        return "distribute/index";
    }

    @GetMapping("/distribute/export")
    public void exportDoc(HttpServletResponse response) throws IOException {
        DistributionService.DistributionResult result = distributionService.distribute();
        byte[] docBytes = docExportService.generatePlanDocument(result);
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"hybrid-plan.docx\"");
        response.setContentLength(docBytes.length);
        response.getOutputStream().write(docBytes);
    }

    private String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String p : name.trim().split("\\s+")) if (!p.isEmpty()) sb.append(p.charAt(0));
        return sb.toString().toUpperCase().substring(0, Math.min(2, sb.length()));
    }
}