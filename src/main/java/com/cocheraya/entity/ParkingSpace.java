package com.cocheraya.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa un espacio de estacionamiento publicado por un HOST en CocheraYa.
 *
 * COORDENADAS — SRID 4326 (WGS84):
 *   El estándar WGS84 es el sistema de referencia geodésico utilizado por GPS.
 *   Longitud en eje X, Latitud en eje Y. PostGIS usa este SRID para operaciones
 *   espaciales como ST_DWithin (búsqueda por radio en metros cuando se usa
 *   el tipo ::geography que convierte el cálculo a metros reales en la superficie).
 *
 * FOTO:
 *   photoUrl      — URL pública HTTPS devuelta por Cloudinary al subir la imagen.
 *   cloudinaryPublicId — identificador de la imagen en Cloudinary; necesario para
 *                        poder eliminarla cuando el host actualice o borre la cochera.
 */
@Entity
@Table(name = "parking_space")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false)
    @NotBlank
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @NotBlank
    private String address;

    
    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal pricePerHour;

    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingSpaceStatus status = ParkingSpaceStatus.AVAILABLE;

    /**
     * Coordenadas geográficas del espacio de estacionamiento.
     * SRID 4326 = WGS84, el sistema estándar de GPS (longitud X, latitud Y).
     * PostGIS utiliza este SRID para operaciones espaciales precisas en metros
     * al convertir a tipo geography (ST_DWithin con ::geography).
     */
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    
    @Column(name = "photo_url")
    private String photoUrl;

    
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "parking_space_features",
            joinColumns = @JoinColumn(name = "parking_space_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private Set<Feature> features = new HashSet<>();

    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_favorite_spaces",
            joinColumns = @JoinColumn(name = "parking_space_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> favoritedBy = new HashSet<>();

    /** Fecha y hora de creación, gestionada automáticamente por Hibernate */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    

    public enum ParkingSpaceStatus {
        
        AVAILABLE,
        
        RESERVED,
        
        OCCUPIED
    }
}
