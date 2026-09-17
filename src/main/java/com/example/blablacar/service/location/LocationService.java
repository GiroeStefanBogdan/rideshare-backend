package com.example.blablacar.service.location;

import com.example.blablacar.dto.location.LocationResultDTO;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.location.Street;
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.location.StreetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class LocationService {

    private static final String ADMIN_UNIT_TYPE = "ADMIN_UNIT";
    private static final String STREET_TYPE = "STREET";
    private static final int MAX_RESULTS = 15;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int NO_MATCH_SCORE = 4;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final AdministrativeUnitRepository adminUnitRepository;
    private final StreetRepository streetRepository;

    public LocationService(final AdministrativeUnitRepository adminUnitRepository,
                           final StreetRepository streetRepository) {
        this.adminUnitRepository = adminUnitRepository;
        this.streetRepository = streetRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationResultDTO> search(final String query) {
        if (query == null) {
            return List.of();
        }

        final String trimmedQuery = query.trim();
        if (trimmedQuery.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        return Stream.concat(
                findAdministrativeUnits(trimmedQuery).stream(),
                findStreets(trimmedQuery).stream()
        )
                .filter((final LocationResultDTO location) -> isRelevantStreetMatch(location, trimmedQuery))
                .sorted(getLocationComparator(trimmedQuery))
                .limit(MAX_RESULTS)
                .toList();
    }

    private List<LocationResultDTO> findAdministrativeUnits(final String query) {
        return adminUnitRepository.findTop10ByNameContainingIgnoreCaseOrderByPopulationDesc(query)
                .stream()
                .map(this::toAdministrativeUnitResult)
                .toList();
    }

    private LocationResultDTO toAdministrativeUnitResult(final AdministrativeUnit administrativeUnit) {
        return new LocationResultDTO(
                administrativeUnit.getId(),
                ADMIN_UNIT_TYPE,
                administrativeUnit.getName(),
                getAdministrativeUnitFullName(administrativeUnit),
                administrativeUnit.getLatitude(),
                administrativeUnit.getLongitude(),
                administrativeUnit.getPopulation()
        );
    }

    private String getAdministrativeUnitFullName(final AdministrativeUnit administrativeUnit) {
        if (administrativeUnit.getDisplayFullName() != null) {
            return administrativeUnit.getDisplayFullName();
        }
        if (administrativeUnit.getParent() == null) {
            return administrativeUnit.getName();
        }
        return administrativeUnit.getName() + ", " + administrativeUnit.getParent().getName();
    }

    private List<LocationResultDTO> findStreets(final String query) {
        return streetRepository.findTop10ByFullNameContainingIgnoreCase(query)
                .stream()
                .map(this::toStreetResult)
                .toList();
    }

    private LocationResultDTO toStreetResult(final Street street) {
        return new LocationResultDTO(
                street.getId(),
                STREET_TYPE,
                street.getName(),
                getStreetFullName(street),
                street.getLatitude(),
                street.getLongitude(),
                null
        );
    }

    private String getStreetFullName(final Street street) {
        if (street.getDisplayFullName() != null) {
            return street.getDisplayFullName();
        }
        return street.getName() + ", " + street.getLocation().getName();
    }

    private boolean isRelevantStreetMatch(final LocationResultDTO location, final String query) {
        return !STREET_TYPE.equals(location.type()) || getMatchScore(location.fullName(), query) < NO_MATCH_SCORE;
    }

    private Comparator<LocationResultDTO> getLocationComparator(final String query) {
        return Comparator.comparingInt((final LocationResultDTO location) -> getMatchScore(location.name(), query))
                .thenComparingInt(this::getTypeRank)
                .thenComparing(LocationResultDTO::population, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int getTypeRank(final LocationResultDTO location) {
        if (ADMIN_UNIT_TYPE.equals(location.type())) {
            return 0;
        }
        return 1;
    }

    private int getMatchScore(final String name, final String query) {
        if (name == null || query == null) {
            return NO_MATCH_SCORE;
        }

        final String normalizedName = normalizeForSearch(name);
        final String normalizedQuery = normalizeForSearch(query);

        if (normalizedName.equals(normalizedQuery)) {
            return 0;
        }
        if (normalizedName.startsWith(normalizedQuery) || normalizedQuery.startsWith(normalizedName)) {
            return 1;
        }
        if (normalizedName.contains(" " + normalizedQuery) || normalizedName.contains("-" + normalizedQuery)) {
            return 2;
        }
        if (normalizedName.contains(normalizedQuery)) {
            return 3;
        }
        if (Stream.of(normalizedQuery.split("[\\s,]+"))
                .filter((final String token) -> !token.isBlank())
                .allMatch(normalizedName::contains)) {
            return 3;
        }

        return NO_MATCH_SCORE;
    }

    private String normalizeForSearch(final String value) {
        final String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("").toLowerCase(Locale.ROOT);
    }
}
