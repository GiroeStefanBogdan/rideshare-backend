package com.example.blablacar.repository.location;

import com.example.blablacar.model.location.AdministrativeUnitSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@Repository
public interface AdministrativeUnitSearchRepository extends JpaRepository<AdministrativeUnitSearch, Long> {
}
