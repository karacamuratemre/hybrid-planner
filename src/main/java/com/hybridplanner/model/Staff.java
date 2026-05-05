package com.hybridplanner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "staff")
@Data
@NoArgsConstructor
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ElementCollection
    @CollectionTable(name = "staff_workdays", joinColumns = @JoinColumn(name = "staff_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "work_day")
    private List<WorkDay> workDays = new ArrayList<>();

    // Manuel olarak mı atandı, yoksa otomatik mi?
    @Column
    private boolean manualDays = false;

    public String getInitials() {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(part.charAt(0));
        }
        return sb.toString().toUpperCase().substring(0, Math.min(2, sb.length()));
    }
}
