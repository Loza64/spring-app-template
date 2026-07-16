package com.server.app.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Catalogo de permisos (path + metodo HTTP) auto-descubierto a partir de los
 * endpoints expuestos por la aplicacion (ver {@code SaveEndpoints}).
 *
 * A peticion explicita del negocio, esta entidad NO maneja soft delete: los
 * permisos que ya no correspondan a un endpoint vigente se eliminan de forma
 * definitiva, no se ocultan logicamente.
 */
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columnNames = { "path", "method" }))
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false)
    private String path;

    @Column(nullable = false)
    private String method;

    @Column(nullable = true)
    private String title;
}
