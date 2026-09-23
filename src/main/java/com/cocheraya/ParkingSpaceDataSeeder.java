package com.cocheraya;

import com.cocheraya.entity.Feature;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.User;
import com.cocheraya.repository.FeatureRepository;
import com.cocheraya.repository.ParkingSpaceRepository;
import com.cocheraya.repository.UserRepository;
import com.cocheraya.service.ParkingSpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ParkingSpaceDataSeeder implements CommandLineRunner {

    private final FeatureRepository featureRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final UserRepository userRepository;

    private static final String HOST_EMAIL = "seeded_host@cocheraya.com";

    
    private static final List<String> FEATURE_NAMES = List.of(
            "Techado",
            "Seguridad 24h",
            "Acceso Camionetas",
            "Iluminación",
            "Cámaras"
    );

    @Override
    public void run(String... args) {
        seedFeatures();
        seedParkingSpaces();
    }

    

    private void seedFeatures() {
        boolean anyExists = FEATURE_NAMES.stream()
                .anyMatch(featureRepository::existsByName);

        if (anyExists) {
            log.info("Features ya existen — omitiendo creación de features.");
            return;
        }

        log.info("Creando features de cocheras...");
        for (String name : FEATURE_NAMES) {
            Feature feature = new Feature();
            feature.setName(name);
            featureRepository.save(feature);
        }
        log.info("✅ Features creadas: {}", FEATURE_NAMES);
    }

    

    private void seedParkingSpaces() {
        Optional<User> hostOpt = userRepository.findByEmail(HOST_EMAIL);
        if (hostOpt.isEmpty()) {
            log.warn("Host {} no encontrado — omitiendo creación de cocheras. " +
                    "Asegúrate de que DataSeeder (@Order(1)) se ejecutó primero.", HOST_EMAIL);
            return;
        }
        User host = hostOpt.get();

        
        Feature techado   = getOrNull("Techado");
        Feature seguridad = getOrNull("Seguridad 24h");
        Feature camioneta = getOrNull("Acceso Camionetas");
        Feature ilum      = getOrNull("Iluminación");
        Feature camaras   = getOrNull("Cámaras");

        log.info("Creando cocheras de prueba en Lima...");

        
        ParkingSpace barranco = new ParkingSpace();
        barranco.setHost(host);
        barranco.setTitle("Cochera Techada en Barranco");
        barranco.setDescription("Amplio estacionamiento techado con cámaras de seguridad, " +
                "a 2 cuadras del Parque Municipal de Barranco.");
        barranco.setAddress("Jr. Independencia 325, Barranco, Lima");
        barranco.setPricePerHour(new BigDecimal("5.00"));
        barranco.setLocation(buildPoint(-77.0214, -12.1391)); 
        barranco.setFeatures(setOf(techado, camaras));
        saveIfNotExist(barranco);
        log.info("Cochera creada: 'Cochera Techada en Barranco' — S/. 5.00/h");

        
        ParkingSpace miraflores = new ParkingSpace();
        miraflores.setHost(host);
        miraflores.setTitle("Estacionamiento Seguro Miraflores");
        miraflores.setDescription("Estacionamiento premium en el corazón de Miraflores, " +
                "vigilancia 24 horas, acceso para camionetas y SUVs.");
        miraflores.setAddress("Av. Larco 740, Miraflores, Lima");
        miraflores.setPricePerHour(new BigDecimal("8.00"));
        miraflores.setLocation(buildPoint(-77.0299, -12.1219)); 
        miraflores.setFeatures(setOf(seguridad, camioneta, ilum));
        saveIfNotExist(miraflores);
        log.info("Cochera creada: 'Estacionamiento Seguro Miraflores' — S/. 8.00/h");

        
        ParkingSpace sanIsidro = new ParkingSpace();
        sanIsidro.setHost(host);
        sanIsidro.setTitle("Parking Empresarial San Isidro");
        sanIsidro.setDescription("Cochera en zona financiera de San Isidro, " +
                "iluminación LED, ideal para ejecutivos.");
        sanIsidro.setAddress("Av. El Rosario 250, San Isidro, Lima");
        sanIsidro.setPricePerHour(new BigDecimal("4.50"));
        sanIsidro.setLocation(buildPoint(-77.0465, -12.0931)); 
        sanIsidro.setFeatures(setOf(ilum, seguridad));
        saveIfNotExist(sanIsidro);
        log.info("Cochera creada: 'Parking Empresarial San Isidro' — S/. 4.50/h");

        // --- CHORRILLOS ---
        ParkingSpace chorrillos1 = new ParkingSpace();
        chorrillos1.setHost(host);
        chorrillos1.setTitle("Cochera Frente a Playa Agua Dulce");
        chorrillos1.setDescription("Estacionamiento vigilado las 24 horas frente al malecón de Chorrillos.");
        chorrillos1.setAddress("Malecón Grau 150, Chorrillos, Lima");
        chorrillos1.setPricePerHour(new BigDecimal("4.00"));
        chorrillos1.setLocation(buildPoint(-77.0315, -12.1625));
        chorrillos1.setFeatures(setOf(seguridad, ilum));
        saveIfNotExist(chorrillos1);

        ParkingSpace chorrillos2 = new ParkingSpace();
        chorrillos2.setHost(host);
        chorrillos2.setTitle("Cochera Residencial Chorrillos Centro");
        chorrillos2.setDescription("Estacionamiento techado y seguro a pocas cuadras de la Plaza de Armas.");
        chorrillos2.setAddress("Av. Huaylas 420, Chorrillos, Lima");
        chorrillos2.setPricePerHour(new BigDecimal("3.50"));
        chorrillos2.setLocation(buildPoint(-77.0210, -12.1585));
        chorrillos2.setFeatures(setOf(techado, camaras));
        saveIfNotExist(chorrillos2);

        // --- MIRAFLORES (Adicionales) ---
        ParkingSpace miraflores1 = new ParkingSpace();
        miraflores1.setHost(host);
        miraflores1.setTitle("Cochera Premium Larcomar");
        miraflores1.setDescription("Estacionamiento con control de accesos, techado y cámaras de seguridad.");
        miraflores1.setAddress("Malecón de la Reserva 610, Miraflores, Lima");
        miraflores1.setPricePerHour(new BigDecimal("9.00"));
        miraflores1.setLocation(buildPoint(-77.0305, -12.1315));
        miraflores1.setFeatures(setOf(techado, seguridad, camaras));
        saveIfNotExist(miraflores1);

        ParkingSpace miraflores2 = new ParkingSpace();
        miraflores2.setHost(host);
        miraflores2.setTitle("Estacionamiento Parque del Amor");
        miraflores2.setDescription("Estacionamiento con iluminación LED, ideal para paseos nocturnos.");
        miraflores2.setAddress("Av. Malecón Cisneros 240, Miraflores, Lima");
        miraflores2.setPricePerHour(new BigDecimal("7.00"));
        miraflores2.setLocation(buildPoint(-77.0375, -12.1265));
        miraflores2.setFeatures(setOf(ilum, seguridad));
        saveIfNotExist(miraflores2);

        // --- BARRANCO (Adicionales) ---
        ParkingSpace barranco1 = new ParkingSpace();
        barranco1.setHost(host);
        barranco1.setTitle("Cochera Artística Barranco");
        barranco1.setDescription("Cochera vigilada y con cámaras cerca del Puente de los Suspiros.");
        barranco1.setAddress("Jr. Carrión 112, Barranco, Lima");
        barranco1.setPricePerHour(new BigDecimal("6.00"));
        barranco1.setLocation(buildPoint(-77.0225, -12.1485));
        barranco1.setFeatures(setOf(seguridad, camaras));
        saveIfNotExist(barranco1);

        ParkingSpace barranco2 = new ParkingSpace();
        barranco2.setHost(host);
        barranco2.setTitle("Cochera Boulevard Barranco");
        barranco2.setDescription("Estacionamiento techado y seguro cerca del Boulevard de Barranco.");
        barranco2.setAddress("Av. Bolognesi 520, Barranco, Lima");
        barranco2.setPricePerHour(new BigDecimal("5.50"));
        barranco2.setLocation(buildPoint(-77.0205, -12.1420));
        barranco2.setFeatures(setOf(techado, seguridad));
        saveIfNotExist(barranco2);

        // --- SANTIAGO DE SURCO ---
        ParkingSpace surco1 = new ParkingSpace();
        surco1.setHost(host);
        surco1.setTitle("Cochera Jockey Plaza Express");
        surco1.setDescription("Estacionamiento premium cerca del Jockey Plaza. Vigilancia 24/7.");
        surco1.setAddress("Av. Manuel Olguín 220, Santiago de Surco, Lima");
        surco1.setPricePerHour(new BigDecimal("8.50"));
        surco1.setLocation(buildPoint(-76.9760, -12.0865));
        surco1.setFeatures(setOf(seguridad, camioneta, ilum));
        saveIfNotExist(surco1);

        ParkingSpace surco2 = new ParkingSpace();
        surco2.setHost(host);
        surco2.setTitle("Parking Parque de la Amistad");
        surco2.setDescription("Cochera residencial techada e iluminada a metros del Parque de la Amistad.");
        surco2.setAddress("Av. Caminos del Inca 1820, Santiago de Surco, Lima");
        surco2.setPricePerHour(new BigDecimal("6.00"));
        surco2.setLocation(buildPoint(-76.9795, -12.1365));
        surco2.setFeatures(setOf(techado, ilum, camaras));
        saveIfNotExist(surco2);

        // --- LA MOLINA ---
        ParkingSpace molina1 = new ParkingSpace();
        molina1.setHost(host);
        molina1.setTitle("Cochera Universitaria La Molina");
        molina1.setDescription("Estacionamiento seguro en Av. Javier Prado, ideal para estudiantes de la U de Lima.");
        molina1.setAddress("Av. Javier Prado Este 4600, La Molina, Lima");
        molina1.setPricePerHour(new BigDecimal("6.50"));
        molina1.setLocation(buildPoint(-76.9655, -12.0835));
        molina1.setFeatures(setOf(seguridad, camaras));
        saveIfNotExist(molina1);

        ParkingSpace molina2 = new ParkingSpace();
        molina2.setHost(host);
        molina2.setTitle("Parking Residencial Molina Plaza");
        molina2.setDescription("Cochera techada amplia en zona comercial Molina Plaza.");
        molina2.setAddress("Av. Raúl Ferrero 1200, La Molina, Lima");
        molina2.setPricePerHour(new BigDecimal("5.00"));
        molina2.setLocation(buildPoint(-76.9385, -12.0945));
        molina2.setFeatures(setOf(techado, ilum));
        saveIfNotExist(molina2);

        // --- SAN ISIDRO (Adicionales) ---
        ParkingSpace sanIsidro1 = new ParkingSpace();
        sanIsidro1.setHost(host);
        sanIsidro1.setTitle("Cochera El Olivar");
        sanIsidro1.setDescription("Estacionamiento exclusivo en zona residencial El Olivar. Seguridad 24h.");
        sanIsidro1.setAddress("Calle Choquehuanca 310, San Isidro, Lima");
        sanIsidro1.setPricePerHour(new BigDecimal("7.50"));
        sanIsidro1.setLocation(buildPoint(-77.0350, -12.1005));
        sanIsidro1.setFeatures(setOf(seguridad, camioneta));
        saveIfNotExist(sanIsidro1);

        ParkingSpace sanIsidro2 = new ParkingSpace();
        sanIsidro2.setHost(host);
        sanIsidro2.setTitle("Parking Centro Financiero");
        sanIsidro2.setDescription("Cochera techada ideal para ejecutivos, en el corazón financiero.");
        sanIsidro2.setAddress("Av. Rivera Navarrete 540, San Isidro, Lima");
        sanIsidro2.setPricePerHour(new BigDecimal("8.00"));
        sanIsidro2.setLocation(buildPoint(-77.0250, -12.0940));
        sanIsidro2.setFeatures(setOf(techado, camaras, seguridad));
        saveIfNotExist(sanIsidro2);

        log.info("✅ Cocheras de prueba extendidas creadas (15 en total) en Chorrillos, Miraflores, Barranco, Surco, La Molina y San Isidro.");
    }

    

    /**
     * Construye un Point con SRID 4326 (WGS84/GPS).
     * En WGS84: X = longitud, Y = latitud.
     */
    private Point buildPoint(double longitude, double latitude) {
        GeometryFactory gf = new GeometryFactory();
        Point point = gf.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    
    private Feature getOrNull(String name) {
        return featureRepository.findByName(name).orElse(null);
    }

    
    @SafeVarargs
    private Set<Feature> setOf(Feature... features) {
        Set<Feature> set = new HashSet<>();
        for (Feature f : features) {
            if (f != null) set.add(f);
        }
        return set;
    }

    private void saveIfNotExist(ParkingSpace space) {
        if (!parkingSpaceRepository.existsByTitle(space.getTitle())) {
            parkingSpaceRepository.save(space);
            log.info("Cochera creada: '{}' — S/. {}/h", space.getTitle(), space.getPricePerHour());
        }
    }
}
