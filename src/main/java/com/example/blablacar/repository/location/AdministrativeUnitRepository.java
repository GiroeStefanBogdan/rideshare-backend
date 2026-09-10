package com.example.blablacar.repository.location;

import com.example.blablacar.model.location.AdministrativeUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@Repository
public interface AdministrativeUnitRepository extends JpaRepository<AdministrativeUnit, Long> {

    @Query(value = """
            SELECT a.* FROM admin_units a
            WHERE a.active = TRUE
              AND a.type <> 'COUNTY'
              AND a.name_norm LIKE CONCAT('%', LOWER(unaccent(:name)), '%')
            ORDER BY a.population DESC NULLS LAST
            LIMIT 10
            """, nativeQuery = true)
    List<AdministrativeUnit> findTop10ByNameContainingIgnoreCaseOrderByPopulationDesc(@Param("name") String name);
}
