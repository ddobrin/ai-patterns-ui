package ai.patterns.dbingestion;

import ai.patterns.CapitalDataRAG;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class demonstrating usage of the CapitalDataAccessService with HikariCP
 */
@Service
public class CapitalService {

    private final CapitalDataAccessService dataAccessService;

    @Autowired
    public CapitalService(CapitalDataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    /**
     * Add a new capital with its continents
     */
    @Transactional
    public CapitalDataAccessService.CapitalDetail addCapital(CapitalDataRAG capital) {
        return dataAccessService.insertCapitalWithContinentsAndChunks(capital);
    }

    /**
     * Add a new capital with its continents
     */
    // @Transactional
    // public CapitalDataAccessService.CapitalDetail addCapital(String capital, String country, List<String> continents) {
    //     return dataAccessService.insertCapitalWithContinents(
    //             capital,
    //             country,
    //             "hypothetical", // Default embed type
    //             "Content about " + capital + "...",
    //             "Chunk about " + capital + "...",
    //             continents
    //     );
    // }

    /**
     * Get capital details with continents
     */
    // public Optional<CapitalDataAccessService.CapitalDetail> getCapitalDetails(String capitalName) {
    //     return dataAccessService.getCapitalDetailByName(capitalName);
    // }

    /**
     * Get all capitals in a specific continent
     */
    public List<CapitalDataAccessService.Capital> getCapitalsInContinent(String continentName) {
        // Get continent ID
        List<CapitalDataAccessService.Continent> continents = dataAccessService.getAllContinents();

        // Find matching continent
        Optional<CapitalDataAccessService.Continent> continent = continents.stream()
                .filter(c -> c.getContinentName().equalsIgnoreCase(continentName))
                .findFirst();

        if (continent.isPresent()) {
            return dataAccessService.getCapitalsForContinent(continent.get().getContinentId());
        }

        return List.of(); // Empty list if continent not found
    }

    /**
     * Example usage in an application
     */
    /*
    public void demonstrateUsage() {
        // Add a new capital
        CapitalDataAccessService.CapitalDetail tokyo = addCapital(
                "Tokyo",
                "Japan",
                Arrays.asList("Asia")
        );
        System.out.println("Added: " + tokyo);

        // Get capital details for an existing capital
        Optional<CapitalDataAccessService.CapitalDetail> paris = getCapitalDetails("Paris");
        paris.ifPresent(p -> System.out.println("Found: " + p));

        // Get all capitals in Europe
        List<CapitalDataAccessService.Capital> europeanCapitals = getCapitalsInContinent("Europe");
        System.out.println("European capitals: " + europeanCapitals.size());
        europeanCapitals.forEach(System.out::println);
    }
    */

}