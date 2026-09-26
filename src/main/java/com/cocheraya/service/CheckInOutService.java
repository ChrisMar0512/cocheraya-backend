package com.cocheraya.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.cocheraya.dto.CheckInResponse;
import com.cocheraya.dto.CheckOutResponse;
import com.cocheraya.dto.ParkingUpdateEvent;
import com.cocheraya.dto.QRResponse;
import com.cocheraya.dto.WalletResponse;
import com.cocheraya.exception.InvalidOperationException;
import com.cocheraya.exception.QRCodeAlreadyUsedException;
import com.cocheraya.exception.QRCodeExpiredException;
import com.cocheraya.exception.ResourceNotFoundException;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import com.cocheraya.entity.QRCode;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.Reservation.ReservationStatus;
import com.cocheraya.repository.QRCodeRepository;
import com.cocheraya.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInOutService implements ICheckInOutService {

    private static final int QR_EXPIRATION_MINUTES = 30;
    private static final int QR_CODE_LENGTH = 8;
    private static final int QR_IMAGE_SIZE = 300;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MONEY_SCALE = 2;
    private static final String QR_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String PARKING_UPDATES_TOPIC = "/topic/parking-updates";
    private static final Set<ReservationStatus> QR_ALLOWED_STATUSES = EnumSet.of(
            ReservationStatus.PENDING,
            ReservationStatus.ACTIVE
    );
    private static final SecureRandom CODE_RANDOM = new SecureRandom();

    private final QRCodeRepository qrCodeRepository;
    private final ReservationRepository reservationRepository;
    private final WalletService walletService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    @Transactional
    public QRResponse generateQR(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con id: " + reservationId));

        if (!QR_ALLOWED_STATUSES.contains(reservation.getStatus())) {
            throw new InvalidOperationException("Solo se puede generar QR para reservas pendientes o activas");
        }

        Optional<QRCode> existingQr = qrCodeRepository.findByReservationId(reservationId);
        if (existingQr.isPresent()) {
            QRCode qr = existingQr.get();
            // If the QR has expired, regenerate a new short alphanumeric code, renew expiration, and reset used flags.
            if (qr.getExpiresAt().isBefore(LocalDateTime.now())) {
                qr.setCode(generateUniqueShortCode());
                qr.setExpiresAt(LocalDateTime.now().plusMinutes(QR_EXPIRATION_MINUTES));
                qr.setUsedForCheckin(false);
                qr.setUsedForCheckout(false);
                qr = qrCodeRepository.save(qr);
            }
            try {
                String base64Image = generateQrBase64Image(qr.getCode());
                QRResponse response = modelMapper.map(qr, QRResponse.class);
                response.setQrImageBase64(base64Image);
                response.setReservationId(reservationId);
                return response;
            } catch (Exception e) {
                throw new RuntimeException("Error al regenerar la imagen del QR", e);
            }
        }

        String code = generateUniqueShortCode();
        QRCode qrCode = new QRCode();
        qrCode.setReservation(reservation);
        qrCode.setCode(code);
        qrCode.setExpiresAt(LocalDateTime.now().plusMinutes(QR_EXPIRATION_MINUTES));
        qrCodeRepository.save(qrCode);

        try {
            String base64Image = generateQrBase64Image(code);
            QRResponse response = modelMapper.map(qrCode, QRResponse.class);
            response.setQrImageBase64(base64Image);
            response.setReservationId(reservationId);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar la imagen del QR", e);
        }
    }

    private String generateUniqueShortCode() {
        String code;
        boolean exists;
        do {
            StringBuilder sb = new StringBuilder(QR_CODE_LENGTH);
            for (int i = 0; i < QR_CODE_LENGTH; i++) {
                int characterIndex = CODE_RANDOM.nextInt(QR_CODE_CHARACTERS.length());
                sb.append(QR_CODE_CHARACTERS.charAt(characterIndex));
            }
            code = sb.toString();
            exists = qrCodeRepository.findByCode(code).isPresent();
        } while (exists);
        return code;
    }

    private String generateQrBase64Image(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        var bitMatrix = qrCodeWriter.encode(
                text,
                BarcodeFormat.QR_CODE,
                QR_IMAGE_SIZE,
                QR_IMAGE_SIZE
        );
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(pngData);
    }

    @Transactional
    public CheckInResponse checkIn(String code) {
        QRCode qrCode = qrCodeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Código QR no encontrado"));

        if (qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new QRCodeExpiredException("El código QR ha expirado, solicita uno nuevo");
        }

        if (Boolean.TRUE.equals(qrCode.getUsedForCheckin())) {
            throw new QRCodeAlreadyUsedException("Este QR ya fue utilizado para hacer check-in");
        }

        qrCode.setUsedForCheckin(true);
        Reservation reservation = qrCode.getReservation();
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setStartTime(LocalDateTime.now());
        
        ParkingSpace parkingSpace = reservation.getParkingSpace();
        parkingSpace.setStatus(ParkingSpaceStatus.OCCUPIED);

        qrCodeRepository.save(qrCode);
        reservationRepository.save(reservation);

        ParkingUpdateEvent event = new ParkingUpdateEvent(parkingSpace.getId(), ParkingSpaceStatus.OCCUPIED.name());
        messagingTemplate.convertAndSend(PARKING_UPDATES_TOPIC, event);

        CheckInResponse response = new CheckInResponse();
        response.setMessage("Check-in exitoso");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    @Transactional
    public CheckOutResponse checkOut(String code) {
        QRCode qrCode = qrCodeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Código QR no encontrado"));

        Reservation reservation = qrCode.getReservation();

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidOperationException("No hay una sesión activa para este espacio");
        }

        if (Boolean.TRUE.equals(qrCode.getUsedForCheckout())) {
            throw new QRCodeAlreadyUsedException("Este QR ya fue utilizado para hacer check-out");
        }

        
        long minutosUsados = calculateUsedMinutes(reservation);

        BigDecimal pricePerMinute = reservation.getParkingSpace().getPricePerHour()
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal costoTotal = pricePerMinute.multiply(BigDecimal.valueOf(minutosUsados))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        Long driverId = reservation.getDriver().getId();
        WalletResponse walletResponse = walletService.charge(driverId, costoTotal, reservation);

        Long hostId = reservation.getParkingSpace().getHost().getId();
        walletService.creditHost(hostId, costoTotal, reservation);

        qrCode.setUsedForCheckout(true);
        reservation.setEndTime(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.FINISHED);

        ParkingSpace parkingSpace = reservation.getParkingSpace();
        parkingSpace.setStatus(ParkingSpaceStatus.AVAILABLE);

        qrCodeRepository.save(qrCode);
        reservationRepository.save(reservation);

        ParkingUpdateEvent event = new ParkingUpdateEvent(parkingSpace.getId(), ParkingSpaceStatus.AVAILABLE.name());
        messagingTemplate.convertAndSend(PARKING_UPDATES_TOPIC, event);

        CheckOutResponse response = modelMapper.map(reservation, CheckOutResponse.class);
        response.setDurationMinutes(minutosUsados);
        response.setTotalCharged(costoTotal);
        response.setRemainingBalance(walletResponse.getBalance());
        response.setParkingSpaceName(parkingSpace.getTitle());
        response.setReservationId(reservation.getId());

        
        eventPublisher.publishEvent(new com.cocheraya.event.ReservationCompletedEvent(
                this,
                reservation.getId(),
                reservation.getDriver().getEmail(),
                reservation.getDriver().getName(),
                parkingSpace.getTitle(),
                minutosUsados,
                costoTotal,
                walletResponse.getBalance()
        ));

        return response;
    }

    private long calculateUsedMinutes(Reservation reservation) {
        long usedMinutes = ChronoUnit.MINUTES.between(
                reservation.getStartTime(),
                LocalDateTime.now()
        );
        return usedMinutes == 0 ? 1 : usedMinutes;
    }
}
