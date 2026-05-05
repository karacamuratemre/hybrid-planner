package com.hybridplanner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pyo")
@Data
@NoArgsConstructor
public class Pyo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 5)
    private String code;

    @OneToMany(mappedBy = "pyo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teams = new ArrayList<>();

    public Pyo(String name, String code) {
        this.name = name;
        this.code = code;
    }
}
