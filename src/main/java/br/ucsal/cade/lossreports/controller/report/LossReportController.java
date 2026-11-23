package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.lossreports.controller.report.dto.LossLocationRequestDTO;
import br.ucsal.cade.lossreports.controller.report.dto.LossReportRequestDTO;
import br.ucsal.cade.lossreports.controller.report.dto.LossReportResponseDTO;
import br.ucsal.cade.lossreports.controller.report.dto.LossReportUpdateRequestDTO;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.lossreports.service.report.UpdateLossReport;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import br.ucsal.cade.shared.storage.StoredPhoto;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/loss-reports")
@RequiresRole(Role.STUDENT)
class LossReportController implements LossReportApi {

    private final LossReportService service;

    LossReportController(LossReportService service) {
        this.service = service;
    }

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LossReportResponseDTO> register(@Valid @RequestPart("report") LossReportRequestDTO report,
                                                          @RequestPart(name = "photo", required = false)
                                                          MultipartFile photo,
                                                          @AuthenticationPrincipal Jwt token) {
        RegisterLossReport request = new RegisterLossReport(report.categoryId(), report.description(),
                report.secretDetail(), locationsOf(report), report.lostFrom(), report.lostUntil(), report.shift(),
                bytesOf(photo));
        LossReport registered = service.register(request, UUID.fromString(token.getSubject()));
        return ResponseEntity.created(URI.create("/loss-reports/" + registered.getId()))
                .body(LossReportResponseDTO.from(registered));
    }

    @Override
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LossReportResponseDTO update(@PathVariable UUID id, @Valid @RequestBody LossReportUpdateRequestDTO report,
                                        @AuthenticationPrincipal Jwt token) {
        List<LossLocationRequestDTO> locations = report.locations() == null ? List.of() : report.locations();
        UpdateLossReport request = new UpdateLossReport(report.categoryId(), report.description(),
                report.secretDetail(),
                locations.stream().map(location -> LossLocation.of(location.block(), location.room())).toList(),
                report.lostFrom(), report.lostUntil(), report.shift(), report.version());
        return LossReportResponseDTO.from(service.update(id, request, UUID.fromString(token.getSubject())));
    }

    @Override
    @GetMapping("/mine")
    public List<LossReportResponseDTO> mine(@AuthenticationPrincipal Jwt token) {
        return service.listOf(UUID.fromString(token.getSubject())).stream().map(LossReportResponseDTO::from).toList();
    }

    @Override
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        StoredPhoto photo = service.photoOf(id, UUID.fromString(token.getSubject()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.type().mediaType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(photo.content());
    }

    private static List<LossLocation> locationsOf(LossReportRequestDTO report) {
        List<LossLocationRequestDTO> locations = report.locations() == null ? List.of() : report.locations();
        return locations.stream().map(location -> LossLocation.of(location.block(), location.room())).toList();
    }

    private static byte[] bytesOf(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return null;
        }
        try {
            return photo.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
