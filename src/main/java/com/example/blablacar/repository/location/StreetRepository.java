package com.example.blablacar.repository.location;

import com.example.blablacar.model.location.Street;
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
public interface StreetRepository extends JpaRepository<Street, Long> {

    @Query(value = """
            SELECT s.* FROM streets s
            WHERE s.active = TRUE
              AND NOT EXISTS (
                  SELECT 1
                  FROM regexp_split_to_table(LOWER(unaccent(BTRIM(:fullName))), '[[:space:],]+') AS token
                  WHERE token <> ''
                    AND s.full_name NOT LIKE CONCAT('%', token, '%')
              )
            LIMIT 10
            """, nativeQuery = true)
    List<Street> findTop10ByFullNameContainingIgnoreCase(@Param("fullName") String fullName);
}
