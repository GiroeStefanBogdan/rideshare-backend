package com.example.blablacar.repository.location;

import com.example.blablacar.model.location.LocationSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@Repository
public interface LocationSearchRepository extends JpaRepository<LocationSearch, Long> {
}
