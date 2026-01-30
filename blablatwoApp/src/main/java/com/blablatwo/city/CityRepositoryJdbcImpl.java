package com.blablatwo.city;

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
public abstract class CityRepositoryJdbcImpl implements CityRepositoryJdbc {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    protected CityRepositoryJdbcImpl(JdbcTemplate jdbcTemplate) {
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
        data.put("place_id", city.getPlaceId());
        data.put("name_pl", city.getNamePl());
        data.put("name_en", city.getNameEn());
        data.put("norm_name_pl", city.getNormNamePl());
        data.put("norm_name_en", city.getNormNameEn());
        data.put("country_code", city.getCountryCode());
        data.put("population", city.getPopulation());
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
                "UPDATE city SET place_id = ?, name_pl = ?, name_en = ?, norm_name_pl = ?, norm_name_en = ?, country_code = ?, population = ? WHERE id = ?",
                city.getPlaceId(),
                city.getNamePl(),
                city.getNameEn(),
                city.getNormNamePl(),
                city.getNormNameEn(),
                city.getCountryCode(),
                city.getPopulation(),
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
            city.setId(rs.getLong("id"));
            city.setPlaceId(rs.getLong("place_id"));
            city.setNamePl(rs.getString("name_pl"));
            city.setNameEn(rs.getString("name_en"));
            city.setNormNamePl(rs.getString("norm_name_pl"));
            city.setNormNameEn(rs.getString("norm_name_en"));
            city.setCountryCode(rs.getString("country_code"));
            Long population = rs.getLong("population");
            if (!rs.wasNull()) {
                city.setPopulation(population);
            }
            return city;
        }
    }
}
