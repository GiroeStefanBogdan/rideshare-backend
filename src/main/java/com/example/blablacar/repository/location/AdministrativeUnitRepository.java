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

    @Query("SELECT a FROM AdministrativeUnit a LEFT JOIN FETCH a.parent WHERE lower(function('unaccent', a.name)) LIKE lower(concat('%', function('unaccent', :name), '%')) ORDER BY a.population DESC NULLS LAST")
    List<AdministrativeUnit> findTop10ByNameContainingIgnoreCaseOrderByPopulationDesc(@Param("name") String name);
}
