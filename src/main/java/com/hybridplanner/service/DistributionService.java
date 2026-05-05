package com.hybridplanner.service;

import com.hybridplanner.model.*;
import com.hybridplanner.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DistributionService {

    private static final List<WorkDay> ALL_DAYS = List.of(WorkDay.values());

    private final StaffRepository staffRepository;

    /**
     * Tüm personele henüz manuel gün atanmamışsa otomatik 3 gün atar.
     * Kısıt: Aynı odada aynı günde kapasite aşılmamalı (best-effort, uyarı verir).
     */
    @Transactional
    public DistributionResult distribute() {
        List<Staff> allStaff = staffRepository.findAllWithDetails();

        // Önce manuel günleri olmayanlar için otomatik ata
        autoAssignDays(allStaff);

        // Sonra kapasite kontrolü
        List<String> warnings = checkCapacity(allStaff);

        // Haftalık plan oluştur
        Map<WorkDay, Map<Room, List<Staff>>> plan = buildPlan(allStaff);

        return new DistributionResult(plan, warnings, allStaff);
    }

    private void autoAssignDays(List<Staff> allStaff) {
        // Günlük oda yükü haritası: roomId -> day -> count
        Map<Long, Map<WorkDay, Integer>> roomDayLoad = new HashMap<>();

        // Manuel günleri olanlara göre mevcut yükü hesapla
        for (Staff s : allStaff) {
            if (s.isManualDays() && !s.getWorkDays().isEmpty()) {
                Room room = s.getTeam().getRoom();
                roomDayLoad.computeIfAbsent(room.getId(), k -> new EnumMap<>(WorkDay.class));
                for (WorkDay d : s.getWorkDays()) {
                    roomDayLoad.get(room.getId()).merge(d, 1, Integer::sum);
                }
            }
        }

        // Otomatik atama: her personel için en düşük yüklu 3 günü seç
        for (Staff s : allStaff) {
            if (!s.isManualDays() || s.getWorkDays().isEmpty()) {
                Room room = s.getTeam().getRoom();
                Map<WorkDay, Integer> dayLoad = roomDayLoad.computeIfAbsent(
                        room.getId(), k -> new EnumMap<>(WorkDay.class));

                // Tüm günleri yüke göre sırala, en düşük 3'ü al
                List<WorkDay> sorted = new ArrayList<>(ALL_DAYS);
                sorted.sort(Comparator.comparingInt(d -> dayLoad.getOrDefault(d, 0)));
                List<WorkDay> assigned = sorted.subList(0, 3);

                s.setWorkDays(new ArrayList<>(assigned));
                s.setManualDays(false);

                // Yükü güncelle
                for (WorkDay d : assigned) {
                    dayLoad.merge(d, 1, Integer::sum);
                }

                staffRepository.save(s);
            }
        }
    }

    private List<String> checkCapacity(List<Staff> allStaff) {
        List<String> warnings = new ArrayList<>();

        // roomId -> day -> personeller
        Map<Long, Map<WorkDay, List<Staff>>> roomDayStaff = new HashMap<>();
        for (Staff s : allStaff) {
            Long roomId = s.getTeam().getRoom().getId();
            for (WorkDay d : s.getWorkDays()) {
                roomDayStaff
                        .computeIfAbsent(roomId, k -> new EnumMap<>(WorkDay.class))
                        .computeIfAbsent(d, k -> new ArrayList<>())
                        .add(s);
            }
        }

        for (Staff s : allStaff) {
            Room room = s.getTeam().getRoom();
            for (WorkDay d : s.getWorkDays()) {
                List<Staff> inRoom = roomDayStaff
                        .getOrDefault(room.getId(), Collections.emptyMap())
                        .getOrDefault(d, Collections.emptyList());
                if (inRoom.size() > room.getCapacity()) {
                    String warn = room.getName() + " – " + d.getDisplayName() +
                            " günü kapasite aşımı: " + inRoom.size() + "/" + room.getCapacity();
                    if (!warnings.contains(warn)) warnings.add(warn);
                }
            }
        }
        return warnings;
    }

    public Map<WorkDay, Map<Room, List<Staff>>> buildPlan(List<Staff> allStaff) {
        Map<WorkDay, Map<Room, List<Staff>>> plan = new LinkedHashMap<>();
        for (WorkDay day : ALL_DAYS) {
            Map<Room, List<Staff>> roomMap = new LinkedHashMap<>();
            for (Staff s : allStaff) {
                if (s.getWorkDays().contains(day)) {
                    roomMap.computeIfAbsent(s.getTeam().getRoom(), k -> new ArrayList<>()).add(s);
                }
            }
            plan.put(day, roomMap);
        }
        return plan;
    }

    public record DistributionResult(
            Map<WorkDay, Map<Room, List<Staff>>> plan,
            List<String> warnings,
            List<Staff> allStaff
    ) {
        public boolean hasWarnings() { return !warnings.isEmpty(); }
    }
}
