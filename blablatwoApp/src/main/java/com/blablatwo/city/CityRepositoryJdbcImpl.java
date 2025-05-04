package com.blablatwo.city;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CityRepositoryJdbcImpl implements CityRepositoryJdbc {
    private static final Logger LOGGER = LoggerFactory.getLogger(CityRepositoryJdbcImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public CityRepositoryJdbcImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("city")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Optional<City> findById(Long id) {
        String sql = "SELECT * FROM city WHERE id = ?";
        try {
            City city = jdbcTemplate.queryForObject(sql, new CityRowMapper(), id);
            return Optional.ofNullable(city);
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<City> findAll() {
        return jdbcTemplate.query("SELECT * FROM city", new CityRowMapper());
    }

    @Override
    public City save(City city) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", city.getName());
        Number id = simpleJdbcInsert.executeAndReturnKey(data);
        city.setId(id.longValue());
        return city;
    }

    @Override
    public boolean deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM city WHERE id = ?", id) == 1;
    }

    @Override
    public Optional<City> update(City city) {
        int rowsAffected = jdbcTemplate.update(
                "UPDATE city SET name = ? WHERE id = ?",
                city.getName(),
                city.getId()
        );
        if (rowsAffected == 0) {
            return Optional.empty();
        }
        return Optional.of(city);
    }

    private static class CityRowMapper implements RowMapper<City> {
        @Override
        public City mapRow(ResultSet rs, int rowNum) throws SQLException {
            City city = new City();
            city.setId(rs.getLong("ID"));
            city.setName(rs.getString("NAME"));
            return city;
        }
    }
}
