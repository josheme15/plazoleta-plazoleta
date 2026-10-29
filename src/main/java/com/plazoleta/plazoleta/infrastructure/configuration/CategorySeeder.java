package com.plazoleta.plazoleta.infrastructure.configuration;

import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.CategoryEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.ICategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Precarga el catalogo de categorias de platos (solo inserta las que no existen).
 */
@Component
@RequiredArgsConstructor
public class CategorySeeder implements CommandLineRunner {

    private static final Map<String, String> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put("Entradas", "Platos para abrir el apetito");
        CATEGORIES.put("Platos fuertes", "Platos principales");
        CATEGORIES.put("Postres", "Dulces y postres");
        CATEGORIES.put("Bebidas", "Bebidas frias y calientes");
        CATEGORIES.put("Acompanamientos", "Porciones adicionales para acompanar");
    }

    private final ICategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        CATEGORIES.forEach((name, description) -> {
            if (!categoryRepository.existsByName(name)) {
                categoryRepository.save(new CategoryEntity(null, name, description));
            }
        });
    }
}
