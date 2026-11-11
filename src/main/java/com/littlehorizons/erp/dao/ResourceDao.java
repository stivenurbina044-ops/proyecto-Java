package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceRegistration;
import com.littlehorizons.erp.model.SchoolResource;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class ResourceDao {
    public List<ResourceCategory> findCategories() throws SQLException {
        List<ResourceCategory> categories = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(
                     "SELECT id_categoria, nombre_categoria FROM categorias_recurso ORDER BY nombre_categoria");
             var result = statement.executeQuery()) {
            while (result.next()) {
                categories.add(new ResourceCategory(result.getInt("id_categoria"), result.getString("nombre_categoria")));
            }
        }
        return categories;
    }

    public List<SchoolResource> findAll() throws SQLException {
        String sql = """
                SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, r.descripcion,
                       r.fecha_adquisicion, r.ubicacion_fisica, r.estado_recurso, c.nombre_categoria
                FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria
                ORDER BY r.nombre_recurso, r.codigo_recurso
                """;
        List<SchoolResource> resources = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                var date = result.getDate("fecha_adquisicion");
                resources.add(new SchoolResource(result.getInt("id_recurso"), result.getString("codigo_recurso"),
                        result.getString("nombre_recurso"), result.getString("nombre_categoria"),
                        result.getString("descripcion"), date == null ? null : date.toLocalDate(),
                        result.getString("ubicacion_fisica"), result.getString("estado_recurso")));
            }
        }
        return resources;
    }

    public int insert(ResourceRegistration resource) throws SQLException {
        String sql = """
                INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion,
                                      fecha_adquisicion, ubicacion_fisica, estado_recurso)
                SELECT ?, ?, id_categoria, ?, ?, ?, 'Disponible'
                FROM categorias_recurso WHERE id_categoria = ?
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, resource.code());
            statement.setString(2, resource.name());
            statement.setString(3, resource.description().isEmpty() ? null : resource.description());
            if (resource.acquisitionDate() == null) {
                statement.setNull(4, Types.DATE);
            } else {
                statement.setDate(4, Date.valueOf(resource.acquisitionDate()));
            }
            statement.setString(5, resource.location());
            statement.setInt(6, resource.categoryId());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("La categoría ya no existe.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Resource ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }
}
