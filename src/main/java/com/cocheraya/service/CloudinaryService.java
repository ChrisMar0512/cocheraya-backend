package com.cocheraya.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    
    public String uploadImage(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", "cocheraya/parking-spaces"
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            log.info("Imagen subida a Cloudinary exitosamente: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Error al leer el archivo de imagen para subir a Cloudinary", e);
            throw new RuntimeException(
                    "No se pudo leer el archivo de imagen: " + e.getMessage(), e
            );
        } catch (Exception e) {
            log.error("Error al subir imagen a Cloudinary", e);
            throw new RuntimeException(
                    "Error al subir la imagen a Cloudinary: " + e.getMessage(), e
            );
        }
    }

    
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            log.warn("Se intentó eliminar una imagen con publicId nulo o vacío — omitiendo.");
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Imagen eliminada de Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Error al eliminar imagen de Cloudinary con publicId: {}", publicId, e);
            throw new RuntimeException(
                    "No se pudo eliminar la imagen de Cloudinary: " + e.getMessage(), e
            );
        }
    }

    
    public String[] uploadImageWithPublicId(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", "cocheraya/parking-spaces"
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String publicId  = (String) result.get("public_id");

            log.info("Imagen subida a Cloudinary: url={}, publicId={}", secureUrl, publicId);
            return new String[]{secureUrl, publicId};

        } catch (IOException e) {
            log.error("Error al leer el archivo de imagen", e);
            throw new RuntimeException(
                    "No se pudo leer el archivo de imagen: " + e.getMessage(), e
            );
        } catch (Exception e) {
            log.error("Error al subir imagen a Cloudinary", e);
            throw new RuntimeException(
                    "Error al subir la imagen a Cloudinary: " + e.getMessage(), e
            );
        }
    }
}
