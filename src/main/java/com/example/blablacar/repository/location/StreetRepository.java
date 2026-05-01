package com.example.blablacar.repository.location;

import com.example.blablacar.model.location.Street;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@Repository
public interface StreetRepository extends JpaRepository<Street, Long> {

    List<Street> findTop10ByFullNameContainingIgnoreCase(String fullName);
}
