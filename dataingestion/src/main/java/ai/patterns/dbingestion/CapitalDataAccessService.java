package ai.patterns.dbingestion;

import ai.patterns.CapitalData;
import ai.patterns.CapitalData.Continent;
import ai.patterns.CapitalDataRAG;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Configuration class for database connection and transaction management
 */
@Configuration
@EnableTransactionManagement
class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.hikari.idle-timeout:30000}")
    private int idleTimeout;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private int maxLifetime;

    /**
     * Configure and create HikariDataSource
     * @return Configured DataSource
     */
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setIdleTimeout(idleTimeout);
        config.setConnectionTimeout(connectionTimeout);
        config.setMaxLifetime(maxLifetime);

        // PostgreSQL-specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }

    /**
     * Configure JdbcTemplate with the HikariDataSource
     * @param dataSource The configured HikariDataSource
     * @return JdbcTemplate
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Configure transaction manager
     * @param dataSource The configured HikariDataSource
     * @return PlatformTransactionManager
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}

/**
 * Data Access Service for capital, continent, and capital_continent relationships
 */
@Repository
public class CapitalDataAccessService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CapitalDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Capital-related methods

    /**
     * Insert a new capital
     * @param capital Capital name
     * @param country Country name
     * @param embedType Embedding type (hierarchical, hypothetical, contextual, late)
     * @param content Capital content
     * @param chunk Text chunk for embedding
     * @return The generated capital ID
     */
    public int insertCapital(String capital, String country) {
        final String sql = "INSERT INTO capitals (capital, country) VALUES (?, ?) ON CONFLICT (capital) DO NOTHING RETURNING capital_id";

        // Try to insert and get the ID in one step
        Integer id = jdbcTemplate.query(sql,
            ps -> {
                ps.setString(1, capital);
                ps.setString(2, country);
            },
            rs -> rs.next() ? rs.getInt("capital_id") : null
        );

        // If no row was inserted (conflict occurred), get the existing ID
        if (id == null) {
            id = jdbcTemplate.queryForObject(
                "SELECT capital_id FROM capitals WHERE capital = ?",
                Integer.class,
                capital
            );
        }

        return id;
        // KeyHolder keyHolder = new GeneratedKeyHolder();
        //
        // jdbcTemplate.update(connection -> {
        //     PreparedStatement ps = connection.prepareStatement(sql); //, Statement.RETURN_GENERATED_KEYS);
        //     ps.setString(1, capital);
        //     ps.setString(2, country);
        //     return ps;
        // }, keyHolder);
        //
        // return Integer.valueOf(keyHolder.getKeys().get("capital_id") + "");
    }

    /**
     * Get a capital by its ID
     * @param capitalId The capital ID
     * @return Optional containing the Capital if found
     */
    public Optional<Capital> getCapitalById(int capitalId) {
        final String sql = "SELECT capital_id, capital, country, embed::text, content, chunk FROM capitals WHERE capital_id = ?";

        List<Capital> capitals = jdbcTemplate.query(sql, new Object[]{capitalId}, capitalRowMapper());

        return capitals.isEmpty() ? Optional.empty() : Optional.of(capitals.get(0));
    }

    /**
     * Get a capital by its name
     * @param capitalName The capital name
     * @return Optional containing the Capital if found
     */
    public Optional<Capital> getCapitalByName(String capitalName) {
        final String sql = "SELECT capital_id, capital, country, embed::text, content, chunk FROM capitals WHERE capital = ?";

        List<Capital> capitals = jdbcTemplate.query(sql, new Object[]{capitalName}, capitalRowMapper());

        return capitals.isEmpty() ? Optional.empty() : Optional.of(capitals.get(0));
    }

    /**
     * Get all capitals
     * @return List of all capitals
     */
    public List<Capital> getAllCapitals() {
        final String sql = "SELECT capital_id, capital, country, embed::text, content, chunk FROM capitals";

        return jdbcTemplate.query(sql, capitalRowMapper());
    }

    // Continent-related methods

    /**
     * Insert a new continent
     * @param continentName Name of the continent
     * @return The generated continent ID
     */
    public int insertContinent(String continentName) {
        final String sql = "INSERT INTO continents (continent_name) VALUES (?) ON CONFLICT (continent_name) DO NOTHING RETURNING continent_id";

        // Try to insert and get ID
        Integer id = jdbcTemplate.query(sql, new Object[]{continentName}, rs -> {
            if (rs.next()) {
                return rs.getInt("continent_id");
            }
            return null;
        });

        // If no ID returned (because continent already exists), get the existing ID
        if (id == null) {
            final String selectSql = "SELECT continent_id FROM continents WHERE continent_name = ?";
            id = jdbcTemplate.queryForObject(selectSql, new Object[]{continentName}, Integer.class);
        }

        return id;
    }

    /**
     * Get a continent by its ID
     * @param continentId The continent ID
     * @return Optional containing the Continent if found
     */
    public Optional<Continent> getContinentById(int continentId) {
        final String sql = "SELECT continent_id, continent_name FROM continents WHERE continent_id = ?";

        List<Continent> continents = jdbcTemplate.query(sql, new Object[]{continentId}, continentRowMapper());

        return continents.isEmpty() ? Optional.empty() : Optional.of(continents.get(0));
    }

    /**
     * Get all continents
     * @return List of all continents
     */
    public List<Continent> getAllContinents() {
        final String sql = "SELECT continent_id, continent_name FROM continents";

        return jdbcTemplate.query(sql, continentRowMapper());
    }

    // Capital-Continent relationship methods

    /**
     * Associate a capital with a continent
     * @param capitalId The capital ID
     * @param continentId The continent ID
     */
    public void linkCapitalToContinent(int capitalId, int continentId) {
        final String sql = "INSERT INTO capital_continents (capital_id, continent_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

        jdbcTemplate.update(sql, capitalId, continentId);
    }

    /**
     * Get all continents for a capital
     * @param capitalId The capital ID
     * @return List of continents associated with the capital
     */
    public List<Continent> getContinentsForCapital(int capitalId) {
        final String sql = "SELECT c.continent_id, c.continent_name " +
                "FROM continents c " +
                "JOIN capital_continents cc ON c.continent_id = cc.continent_id " +
                "WHERE cc.capital_id = ?";

        return jdbcTemplate.query(sql, new Object[]{capitalId}, continentRowMapper());
    }

    /**
     * Get all capitals for a continent
     * @param continentId The continent ID
     * @return List of capitals associated with the continent
     */
    public List<Capital> getCapitalsForContinent(int continentId) {
        final String sql = "SELECT c.capital_id, c.capital, c.country, c.embed::text, c.content, c.chunk " +
                "FROM capitals c " +
                "JOIN capital_continents cc ON c.capital_id = cc.capital_id " +
                "WHERE cc.continent_id = ?";

        return jdbcTemplate.query(sql, new Object[]{continentId}, capitalRowMapper());
    }

    /**
     * Get detailed information about a capital including its continents
     * @param capitalName The capital name
     * @return A CapitalDetail object with all information
     */
    // public Optional<CapitalDetail> getCapitalDetailByName(String capitalName) {
    //     final String sql = "SELECT c.capital_id, c.capital, c.country, c.embed::text, c.content, c.chunk, " +
    //             "cont.continent_name " +
    //             "FROM capitals c " +
    //             "JOIN capital_continents cc ON c.capital_id = cc.capital_id " +
    //             "JOIN continents cont ON cc.continent_id = cont.continent_id " +
    //             "WHERE c.capital = ?";
    //
    //     List<CapitalDetailRow> rows = jdbcTemplate.query(sql, new Object[]{capitalName}, (rs, rowNum) -> {
    //         CapitalDetailRow row = new CapitalDetailRow();
    //         row.setCapitalId(rs.getInt("capital_id"));
    //         row.setCapital(rs.getString("capital"));
    //         row.setCountry(rs.getString("country"));
    //         row.setEmbedType(rs.getString("embed"));
    //         row.setContent(rs.getString("content"));
    //         row.setChunk(rs.getString("chunk"));
    //         row.setContinentName(rs.getString("continent_name"));
    //         return row;
    //     });
    //
    //     if (rows.isEmpty()) {
    //         return Optional.empty();
    //     }
    //
    //     // Group by capital (should be only one in this case)
    //     CapitalDetailRow firstRow = rows.get(0);
    //     Capital capital = new Capital(
    //             firstRow.getCapitalId(),
    //             firstRow.getCapital(),
    //             firstRow.getCountry(),
    //             firstRow.getEmbedType(),
    //             firstRow.getContent(),
    //             firstRow.getChunk()
    //     );
    //
    //     // Collect all continents
    //     List<String> continents = rows.stream()
    //             .map(CapitalDetailRow::getContinentName)
    //             .collect(Collectors.toList());
    //
    //     return Optional.of(new CapitalDetail(capital, continents));
    // }

    @Transactional
    public CapitalDetail insertCapitalWithContinentsAndChunks(CapitalDataRAG capital) {

        // Insert capital
        int capitalId = insertCapital(
            capital.capitalData().capitalCity().city(),
            capital.capitalData().country().country());

        // Create capital object
        Capital capitalObj = new Capital(
            capitalId,
            capital.capitalData().capitalCity().city(),
            capital.capitalData().country().country(),
            String.valueOf(capital.embedType()),
            capital.content(),
            capital.chunk());

        List<String> continentNames = capital.capitalData().continents().stream()
            .map(CapitalData.Continent::getDisplayName)
            .collect(Collectors.toList());

        // Insert or get continents and link them
        for (String continentName : continentNames) {
            int continentId = insertContinent(continentName);
            linkCapitalToContinent(capitalId, continentId);
        }

        // Insert chunks
        for (String content : capital.content()) {
            int chunkId = insertCapitalChunk(
                capitalId,
                capital.embedType().name().toLowerCase(),
                content,
                capital.chunk()
            );
        }

        // Return complete detail
        return new CapitalDetail(capitalObj, continentNames);
    }

    /**
     * Insert a new chunk for a capital
     * @param capitalId ID of the capital
     * @param embedType Embedding type
     * @param content Content text
     * @param chunk Chunk text for embedding
     * @return The generated chunk ID
     */
    public int insertCapitalChunk(int capitalId, String embedType, String content, String chunk) {
        final String sql = "INSERT INTO capital_chunks (capital_id, embed, content, chunk) VALUES (?, ?::embed_type, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, capitalId);
            ps.setString(2, embedType);
            ps.setString(3, content);
            ps.setString(4, chunk);
            return ps;
        }, keyHolder);

        return Integer.valueOf(keyHolder.getKeys().get("capital_id") + "");
    }

    /**
     * Insert a complete capital with its continent associations in a single transaction
     * @param capital Capital name
     * @param country Country name
     * @param embedType Embedding type
     * @param content Capital content
     * @param chunk Text chunk for embedding
     * @param continentNames List of continent names
     * @return The CapitalDetail with all information
     */
    // @Transactional
    // public CapitalDetail insertCapitalWithContinents(
    //         String capital,
    //         String country,
    //         String embedType,
    //         String content,
    //         String chunk,
    //         List<String> continentNames) {
    //
    //     // Insert capital
    //     int capitalId = insertCapital(capital, country, embedType, content, chunk);
    //
    //     // Create capital object
    //     Capital capitalObj = new Capital(capitalId, capital, country, embedType, content, chunk);
    //
    //     // Insert or get continents and link them
    //     for (String continentName : continentNames) {
    //         int continentId = insertContinent(continentName);
    //         linkCapitalToContinent(capitalId, continentId);
    //     }
    //
    //     // Return complete detail
    //     return new CapitalDetail(capitalObj, continentNames);
    // }

    // Row mappers

    private RowMapper<Capital> capitalRowMapper() {
        return (rs, rowNum) -> {
            int capitalId = rs.getInt("capital_id");
            String capital = rs.getString("capital");
            String country = rs.getString("country");
            String embedType = rs.getString("embed");
            // @todo
            // String content = rs.getString("content");
            String chunk = rs.getString("chunk");

            return new Capital(capitalId, capital, country, embedType, new ArrayList<>(), chunk);
        };
    }

    private RowMapper<Continent> continentRowMapper() {
        return (rs, rowNum) -> {
            int continentId = rs.getInt("continent_id");
            String continentName = rs.getString("continent_name");

            return new Continent(continentId, continentName);
        };
    }

    // Model classes
    public record Capital(
        int capitalId,
        String capital,
        String country,
        String embedType,
        List<String> content,
        String chunk
    ){}
    // public static class Capital {
    //     private final int capitalId;
    //     private final String capital;
    //     private final String country;
    //     private final String embedType;
    //     private final String content;
    //     private final String chunk;
    //
    //     public Capital(int capitalId, String capital, String country, String embedType, String content, String chunk) {
    //         this.capitalId = capitalId;
    //         this.capital = capital;
    //         this.country = country;
    //         this.embedType = embedType;
    //         this.content = content;
    //         this.chunk = chunk;
    //     }
    //
    //     public int getCapitalId() { return capitalId; }
    //     public String getCapital() { return capital; }
    //     public String getCountry() { return country; }
    //     public String getEmbedType() { return embedType; }
    //     public String getContent() { return content; }
    //     public String getChunk() { return chunk; }
    //
    //     @Override
    //     public String toString() {
    //         return "Capital{" +
    //                 "capitalId=" + capitalId +
    //                 ", capital='" + capital + '\'' +
    //                 ", country='" + country + '\'' +
    //                 ", embedType='" + embedType + '\'' +
    //                 ", content='" + content + '\'' +
    //                 ", chunk='" + chunk + '\'' +
    //                 '}';
    //     }
    // }

    public static class Continent {
        private final int continentId;
        private final String continentName;

        public Continent(int continentId, String continentName) {
            this.continentId = continentId;
            this.continentName = continentName;
        }

        public int getContinentId() { return continentId; }
        public String getContinentName() { return continentName; }

        @Override
        public String toString() {
            return "Continent{" +
                    "continentId=" + continentId +
                    ", continentName='" + continentName + '\'' +
                    '}';
        }
    }

    public static class CapitalDetail {
        private final Capital capital;
        private final List<String> continents;

        public CapitalDetail(Capital capital, List<String> continents) {
            this.capital = capital;
            this.continents = continents;
        }

        public Capital getCapital() { return capital; }
        public List<String> getContinents() { return continents; }

        @Override
        public String toString() {
            return "CapitalDetail{" +
                    "capital=" + capital +
                    ", continents=" + continents +
                    '}';
        }
    }

    private static class CapitalDetailRow {
        private int capitalId;
        private String capital;
        private String country;
        private String embedType;
        private String content;
        private String chunk;
        private String continentName;

        public int getCapitalId() { return capitalId; }
        public void setCapitalId(int capitalId) { this.capitalId = capitalId; }

        public String getCapital() { return capital; }
        public void setCapital(String capital) { this.capital = capital; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getEmbedType() { return embedType; }
        public void setEmbedType(String embedType) { this.embedType = embedType; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getChunk() { return chunk; }
        public void setChunk(String chunk) { this.chunk = chunk; }

        public String getContinentName() { return continentName; }
        public void setContinentName(String continentName) { this.continentName = continentName; }
    }
}
